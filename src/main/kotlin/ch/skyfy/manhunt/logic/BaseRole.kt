package ch.skyfy.manhunt.logic

import ch.skyfy.manhunt.callbacks.EntityDamageCallback
import ch.skyfy.manhunt.callbacks.PlayerMoveCallback
import ch.skyfy.manhunt.config.Configs
import ch.skyfy.manhunt.config.persistent.Persistent
import ch.skyfy.manhunt.utils.MathUtils
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
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
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.world.GameMode
import java.io.File
import java.util.*

open class BaseRole(private val game: Game, private val starterKitFile: File, protected val respawnKitFile: File) {

    enum class Role(val displayName: String) {
        HUNTER("Hunter"),
        THE_HUNTED_ONE("The hunted one"),
        NO_ROLE("No defined role")
    }

    val serverPlayerEntities: MutableSet<ServerPlayerEntity> = mutableSetOf()

    private val deadPlayers: MutableSet<String> = mutableSetOf()

    init { registerEvents() }

    private fun registerEvents() {
        ServerPlayConnectionEvents.JOIN.register(::onPlayerJoin)
        ServerPlayConnectionEvents.DISCONNECT.register(::onPlayerDisconnect)
        ServerEntityEvents.ENTITY_LOAD.register(::onEntityLoaded)
        EntityDamageCallback.EVENT.register(::onPlayerDamage)
        PlayerMoveCallback.EVENT.register(::onPlayerMove)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onPlayerJoin(handler: ServerPlayNetworkHandler, sender: PacketSender?, server: MinecraftServer) {
        val player = handler.player

        setCustomHealth(player, GameUtils.isNotStarted())

        when (GameUtils.getPlayerRole(player.name.string)) {
            Role.HUNTER -> if (this is Hunters) serverPlayerEntities.add(player)
            Role.THE_HUNTED_ONE -> if (this is TheHuntedOnes) serverPlayerEntities.add(player)
            else -> {}
        }

        if (GameUtils.isNotStarted()) {
            if (!player.hasPermissionLevel(4) && Configs.MANHUNT_CONFIG.serializableData.debug) player.changeGameMode(GameMode.ADVENTURE)

            val spawnLocation = Configs.MANHUNT_CONFIG.serializableData.waitingRoom.spawnLocation
            GameUtils.getServerWorldByIdentifier(server, spawnLocation.dimensionName).ifPresent { serverWorld ->
                player.teleport(serverWorld, spawnLocation.x, spawnLocation.y, spawnLocation.z, spawnLocation.yaw, spawnLocation.pitch)
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onPlayerDisconnect(handler: ServerPlayNetworkHandler, server: MinecraftServer) {
        val player = handler.player

        when (GameUtils.getPlayerRole(player.name.string)) {
            Role.HUNTER -> if (this is Hunters) serverPlayerEntities.remove(player)
            Role.THE_HUNTED_ONE -> if (this is TheHuntedOnes) serverPlayerEntities.remove(player)
            else -> {}
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onEntityLoaded(entity: Entity, world: ServerWorld) {
        if (entity is ServerPlayerEntity && deadPlayers.any { uuid -> uuid == entity.uuidAsString }) onPlayerRespawn(entity)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onPlayerDamage(livingEntity: LivingEntity, damageSource: DamageSource, amount: Float): ActionResult {
        fun onPlayerDeath(serverPlayerEntity: ServerPlayerEntity) {
            if (this is Hunters) {
                this.clearKit(serverPlayerEntity)
                deadPlayers.add(livingEntity.uuidAsString)
            }
        }

        if (livingEntity is ServerPlayerEntity) {
            if (livingEntity.hasPermissionLevel(4) && Configs.MANHUNT_CONFIG.serializableData.debug) return ActionResult.PASS
            if ((GameUtils.isNotStarted() || GameUtils.isStarting())) return ActionResult.FAIL
            if (GameUtils.isRunning() && this is Hunters && !Persistent.MANHUNT_PERSISTENT.serializableData.huntersStarted) return ActionResult.FAIL
            if (livingEntity.health - amount <= 0) onPlayerDeath(livingEntity)
        }

        return ActionResult.PASS
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onPlayerMove(moveData: PlayerMoveCallback.MoveData, player: ServerPlayerEntity): ActionResult {
        if (player.hasPermissionLevel(4) && Configs.MANHUNT_CONFIG.serializableData.debug) return ActionResult.PASS

        if (GameUtils.isFinished()) return ActionResult.PASS
        if (GameUtils.isPaused()) return ActionResult.FAIL

        if (GameUtils.isNotStarted() || GameUtils.isStarting()) {
            val waitingRoom = Configs.MANHUNT_CONFIG.serializableData.waitingRoom
            if (MathUtils.cancelPlayerFromLeavingAnArea(player, waitingRoom.cube, waitingRoom.spawnLocation)) return ActionResult.FAIL
        }

        if (GameUtils.isRunning()) {

            // Hunters have to wait their custom delayed start before walking !
            if (this is Hunters && !Persistent.MANHUNT_PERSISTENT.serializableData.huntersStarted) {
                serverPlayerEntities.find { it.uuidAsString === player.uuidAsString }?.let {
                    val waitingRoom = Configs.MANHUNT_CONFIG.serializableData.waitingRoom
                    if (MathUtils.cancelPlayerFromLeavingAnArea(player, waitingRoom.cube, waitingRoom.spawnLocation)) return ActionResult.FAIL
                }
            }

        }

        return ActionResult.PASS
    }

    private fun onPlayerRespawn(serverPlayerEntity: ServerPlayerEntity) {
        insertKit(respawnKitFile, mutableSetOf(serverPlayerEntity))
        setCustomHealth(serverPlayerEntity, true)

        if (this is Hunters && game.theHuntedOnes.serverPlayerEntities.isNotEmpty()) {
            val theHuntedOne = game.theHuntedOnes.serverPlayerEntities.random()
            val x = (theHuntedOne.blockX - 40..theHuntedOne.blockX + 40).random().toDouble()
            val z = (theHuntedOne.blockZ - 40..theHuntedOne.blockZ + 40).random().toDouble()
            serverPlayerEntity.addStatusEffect(StatusEffectInstance(StatusEffects.RESISTANCE, 20 * 20, 100))
            serverPlayerEntity.teleport(theHuntedOne.world as ServerWorld, x, 320.0, z, 100f, 100f)
        }
    }

    private fun setCustomHealth(player: ServerPlayerEntity, refillHealth: Boolean = true) {
        fun setCustomHealthImpl(amount: Double) {
            val maxHealthAttr = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH)!!
            maxHealthAttr.clearModifiers()
            val attributeModifier = EntityAttributeModifier(UUID.randomUUID(), "Health Modifier", amount, EntityAttributeModifier.Operation.ADDITION)
            maxHealthAttr.addPersistentModifier(attributeModifier)
            if (refillHealth) player.health = maxHealthAttr.value.toFloat()
            player.networkHandler.sendPacket(EntityAttributesS2CPacket(player.id, setOf(maxHealthAttr)))
        }

        val manHuntConfig = Configs.MANHUNT_CONFIG.serializableData
        val health = when (GameUtils.getPlayerRole(player.name.string)) {
            Role.HUNTER -> manHuntConfig.huntersHealth
            Role.THE_HUNTED_ONE -> manHuntConfig.theHuntedOnesHealth
            else -> {
                20.0
            }
        }
        setCustomHealthImpl(health)
    }

     private fun insertKit(kitFile: File, serverPlayerEntities: MutableSet<ServerPlayerEntity> = this.serverPlayerEntities, message: Text? = null) {
        if (kitFile.exists()) {
            serverPlayerEntities.forEach { serverPlayerEntity ->
                message?.let { serverPlayerEntity.sendMessage(message) }
                getKitAsNbtList(kitFile)?.let { serverPlayerEntity.inventory.readNbt(it) }
            }
        }
    }

    fun insertStarterKit(serverPlayerEntities: MutableSet<ServerPlayerEntity> = this.serverPlayerEntities, message: Text? = null) = insertKit(starterKitFile, serverPlayerEntities, message = message)

    fun getKitAsNbtList(kitFile: File) = if (kitFile.exists()) NbtIo.read(kitFile)?.let { it.get("inventory") as NbtList } else null

}