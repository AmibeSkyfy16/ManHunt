package ch.skyfy.manhunt.logic

import ch.skyfy.jsonconfiglib.ConfigManager
import ch.skyfy.jsonconfiglib.update
import ch.skyfy.manhunt.callbacks.TimeUpdatedCallback
import ch.skyfy.manhunt.config.persistent.Persistent
import ch.skyfy.manhunt.config.persistent.TimelinePersistent
import net.silkmc.silk.core.kotlin.ticks
import net.silkmc.silk.core.task.infiniteMcCoroutineTask

class Timeline {

    fun startTimer() {
        infiniteMcCoroutineTask(sync = false, client = false, period = 1.ticks) {
            val configData = Persistent.TIMELINE_PERSISTENT
            val timelinePersistent = configData.serializableData

            if (timelinePersistent.timeOfDay >= 20 * 1200) { // every 24000 tick (1 one day in minecraft vanilla)
                configData.update(TimelinePersistent::timeOfDay, 0)
                configData.update(TimelinePersistent::day, 1)
            }

            val previousMinutes = timelinePersistent.minutes

            configData.update(TimelinePersistent::minutes,  (timelinePersistent.timeOfDay / 1200.0).toInt())
            configData.update(TimelinePersistent::seconds, (((timelinePersistent.timeOfDay / 1200.0) - timelinePersistent.minutes) * 60).toInt())

            if (previousMinutes != timelinePersistent.minutes) ConfigManager.save(Persistent.TIMELINE_PERSISTENT) // Save to file every minute to prevent losing data if game crashing

            configData.update(TimelinePersistent::timeOfDay, timelinePersistent.timeOfDay + 1)

            TimeUpdatedCallback.EVENT.invoker().onTimeUpdated(timelinePersistent.day, timelinePersistent.minutes, timelinePersistent.seconds, timelinePersistent.timeOfDay)
        }
    }
}