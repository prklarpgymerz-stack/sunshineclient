package dev.sunshine.client.mixin;

import dev.sunshine.client.SunshineConfig;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Living/player renderers override shouldShowName() and never call the
 * EntityRenderer version, so the hook in PlayerNametagMixin is skipped for
 * players. This repeats the visibility rule on the override that actually runs.
 * require = 0: if the signature differs in a future version this is skipped
 * instead of crashing the game.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingNametagMixin {
    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void sunshine$livingNameplateVisibility(LivingEntity entity, double squaredDistanceToCamera,
                                                    CallbackInfoReturnable<Boolean> cir) {
        if (!SunshineConfig.selfNametag || !(entity instanceof Player player)) return;

        Minecraft client = Minecraft.getInstance();
        if (player == client.player) {
            // Hidden in first person, forced visible in third person.
            cir.setReturnValue(client.options.getCameraType() != CameraType.FIRST_PERSON);
        }
    }
}
