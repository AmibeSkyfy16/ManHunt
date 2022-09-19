package ch.skyfy.manhunt.config.persistent

import ch.skyfy.jsonconfiglib.ConfigData
import ch.skyfy.manhunt.ManHuntMod.Companion.DATA_DIRECTORY

object Persistent {
    val MANHUNT_PERSISTENT = ConfigData<ManHuntPersistent, DefaultManHuntPersistent>(DATA_DIRECTORY.resolve("manhunt-persistent.json"), true)
    val TIMELINE_PERSISTENT = ConfigData<TimelinePersistent, DefaultTimelinePersistent>(DATA_DIRECTORY.resolve("timeline-persistent.json"), false)
}