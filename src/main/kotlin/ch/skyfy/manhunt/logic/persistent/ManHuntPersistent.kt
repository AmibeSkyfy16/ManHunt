package ch.skyfy.manhunt.logic.persistent

import ch.skyfy.jsonconfiglib.Defaultable
import ch.skyfy.jsonconfiglib.Validatable
import kotlinx.serialization.Serializable

@Serializable
enum class GameState{
    NOT_STARTED,
    STARTING,
    RUNNING,
    PAUSED,
    FINISHED
}

@Serializable
data class ManHuntPersistent(
    var gameState: GameState,
    var huntersStarted: Boolean
): Validatable

class DefaultManHuntPersistent : Defaultable<ManHuntPersistent> {
    override fun getDefault() = ManHuntPersistent(
        GameState.NOT_STARTED,
        false
    )
}