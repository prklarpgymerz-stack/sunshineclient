package dev.sunshine.client.mixin;

import dev.sunshine.client.SunshineClient;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @org.spongepowered.asm.mixin.Shadow private double accumulatedDX;
    @org.spongepowered.asm.mixin.Shadow private double accumulatedDY;

    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void sunshine$turnPlayer(double frameTime, CallbackInfo ci) {
        if (SunshineClient.isFreelookActive()) {
            SunshineClient.rotateFreelook(accumulatedDX, accumulatedDY);
            accumulatedDX = 0.0;
            accumulatedDY = 0.0;
            ci.cancel();
        }
    }
}
