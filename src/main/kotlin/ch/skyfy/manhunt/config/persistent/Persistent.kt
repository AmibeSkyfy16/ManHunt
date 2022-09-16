package ch.skyfy.manhunt.config.persistent

import ch.skyfy.jsonconfiglib.ConfigData
import ch.skyfy.manhunt.ManHuntMod.Companion.PERSISTENT_DIRECTORY

object Persistent {
    val MANHUNT_PERSISTENT = ConfigData<ManHuntPersistent, DefaultManHuntPersistent>(PERSISTENT_DIRECTORY.resolve("manhunt-persistent.json"), true)
    val TIMELINE_PERSISTENT = ConfigData<TimelinePersistent, DefaultTimelinePersistent>(PERSISTENT_DIRECTORY.resolve("timeline-persistent.json"), false)
}