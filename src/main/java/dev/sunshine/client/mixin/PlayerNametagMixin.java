package dev.sunshine.client.mixin;

import dev.sunshine.client.SunshineConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Client-side replacement for player nameplate text and visibility. */
@Mixin(EntityRenderer.class)
public abstract class PlayerNametagMixin {
    @Inject(method = "getNameTag", at = @At("HEAD"), cancellable = true)
    private void sunshine$replacePlayerNameplate(Entity entity, CallbackInfoReturnable<Component> cir) {
        if (!SunshineConfig.selfNametag || !(entity instanceof Player player)) return;

        Minecraft client = Minecraft.getInstance();
        String nickname = player.getGameProfile().name();
        ChatFormatting color = nickname.equalsIgnoreCase("Prosek_MC") ? ChatFormatting.RED : ChatFormatting.YELLOW;
        cir.setReturnValue(Component.literal("☀ " + nickname).withStyle(color));
    }

    @Inject(method = "shouldShowName", at = @At("HEAD"), cancellable = true)
    private void sunshine$controlPlayerNameplateVisibility(Entity entity, double squaredDistanceToCamera,
                                                            CallbackInfoReturnable<Boolean> cir) {
        if (!SunshineConfig.selfNametag || !(entity instanceof Player player)) return;

        Minecraft client = Minecraft.getInstance();
        if (player == client.player) {
            // Self tag must be hidden in first person and forced visible in third person.
            cir.setReturnValue(client.options.getCameraType() != CameraType.FIRST_PERSON);
        }
    }
}
