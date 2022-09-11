package ch.skyfy.manhunt.logic

import ch.skyfy.jsonconfiglib.ConfigManager
import ch.skyfy.manhunt.ManHuntMod
import ch.skyfy.manhunt.ManHuntMod.Companion.CONFIG_DIRECTORY
import ch.skyfy.manhunt.ManHuntMod.Companion.THE_HUNTED_ONES
import ch.skyfy.manhunt.ManHuntMod.Companion.THE_HUNTERS
import ch.skyfy.manhunt.callbacks.EntityDamageCallback
import ch.skyfy.manhunt.callbacks.PlayerMoveCallback
import ch.skyfy.manhunt.callbacks.TimeUpdatedCallback
import ch.skyfy.manhunt.config.Configs
import ch.skyfy.manhunt.config.DefaultManHuntConfig
import ch.skyfy.manhunt.logic.persistent.GameState
import ch.skyfy.manhunt.logic.persistent.Persistent
import ch.skyfy.manhunt.utils.MathUtils
import kotlinx.coroutines.cancel
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.block.Blocks
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtList
import net.minecraft.network.packet.s2c.play.EntityAttributesS2CPacket
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayNetworkHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.math.BlockPos
import net.minecraft.world.GameMode
import net.silkmc.silk.core.task.infiniteMcCoroutineTask
import net.silkmc.silk.core.text.broadcastText
import java.io.File
import java.util.*
import kotlin.time.Duration.Companion.seconds

class Game(private val minecraftServer: MinecraftServer) {

    private val events: Events = Events(this)
    private val timeline: Timeline = Timeline()

    private val huntersServerPlayerEntities: MutableSet<ServerPlayerEntity> = mutableSetOf()
    private val theHuntedOnesServerPlayerEntities: MutableSet<ServerPlayerEntity> = mutableSetOf()

    private val deadPlayers: MutableSet<String> = mutableSetOf()

    init {
        registerEvents()
    }

    private fun registerEvents() {
        ServerPlayConnectionEvents.JOIN.register(events::onPlayerJoin)
        ServerPlayConnectionEvents.DISCONNECT.register(events::onPlayerDisconnect)
        PlayerMoveCallback.EVENT.register(events::onPlayerMove)
        EntityDamageCallback.EVENT.register(events::onPlayerDamage)
        TimeUpdatedCallback.EVENT.register(events::onTimeUpdated)
        ServerEntityEvents.ENTITY_LOAD.register(events::onEntityLoaded)
    }

    fun cooldownStart() {

        // Clear some player stats
        val players = theHuntedOnesServerPlayerEntities + huntersServerPlayerEntities
        players.forEach { serverPlayerEntity ->
            serverPlayerEntity.clearStatusEffects()
            serverPlayerEntity.inventory.clear()
        }

        var count = 0
        infiniteMcCoroutineTask(sync = true, client = false, period = 1.seconds) {
            minecraftServer.broadcastText(Text.literal("Game starting in ${20 - count}").setStyle(Style.EMPTY.withColor(Formatting.LIGHT_PURPLE)))
            if (++count == 20) {
                minecraftServer.broadcastText(Text.literal("The game has started !").setStyle(Style.EMPTY.withColor(Formatting.LIGHT_PURPLE)))
                cancel()
                start()
            }
        }
    }

