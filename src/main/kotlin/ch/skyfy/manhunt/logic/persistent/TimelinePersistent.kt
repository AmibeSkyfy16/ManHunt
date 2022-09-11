package ch.skyfy.manhunt.logic.persistent

import ch.skyfy.jsonconfiglib.Defaultable
import ch.skyfy.jsonconfiglib.Validatable
import kotlinx.serialization.Serializable

@Serializable
data class TimelinePersistent(
    var day: Int,
    var minutes: Int,
    var seconds: Int,
    var timeOfDay: Int
) : Validatable

class DefaultTimelinePersistent : Defaultable<TimelinePersistent> {
    override fun getDefault() = TimelinePersistent(0, 0, 0, 0)
}
