package ch.skyfy.manhunt.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.util.ActionResult

fun interface TimeUpdatedCallback {

    companion object{
        var EVENT: Event<TimeUpdatedCallback> = EventFactory.createArrayBacked(TimeUpdatedCallback::class.java) { listeners: Array<TimeUpdatedCallback> ->
            TimeUpdatedCallback { day, minutes, seconds, timeOfDay ->
                for (listener in listeners) {
                    val result: ActionResult = listener.onTimeUpdated(day, minutes, seconds, timeOfDay)
                    if (result != ActionResult.PASS) return@TimeUpdatedCallback result
                }
                ActionResult.PASS
            }
        }
    }

    fun onTimeUpdated(day: Int, minutes: Int, seconds: Int, timeOfDay: Int): ActionResult
}