package ch.skyfy.manhunt.mixin;

import ch.skyfy.manhunt.callbacks.PlayerMoveCallback;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {

    @Shadow
    public ServerPlayerEntity player;

    @Shadow
    private double lastTickX;
    @Shadow
    private double lastTickY;
    @Shadow
    private double lastTickZ;
    @Shadow
    private double updatedX;
    @Shadow
    private double updatedY;
    @Shadow
    private double updatedZ;

    @Inject(
            method = "onPlayerMove(Lnet/minecraft/network/packet/c2s/play/PlayerMoveC2SPacket;)V",
            at = @At(
                    value = "INVOKE",
                    // Thanks to Liach for helping me out!
                    // Thanks for FlashyReese for helping me out!
                    target = "net/minecraft/network/NetworkThreadUtils.forceMainThread(Lnet/minecraft/network/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/server/world/ServerWorld;)V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void onPlayerMove(PlayerMoveC2SPacket playerMoveC2SPacket, CallbackInfo ci) {
        var result = PlayerMoveCallback.EVENT.invoker().onMove(
                new PlayerMoveCallback.MoveData(lastTickX,
                        lastTickY,
                        lastTickZ,
                        updatedX,
                        updatedY,
                        updatedZ), player);

        if (result == ActionResult.FAIL) {
            // A bit ugly, I know. (we need to update player position)
            this.player.networkHandler.requestTeleport(this.player.getX(), this.player.getY(), this.player.getZ(), this.player.getYaw(), this.player.getPitch());
            ci.cancel();
        }
    }

}
