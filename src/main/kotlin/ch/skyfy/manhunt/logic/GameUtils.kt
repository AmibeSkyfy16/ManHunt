@file:Suppress("unused")

package ch.skyfy.manhunt.logic

import ch.skyfy.manhunt.config.Configs
import ch.skyfy.manhunt.logic.persistent.GameState
import ch.skyfy.manhunt.logic.persistent.Persistent
import net.minecraft.server.MinecraftServer
import net.minecraft.server.world.ServerWorld
import java.util.*
import java.util.stream.StreamSupport

object GameUtils {

    fun isNotStarted(): Boolean = Persistent.MANHUNT_PERSISTENT.`data`.gameState == GameState.NOT_STARTED
    fun isRunning(): Boolean = Persistent.MANHUNT_PERSISTENT.`data`.gameState == GameState.RUNNING
    fun isPaused(): Boolean = Persistent.MANHUNT_PERSISTENT.`data`.gameState == GameState.PAUSED
    fun isFinished(): Boolean = Persistent.MANHUNT_PERSISTENT.`data`.gameState == GameState.FINISHED

    fun getServerWorldByIdentifier(server: MinecraftServer, id: String): Optional<ServerWorld> = StreamSupport.stream(server.worlds.spliterator(), false)
            .filter { serverWorld: ServerWorld -> serverWorld.dimension.effects().toString() == id }
            .findFirst()

    fun isPlayerAnHunter(name: String)  = Configs.MANHUNT_CONFIG.`data`.hunters.any { it == name }

}