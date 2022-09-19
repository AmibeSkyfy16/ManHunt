package ch.skyfy.manhunt.config

import ch.skyfy.jsonconfiglib.ConfigData
import ch.skyfy.manhunt.ManHuntMod.Companion.CONFIG_DIRECTORY

object Configs {

    val MANHUNT_CONFIG = ConfigData<ManHuntConfig, DefaultManHuntConfig>(CONFIG_DIRECTORY.resolve("manhunt-config.json"), true)

}
