package ch.skyfy.manhunt.logic

import ch.skyfy.jsonconfiglib.ConfigManager
import ch.skyfy.manhunt.config.Configs
import ch.skyfy.manhunt.logic.persistent.GameState
import ch.skyfy.manhunt.logic.persistent.Persistent
import kotlinx.coroutines.cancel
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.silkmc.silk.core.task.infiniteMcCoroutineTask
import net.silkmc.silk.core.text.broadcastText
import kotlin.time.Duration.Companion.seconds

class Game(private val minecraftServer: MinecraftServer) {

    private val timeline: Timeline = Timeline()

    private val hunters = Hunters(this, minecraftServer)
    val theHuntedOnes = TheHuntedOnes(this)

    fun cooldownStart() {

        val gamers = hunters.serverPlayerEntities + theHuntedOnes.serverPlayerEntities
        gamers.forEach { serverPlayerEntity ->
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

        ConfigManager.computeAndSave(Persistent.MANHUNT_PERSISTENT, { it.gameState = GameState.RUNNING })

        timeline.startTimer()

        hunters.insertStarterKit(Text.literal("Good Luck Buddies ! Run run run").setStyle(Style.EMPTY.withColor(Formatting.GREEN)))

        hunters.delayedStartForHunters()

        showTheHuntedOnesPositionsToHunters()

    }

    private fun showTheHuntedOnesPositionsToHunters() {
        infiniteMcCoroutineTask(sync = true, client = false, period = Configs.MANHUNT_CONFIG.`data`.showTheHuntedOnePositionPeriod.seconds) {
            hunters.serverPlayerEntities.forEach { hunter ->
                hunter.sendMessage(Text.literal("Positions for the hunted will be show below").setStyle(Style.EMPTY.withColor(Formatting.GOLD)))
                theHuntedOnes.serverPlayerEntities.forEachIndexed { index, theHunted ->
                    val x = theHunted.blockX
                    val y = theHunted.blockY
                    val z = theHunted.blockZ
                    hunter.sendMessage(Text.literal("   $index.  x: $x   y:$y    z:$z").setStyle(Style.EMPTY.withColor(Formatting.GOLD)))
                }
            }
        }
    }

}