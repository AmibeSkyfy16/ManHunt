@file:Suppress("unused")

package ch.skyfy.manhunt.logic

import ch.skyfy.manhunt.config.Configs
import ch.skyfy.manhunt.config.persistent.GameState
import ch.skyfy.manhunt.config.persistent.Persistent
import net.minecraft.server.MinecraftServer
import net.minecraft.server.world.ServerWorld
import java.util.*
import java.util.stream.StreamSupport

object GameUtils {

    fun isNotStarted(): Boolean = Persistent.MANHUNT_PERSISTENT.serializableData.gameState == GameState.NOT_STARTED
    fun isStarting(): Boolean = Persistent.MANHUNT_PERSISTENT.serializableData.gameState == GameState.STARTING
    fun isRunning(): Boolean = Persistent.MANHUNT_PERSISTENT.serializableData.gameState == GameState.RUNNING
    fun isPaused(): Boolean = Persistent.MANHUNT_PERSISTENT.serializableData.gameState == GameState.PAUSED
    fun isFinished(): Boolean = Persistent.MANHUNT_PERSISTENT.serializableData.gameState == GameState.FINISHED

    fun getServerWorldByIdentifier(server: MinecraftServer, id: String): Optional<ServerWorld> = StreamSupport.stream(server.worlds.spliterator(), false)
        .filter { serverWorld: ServerWorld -> serverWorld.dimension.effects().toString() == id }
        .findFirst()

    fun getPlayerRole(name: String): BaseRole.Role {
        return if (Configs.MANHUNT_CONFIG.serializableData.hunters.any { it == name }) BaseRole.Role.HUNTER
        else if (Configs.MANHUNT_CONFIG.serializableData.theHuntedOnes.any { it == name }) BaseRole.Role.THE_HUNTED_ONE
        else BaseRole.Role.NO_ROLE
    }

}