    private fun start() {
        Persistent.MANHUNT_PERSISTENT.`data`.gameState = GameState.RUNNING
        ConfigManager.save(Persistent.MANHUNT_PERSISTENT)

        val huntersStarterKitFile = CONFIG_DIRECTORY.resolve("starter-kit-$THE_HUNTERS.dat").toFile()
        val theHuntedOnesStarterKitFile = CONFIG_DIRECTORY.resolve("starter-kit-$THE_HUNTED_ONES.dat").toFile()

        timeline.startTimer()

        theHuntedOnesServerPlayerEntities.forEach { serverPlayerEntity ->
            insertKit(theHuntedOnesStarterKitFile, serverPlayerEntity)
            serverPlayerEntity.changeGameMode(GameMode.SURVIVAL)
            serverPlayerEntity.sendMessage(Text.literal("Good Luck Buddies ! Run run run").setStyle(Style.EMPTY.withColor(Formatting.GREEN)))
        }

        val huntersDelay = Configs.MANHUNT_CONFIG.`data`.huntersDelay
        var count = 0
        infiniteMcCoroutineTask(sync = true, client = false, period = 1.seconds) {
            huntersServerPlayerEntities.forEach { serverPlayerEntity ->
                serverPlayerEntity.sendMessage(Text.literal("Wait ${huntersDelay - count} seconds before chasing them !").setStyle(Style.EMPTY.withColor(Formatting.LIGHT_PURPLE)), true)
            }
            if (++count == huntersDelay) {
                huntersServerPlayerEntities.forEach { serverPlayerEntity ->
                    insertKit(huntersStarterKitFile, serverPlayerEntity)
                    serverPlayerEntity.sendMessage(Text.literal("Go go go !").setStyle(Style.EMPTY.withColor(Formatting.LIGHT_PURPLE)))
                }
                Persistent.MANHUNT_PERSISTENT.`data`.huntersStarted = true
                ConfigManager.save(Persistent.MANHUNT_PERSISTENT)
                cancel()
            }
        }

        // Show the hunted ones position every x seconds
        val period = Configs.MANHUNT_CONFIG.`data`.showTheHuntedOnePositionPeriod
        infiniteMcCoroutineTask(sync = true, client = false, period = period.seconds) {
            huntersServerPlayerEntities.forEach { hunter ->
                hunter.sendMessage(Text.literal("Positions for the hunted will be show below").setStyle(Style.EMPTY.withColor(Formatting.GOLD)))
                theHuntedOnesServerPlayerEntities.forEachIndexed { index, theHunted ->
                    val x = theHunted.blockX
                    val y = theHunted.blockY
                    val z = theHunted.blockZ
                    hunter.sendMessage(Text.literal("   $index.  x: $x   y:$y    z:$z").setStyle(Style.EMPTY.withColor(Formatting.GOLD)))
                }
            }
        }

    }

    private fun insertKit(starterKitFile: File, serverPlayerEntity: ServerPlayerEntity) {
        if (starterKitFile.exists()) {
            val inventory = NbtIo.read(starterKitFile)
            if (inventory != null) serverPlayerEntity.inventory.readNbt(inventory.get("inventory") as NbtList?)
        }
    }

    private fun setCustomHealth(player: ServerPlayerEntity, refillHealth: Boolean = true) {
        fun setCustomHealthImpl(amount: Double){
            val maxHealthAttr = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH)!!
            maxHealthAttr.clearModifiers()
            val attributeModifier = EntityAttributeModifier(UUID.randomUUID(), "Health Modifier", amount, EntityAttributeModifier.Operation.ADDITION)
            maxHealthAttr.addPersistentModifier(attributeModifier)
            if (refillHealth) player.health = maxHealthAttr.value.toFloat()
            player.networkHandler.sendPacket(EntityAttributesS2CPacket(player.id, setOf(maxHealthAttr)))
        }

        val theHuntedOnes = Configs.MANHUNT_CONFIG.`data`.theHuntedOnes
        val hunters = Configs.MANHUNT_CONFIG.`data`.hunters
        val theHuntedOnesHealth = Configs.MANHUNT_CONFIG.`data`.theHuntedOnesHealth
        val huntersHealth = Configs.MANHUNT_CONFIG.`data`.huntersHealth

