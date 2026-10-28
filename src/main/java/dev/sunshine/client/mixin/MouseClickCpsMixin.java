package dev.sunshine.client.mixin;

import dev.sunshine.client.SunshineClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.MouseHandler;

@Mixin(MouseHandler.class)
public abstract class MouseClickCpsMixin {
    @Inject(method = "onButton", at = @At("HEAD"))
    private void sunshine$recordClick(long window, net.minecraft.client.input.MouseButtonInfo info, int action, CallbackInfo ci) {
        SunshineClient.recordMouseEvent(info.button(), action);
    }
}
