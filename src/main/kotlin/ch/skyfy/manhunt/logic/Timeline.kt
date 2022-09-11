package ch.skyfy.manhunt.logic

import ch.skyfy.jsonconfiglib.ConfigManager
import ch.skyfy.manhunt.callbacks.TimeUpdatedCallback
import ch.skyfy.manhunt.logic.persistent.Persistent
import net.silkmc.silk.core.kotlin.ticks
import net.silkmc.silk.core.task.infiniteMcCoroutineTask

class Timeline {

    init {

    }

    fun startTimer(){
        infiniteMcCoroutineTask(sync = false, client = false, period = 1.ticks){
            val timelinePersistent = Persistent.TIMELINE_PERSISTENT.`data`

            if (timelinePersistent.timeOfDay >= 20 * 1200) {
                timelinePersistent.timeOfDay = 0
                timelinePersistent.day += 1
            }

            val previousMinutes = timelinePersistent.minutes

            timelinePersistent.minutes = (timelinePersistent.timeOfDay / 1200.0).toInt()
            timelinePersistent.seconds = (((timelinePersistent.timeOfDay / 1200.0) - timelinePersistent.minutes) * 60).toInt()

            if (previousMinutes != timelinePersistent.minutes) saveData() // Save to file every minute to prevent losing data if game crashing

            timelinePersistent.timeOfDay += 1

            TimeUpdatedCallback.EVENT.invoker().onTimeUpdated(timelinePersistent.day, timelinePersistent.minutes, timelinePersistent.seconds, timelinePersistent.timeOfDay)
        }
    }

    private fun saveData(){
        ConfigManager.save(Persistent.TIMELINE_PERSISTENT)
    }

}