        hunters.find { it == player.name.string }?.let{setCustomHealthImpl(huntersHealth) }
        theHuntedOnes.find { it == player.name.string }?.let {setCustomHealthImpl(theHuntedOnesHealth) }
    }

    @Suppress("UNUSED_PARAMETER")
    class Events(private val game: Game) {

        fun onPlayerDisconnect(handler: ServerPlayNetworkHandler, server: MinecraftServer) {
            val player = handler.player
            val theHuntedOnes = Configs.MANHUNT_CONFIG.`data`.theHuntedOnes
            val hunters = Configs.MANHUNT_CONFIG.`data`.hunters
            if (hunters.any { name -> name == player.name.string }) game.huntersServerPlayerEntities.remove(player)
            if (theHuntedOnes.any { name -> name == player.name.string }) game.theHuntedOnesServerPlayerEntities.remove(player)
        }

        fun onPlayerJoin(handler: ServerPlayNetworkHandler, sender: PacketSender?, server: MinecraftServer) {

            /**
             * Will generate a spawn platform for the waiting room (Only if it's the default value)
             */
            fun generatePlatformIfDefault() {
                val waitingRoom = Configs.MANHUNT_CONFIG.`data`.waitingRoom
                val spawnLocation = waitingRoom.spawnLocation
                val defaultWaitingRoom = DefaultManHuntConfig().getDefault().waitingRoom

                if (defaultWaitingRoom != waitingRoom) return
                val optWorld = GameUtils.getServerWorldByIdentifier(server, spawnLocation.dimensionName)
                if (optWorld.isEmpty) return

                val world = optWorld.get()

                var x = spawnLocation.x
                var z = spawnLocation.z
                for (i in 0..5) {
                    for (j in 0..5) {
                        for (k in 0..5) {
                            val pos1 = BlockPos(x + k, spawnLocation.y - 1, z)
                            world.setBlockState(pos1, Blocks.WHITE_STAINED_GLASS.defaultState)
                            val pos2 = BlockPos((x + k) * -1, spawnLocation.y - 1, z)
                            world.setBlockState(pos2, Blocks.WHITE_STAINED_GLASS.defaultState)
                            val pos4 = BlockPos(x + k, spawnLocation.y - 1, z * -1)
                            world.setBlockState(pos4, Blocks.WHITE_STAINED_GLASS.defaultState)
                            val pos3 = BlockPos((x + k) * -1, spawnLocation.y - 1, z * -1)
                            world.setBlockState(pos3, Blocks.WHITE_STAINED_GLASS.defaultState)

                            val pos11 = BlockPos(x, spawnLocation.y - 1, z)
                            world.setBlockState(pos11, Blocks.WHITE_STAINED_GLASS.defaultState)
                            val pos22 = BlockPos(x * -1, spawnLocation.y - 1, z)
                            world.setBlockState(pos22, Blocks.WHITE_STAINED_GLASS.defaultState)
                            val pos44 = BlockPos(x, spawnLocation.y - 1, z * -1)
                            world.setBlockState(pos44, Blocks.WHITE_STAINED_GLASS.defaultState)
                            val pos33 = BlockPos(x * -1, spawnLocation.y - 1, z * -1)
                            world.setBlockState(pos33, Blocks.WHITE_STAINED_GLASS.defaultState)

                            val pos111 = BlockPos(x, spawnLocation.y - 1, z + k)
                            world.setBlockState(pos111, Blocks.WHITE_STAINED_GLASS.defaultState)
                            val pos222 = BlockPos(x * -1, spawnLocation.y - 1, z + k)
                            world.setBlockState(pos222, Blocks.WHITE_STAINED_GLASS.defaultState)
                            val pos444 = BlockPos(x, spawnLocation.y - 1, (z + k) * -1)
                            world.setBlockState(pos444, Blocks.WHITE_STAINED_GLASS.defaultState)
                            val pos333 = BlockPos(x * -1, spawnLocation.y - 1, (z + k) * -1)
                            world.setBlockState(pos333, Blocks.WHITE_STAINED_GLASS.defaultState)

                            val pos1111 = BlockPos(x + k, spawnLocation.y - 1, z + k)
                            world.setBlockState(pos1111, Blocks.WHITE_STAINED_GLASS.defaultState)
                            val pos2222 = BlockPos((x + k) * -1, spawnLocation.y - 1, z + k)
                            world.setBlockState(pos2222, Blocks.WHITE_STAINED_GLASS.defaultState)
                            val pos4444 = BlockPos(x + k, spawnLocation.y - 1, (z + k) * -1)
                            world.setBlockState(pos4444, Blocks.WHITE_STAINED_GLASS.defaultState)
                            val pos3333 = BlockPos((x + k) * -1, spawnLocation.y - 1, (z + k) * -1)
                            world.setBlockState(pos3333, Blocks.WHITE_STAINED_GLASS.defaultState)
                        }
                        x++
                        z++
                    }
                    x = i.toDouble()
                }
            }

            val player = handler.player

            if (Configs.MANHUNT_CONFIG.`data`.hunters.any { name -> name == player.name.string }) game.huntersServerPlayerEntities.add(player)
            if (Configs.MANHUNT_CONFIG.`data`.theHuntedOnes.any { name -> name == player.name.string }) game.theHuntedOnesServerPlayerEntities.add(player)

            game.setCustomHealth(player, GameUtils.isNotStarted())

            if (GameUtils.isNotStarted()) {

//                generatePlatformIfDefault()

                if (!player.hasPermissionLevel(4)) player.changeGameMode(GameMode.ADVENTURE)

                val spawnLocation = Configs.MANHUNT_CONFIG.`data`.waitingRoom.spawnLocation

                GameUtils.getServerWorldByIdentifier(server, spawnLocation.dimensionName).ifPresent { serverWorld ->
                    player.teleport(serverWorld, spawnLocation.x, spawnLocation.y, spawnLocation.z, spawnLocation.yaw, spawnLocation.pitch)
                }
            }
        }

        fun onPlayerMove(moveData: PlayerMoveCallback.MoveData, player: ServerPlayerEntity): ActionResult {
            if (player.hasPermissionLevel(4) && Configs.MANHUNT_CONFIG.`data`.debug) return ActionResult.PASS

            if (GameUtils.isFinished()) return ActionResult.PASS
            if (GameUtils.isPaused()) return ActionResult.FAIL

            if (GameUtils.isNotStarted() || GameUtils.isStarting()) {
                val waitingRoom = Configs.MANHUNT_CONFIG.`data`.waitingRoom
                if (MathUtils.cancelPlayerFromLeavingAnArea(player, waitingRoom.cube, waitingRoom.spawnLocation)) return ActionResult.FAIL
            }

            if (GameUtils.isRunning()) {
                if (!Persistent.MANHUNT_PERSISTENT.`data`.huntersStarted) {
                    game.huntersServerPlayerEntities.find { it === player }?.let {
                        val waitingRoom = Configs.MANHUNT_CONFIG.`data`.waitingRoom
                        if (MathUtils.cancelPlayerFromLeavingAnArea(player, waitingRoom.cube, waitingRoom.spawnLocation)) return ActionResult.FAIL
                    }
                }
            }

            return ActionResult.PASS
        }

        fun onPlayerDamage(livingEntity: LivingEntity, damageSource: DamageSource, amount: Float): ActionResult {
            if (livingEntity is ServerPlayerEntity && livingEntity.hasPermissionLevel(4) && Configs.MANHUNT_CONFIG.`data`.debug) return ActionResult.PASS

            if ((GameUtils.isNotStarted() || GameUtils.isStarting()) && livingEntity is ServerPlayerEntity) return ActionResult.FAIL

            if (GameUtils.isRunning() && livingEntity is ServerPlayerEntity && GameUtils.isPlayerAnHunter(livingEntity.name.string) && !Persistent.MANHUNT_PERSISTENT.`data`.huntersStarted) return ActionResult.FAIL

            if (livingEntity is ServerPlayerEntity) {
                if (livingEntity.health - amount <= 0) {
                    // TODO clear stuff that come from the respawn kit
                    game.deadPlayers.add(livingEntity.uuidAsString)
                }
            }

            return ActionResult.PASS
        }

        fun onEntityLoaded(entity: Entity, world: ServerWorld){
            if(entity is ServerPlayerEntity){
                if(game.deadPlayers.any { uuid -> uuid == entity.uuidAsString }){
                    // On respawn
                    game.setCustomHealth(entity)
                    game.deadPlayers.remove(entity.uuidAsString)

                    if(game.huntersServerPlayerEntities.any { serverPlayerEntity -> serverPlayerEntity.uuidAsString == entity.uuidAsString })
                        game.insertKit(CONFIG_DIRECTORY.resolve("respawn-kit-$THE_HUNTERS.dat").toFile(), entity)
                    if(game.theHuntedOnesServerPlayerEntities.any { serverPlayerEntity -> serverPlayerEntity.uuidAsString == entity.uuidAsString })
                        game.insertKit(CONFIG_DIRECTORY.resolve("respawn-kit-${THE_HUNTED_ONES}.dat").toFile(), entity)

                    if(game.theHuntedOnesServerPlayerEntities.isNotEmpty()) {
                        val theHuntedOne = game.theHuntedOnesServerPlayerEntities.random()
                        val x = (theHuntedOne.blockX-20..theHuntedOne.blockX+20).random().toDouble()
                        val z = (theHuntedOne.blockZ-20..theHuntedOne.blockZ+20).random().toDouble()
                        entity.addStatusEffect(StatusEffectInstance(StatusEffects.RESISTANCE, 20 * 20, 100))
                        entity.teleport(theHuntedOne.world as ServerWorld, x, 320.0, z, 100f, 100f)
                    }
                }
            }
        }

        fun onTimeUpdated(day: Int, minutes: Int, seconds: Int, timeOfDay: Int): ActionResult {
            return ActionResult.PASS
        }

    }

}