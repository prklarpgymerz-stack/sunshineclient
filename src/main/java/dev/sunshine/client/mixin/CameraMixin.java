package dev.sunshine.client.mixin;

import dev.sunshine.client.SunshineClient;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @org.spongepowered.asm.mixin.Shadow protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update", at = @At("TAIL"))
    private void sunshine$applyFreelookRotation(CallbackInfo ci) {
        if (SunshineClient.isFreelookActive()) {
            this.setRotation(SunshineClient.getFreelookYaw(), SunshineClient.getFreelookPitch());
        }
    }
}
