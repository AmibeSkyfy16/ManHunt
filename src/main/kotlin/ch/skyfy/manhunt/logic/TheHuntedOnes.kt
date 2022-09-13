package ch.skyfy.manhunt.logic

import ch.skyfy.manhunt.ManHuntMod
import ch.skyfy.manhunt.ManHuntMod.Companion.CONFIG_DIRECTORY

class TheHuntedOnes(game: Game) : BaseRole(game, CONFIG_DIRECTORY.resolve("starter-kit-${ManHuntMod.THE_HUNTED_ONES}.dat").toFile(), CONFIG_DIRECTORY.resolve("respawn-kit-${ManHuntMod.THE_HUNTED_ONES}.dat").toFile()) {


}