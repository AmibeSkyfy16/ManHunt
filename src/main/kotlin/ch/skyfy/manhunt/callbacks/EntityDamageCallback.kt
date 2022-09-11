package ch.skyfy.manhunt.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.util.ActionResult

fun interface EntityDamageCallback {
    companion object {
        @JvmField
        val EVENT: Event<EntityDamageCallback> = EventFactory.createArrayBacked(EntityDamageCallback::class.java){ listeners ->
            EntityDamageCallback{ livingEntity, damageSource,amount ->
                for(listener in listeners) {
                    val result = listener.onDamage(livingEntity, damageSource, amount)
                    if(result != ActionResult.PASS)return@EntityDamageCallback result
                }
                ActionResult.PASS
            }
        }
    }

    fun onDamage(livingEntity: LivingEntity, damageSource: DamageSource, amount: Float) : ActionResult
}