package dev.sunshine.client.mixin;

import dev.sunshine.client.MotionBlurRenderer;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Runs the temporal after-image pass after the world frame has been rendered. */
@Mixin(GameRenderer.class)
public abstract class MotionBlurMixin {
    @Inject(method = "renderLevel", at = @At("TAIL"), require = 0)
    private void sunshine$applyMotionBlur(CallbackInfo ci) {
        MotionBlurRenderer.render();
    }
}
