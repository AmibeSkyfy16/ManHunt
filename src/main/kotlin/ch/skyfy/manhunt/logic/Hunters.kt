package ch.skyfy.manhunt.logic

import ch.skyfy.jsonconfiglib.update
import ch.skyfy.manhunt.ManHuntMod
import ch.skyfy.manhunt.ManHuntMod.Companion.CONFIG_DIRECTORY
import ch.skyfy.manhunt.config.Configs
import ch.skyfy.manhunt.config.persistent.ManHuntPersistent
import ch.skyfy.manhunt.config.persistent.Persistent
import kotlinx.coroutines.cancel
import net.minecraft.item.CompassItem
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtHelper
import net.minecraft.nbt.NbtList
import net.minecraft.nbt.NbtOps
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.world.World
import net.silkmc.silk.core.task.infiniteMcCoroutineTask
import net.silkmc.silk.core.text.broadcastText
import kotlin.time.Duration.Companion.seconds

class Hunters(val game: Game, private val minecraftServer: MinecraftServer) :
    BaseRole(game, CONFIG_DIRECTORY.resolve("starter-kit-${ManHuntMod.THE_HUNTERS}.dat").toFile(), CONFIG_DIRECTORY.resolve("respawn-kit-${ManHuntMod.THE_HUNTERS}.dat").toFile()) {

    fun delayedStartForHunters() {
        val huntersDelay = Configs.MANHUNT_CONFIG.serializableData.huntersDelay
        var count = 0
        minecraftServer.broadcastText(Text.literal("The game starts, the hunters must wait another $huntersDelay seconds before they can chase you").setStyle(Style.EMPTY.withColor(Formatting.GOLD)))
        infiniteMcCoroutineTask(sync = true, client = false, period = 1.seconds) {
            serverPlayerEntities.forEach { it.sendMessage(Text.literal("Time to wait ${huntersDelay - count}").setStyle(Style.EMPTY.withColor(Formatting.LIGHT_PURPLE)), true) }
            if (++count == huntersDelay) {
                start()
                cancel()
            }
        }
    }

    private fun start() {
        insertStarterKit(message = Text.literal("Go go go !").setStyle(Style.EMPTY.withColor(Formatting.LIGHT_PURPLE)))
        updateTrackingCompass()
        Persistent.MANHUNT_PERSISTENT.update(ManHuntPersistent::huntersStarted, true)
    }

    private fun updateTrackingCompass() {
        infiniteMcCoroutineTask(sync = true, client = false, period = Configs.MANHUNT_CONFIG.serializableData.updateTrackingCompassPeriod.seconds) {
            println("updating compass ...")
            val updatedTrackingCompassItemStack = createTrackingCompass()
            serverPlayerEntities.forEach { serverPlayerEntity ->

                val trackingCompassItemStacks = serverPlayerEntity.inventory.main.filter {
                    var found = false
                    val displayNbt = it.nbt?.get("display")
                    if(displayNbt is NbtCompound)
                        if(displayNbt.getString("name").contains("Tracking")) found = true
                    it.item is CompassItem && it.hasNbt() && found
                }

                if (trackingCompassItemStacks.isEmpty()) {
                    serverPlayerEntity.sendMessage(Text.literal("The tracking compass has been dropped on the ground").setStyle(Style.EMPTY.withColor(Formatting.GREEN)))
                    serverPlayerEntity.dropItem(updatedTrackingCompassItemStack.copy(), false, false)
                }

                trackingCompassItemStacks.forEach { trackingCompassItemStack ->
                    val slot = serverPlayerEntity.inventory.getSlotWithStack(trackingCompassItemStack)
                    serverPlayerEntity.inventory.main.remove(trackingCompassItemStack)
                    serverPlayerEntity.inventory.insertStack(slot, updatedTrackingCompassItemStack.copy())
                }

                serverPlayerEntity.sendMessage(Text.literal("Your tracking compass has been updated"))
            }
        }
    }

    private fun createTrackingCompass(): ItemStack {
        val randomTheHuntedOne = game.theHuntedOnes.serverPlayerEntities.random()

        val compassItemStack = ItemStack(Items.COMPASS, 1)
        val nbtCompound = NbtCompound()
        nbtCompound.put("LodestonePos", NbtHelper.fromBlockPos(randomTheHuntedOne.blockPos))
        nbtCompound.putBoolean("LodestoneTracked", false)
        World.CODEC.encodeStart(NbtOps.INSTANCE, randomTheHuntedOne.world.registryKey).result().ifPresent {
            val tempCompoundTag = NbtCompound()
            tempCompoundTag.putString("name", "[{\"text\":\"Tracking \",\"italic\":false},{\"text\":\"${randomTheHuntedOne.name.string}\",\"color\":\"gold\",\"italic\":false}]")
            nbtCompound.put("display", tempCompoundTag)
            nbtCompound.put("LodestoneDimension", it)
        }
        compassItemStack.nbt = nbtCompound

        return compassItemStack
    }

    /**
     * In order to prevent smart players who will kill themselves several times in a row in order
     * to duplicate the stuff that comes from the respawn kit, we are removing
     * the items from the respawn kit from the inventory
     */
    fun clearKit(serverPlayerEntity: ServerPlayerEntity) {
        val nullableNbtList = getKitAsNbtList(respawnKitFile)
        nullableNbtList?.let { nbtList ->
            val currentNbtList = serverPlayerEntity.inventory.writeNbt(NbtList())
            val iterator = currentNbtList.iterator()
            while (iterator.hasNext()) if (nbtList.any { it == iterator.next() }) iterator.remove()
            serverPlayerEntity.inventory.readNbt(currentNbtList)
        }
    }


}