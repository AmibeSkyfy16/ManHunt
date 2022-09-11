package ch.skyfy.manhunt.mixin;

import ch.skyfy.manhunt.callbacks.EntityDamageCallback;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(at = @At("HEAD"), method = "damage", cancellable = true)
    private void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        var livingEntity = (LivingEntity) (Object) this;
        var result = EntityDamageCallback.EVENT.invoker().onDamage(livingEntity, source, amount);
        if(result == ActionResult.FAIL){
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}
