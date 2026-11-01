package com.lilsort.superpowers.client.mixin;

import com.lilsort.superpowers.client.flight.FlightClientPlayer;
import com.lilsort.superpowers.flight.FlightPlayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V", shift = At.Shift.AFTER))
    private void superpowers$applyCameraRoll(float pPartialTicks, long pFinishTimeNanos, PoseStack pMatrixStack, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player instanceof FlightPlayer fp && fp.isSuperFlightEnabled() && mc.player.getAbilities().flying) {
            float roll = ((FlightClientPlayer) mc.player).getFlightCameraRoll(pPartialTicks);
            if (Math.abs(roll) > 0.1f) {
                pMatrixStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(roll));
            }
        }
    }
}
