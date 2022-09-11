package ch.skyfy.manhunt.data

import ch.skyfy.jsonconfiglib.Validatable
import kotlinx.serialization.Serializable

@Serializable
data class Cube(
    val size: Int,
    val x: Double,
    val y: Double,
    val z: Double,
): Validatable
