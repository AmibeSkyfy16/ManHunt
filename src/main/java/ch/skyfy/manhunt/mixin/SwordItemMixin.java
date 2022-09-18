package ch.skyfy.manhunt.mixin;

import net.minecraft.item.SwordItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(SwordItem.class)
public class SwordItemMixin {

//    @ModifyArg(
//            method = "<init>",
//            at = @At(
//                    value = "INVOKE",
//                    target = "Lnet/minecraft/entity/attribute/EntityAttributeModifier;<init>(Ljava/util/UUID;Ljava/lang/String;DLnet/minecraft/entity/attribute/EntityAttributeModifier$Operation;)V",
//                    ordinal = 1
//            ),
//            index = 2
//    )
//    double init(double value) {
//        System.out.println("default value: " + value);
////        System.out.println("0.0 is now the new default speed");
//        return value;
//    }

}
