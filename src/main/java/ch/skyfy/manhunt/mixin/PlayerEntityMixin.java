package ch.skyfy.manhunt.mixin;

import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {

    @Inject(method = "getAttackCooldownProgressPerTick", at = @At("RETURN"), cancellable = true)
    public void fun(CallbackInfoReturnable<Float> cir){
        cir.setReturnValue(0f);
        cir.cancel();
    }

}
