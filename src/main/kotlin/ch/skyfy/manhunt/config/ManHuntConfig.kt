package ch.skyfy.manhunt.config

import ch.skyfy.jsonconfiglib.Defaultable
import ch.skyfy.jsonconfiglib.Validatable
import ch.skyfy.manhunt.data.Cube
import ch.skyfy.manhunt.data.SpawnLocation
import kotlinx.serialization.Serializable

@Serializable
data class WaitingRoom(
    val cube: Cube,
    val spawnLocation: SpawnLocation
)

@Serializable
data class ManHuntConfig(
    val hunters: MutableList<String>,
    val theHuntedOnes: MutableList<String>,
    val huntersDelay: Int,
    val huntersHealth: Double,
    val theHuntedOnesHealth: Double,
    val showTheHuntedOnePositionPeriod: Int,
    val waitingRoom: WaitingRoom,
    val debug: Boolean
) : Validatable {

}

class DefaultManHuntConfig : Defaultable<ManHuntConfig> {
    override fun getDefault() = ManHuntConfig(
        mutableListOf(),
        mutableListOf(),
        20,
        20.0,
        80.0,
        30,
        WaitingRoom(
            Cube(10, 0.0, 310.0, 0.0),
            SpawnLocation("minecraft:overworld", 0.0, 310.0, 0.0, 180f, 2.8f)
        ),
        false
    )
}