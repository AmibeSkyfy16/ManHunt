package ch.skyfy.manhunt.config.persistent

import ch.skyfy.jsonconfiglib.Defaultable
import ch.skyfy.jsonconfiglib.Validatable
import kotlinx.serialization.Serializable

@Serializable
enum class GameState(val displayName: String) {
    NOT_STARTED("Not started"),
    STARTING("Starting"),
    RUNNING("Running"),
    PAUSED("Paused"),
    FINISHED("Finished")
}

@Serializable
data class ManHuntPersistent(
    var gameState: GameState,
    var huntersStarted: Boolean,
) : Validatable

class DefaultManHuntPersistent : Defaultable<ManHuntPersistent> {
    override fun getDefault() = ManHuntPersistent(
        GameState.NOT_STARTED,
        false
    )
}