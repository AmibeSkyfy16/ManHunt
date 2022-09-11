package ch.skyfy.manhunt.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.ActionResult

fun interface PlayerMoveCallback {

    data class MoveData(val lastX: Double, val lastY: Double, val lastZ: Double, val updatedX: Double, val updatedY: Double, val updatedZ: Double)

    companion object{
        @JvmField
        val EVENT: Event<PlayerMoveCallback> = EventFactory.createArrayBacked(PlayerMoveCallback::class.java) { listeners ->
            PlayerMoveCallback { moveData, player ->
                for (listener in listeners) {
                    val result = listener.onMove(moveData, player)
                    if(result != ActionResult.PASS)return@PlayerMoveCallback result
                }
                ActionResult.PASS
            }
        }
    }

    fun onMove(moveData: MoveData, player: ServerPlayerEntity): ActionResult

}