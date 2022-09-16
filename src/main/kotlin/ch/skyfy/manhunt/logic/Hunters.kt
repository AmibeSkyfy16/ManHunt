package ch.skyfy.manhunt.logic

import ch.skyfy.jsonconfiglib.update
import ch.skyfy.manhunt.ManHuntMod
import ch.skyfy.manhunt.ManHuntMod.Companion.CONFIG_DIRECTORY
import ch.skyfy.manhunt.config.Configs
import ch.skyfy.manhunt.config.persistent.ManHuntPersistent
import ch.skyfy.manhunt.config.persistent.Persistent
import kotlinx.coroutines.cancel
import net.minecraft.nbt.NbtList
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.silkmc.silk.core.task.infiniteMcCoroutineTask
import net.silkmc.silk.core.text.broadcastText
import kotlin.time.Duration.Companion.seconds

class Hunters(game: Game, private val minecraftServer: MinecraftServer) : BaseRole(game, CONFIG_DIRECTORY.resolve("starter-kit-${ManHuntMod.THE_HUNTERS}.dat").toFile(), CONFIG_DIRECTORY.resolve("respawn-kit-${ManHuntMod.THE_HUNTERS}.dat").toFile()) {

    fun delayedStartForHunters() {
        val huntersDelay = Configs.MANHUNT_CONFIG.serializableData.huntersDelay
        var count = 0
        minecraftServer.broadcastText(Text.literal("The game starts, the hunters must wait another $huntersDelay seconds before they can chase you").setStyle(Style.EMPTY.withColor(Formatting.GOLD)))
        infiniteMcCoroutineTask(sync = true, client = false, period = 1.seconds) {
            serverPlayerEntities.forEach { it.sendMessage(Text.literal("Time to wait ${huntersDelay - count}").setStyle(Style.EMPTY.withColor(Formatting.LIGHT_PURPLE)), true) }
            if (++count == huntersDelay) {
                insertStarterKit(message = Text.literal("Go go go !").setStyle(Style.EMPTY.withColor(Formatting.LIGHT_PURPLE)))
                Persistent.MANHUNT_PERSISTENT.update(ManHuntPersistent::huntersStarted, true)
                cancel()
            }
        }
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