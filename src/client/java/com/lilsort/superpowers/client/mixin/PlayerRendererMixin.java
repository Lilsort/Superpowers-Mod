package com.lilsort.superpowers.client.mixin;

import com.lilsort.superpowers.client.flight.FlightClientPlayer;
import com.lilsort.superpowers.flight.FlightPlayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public class PlayerRendererMixin {

    @Inject(method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFF)V", at = @At("TAIL"))
    private void superpowers$setupFlightRotations(AbstractClientPlayer player, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTicks, CallbackInfo ci) {
        if (!(player instanceof FlightPlayer flightPlayer)) return;
        if (!flightPlayer.isSuperFlightEnabled()) return;

        FlightClientPlayer fcp = (FlightClientPlayer) player;
        float sprintProgress = fcp.getSupermanProgress(partialTicks);
        float hoverBobTime = fcp.getHoverBobTime(partialTicks);
        float w = fcp.getFlightAnimWeight(partialTicks);
        float moveProgress = fcp.getFlightMoveProgress(partialTicks);
        float roll = fcp.getFlightBodyRoll(partialTicks);
        
        // 1. Hover Bobbing (Now handled physically in PlayerTravelMixin)

        // 2. Flight Body Rotation
        // Normal flight pitches slightly forward, sprint flight pitches fully horizontal
        float basePitch = moveProgress * -15.0f * w;
        float sprintPitch = sprintProgress * (-90.0f - player.getXRot()) * w;
        
        float targetPitch = Mth.lerp(sprintProgress, basePitch, sprintPitch);
        float targetRoll = sprintProgress * roll;

        // Shift the entire visual model down during sprint-flight so it aligns with the 0.4 eye height.
        // Without this, the body rotates around y=1.0 and stays there, while the camera is at y=0.4.
        if (sprintProgress > 0.01f) {
            poseStack.translate(0.0D, -0.6D * sprintProgress, 0.0D);
        }

        if (Math.abs(targetPitch) > 0.01f || Math.abs(targetRoll) > 0.01f) {
            // Apply rotations around the center of the body.
            poseStack.translate(0.0D, 1.0D, 0.0D); // Move pivot up
            
            // Apply Z rotation (roll) before X rotation (pitch) so it becomes a flight roll
            if (Math.abs(targetRoll) > 0.01f) {
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(targetRoll));
            }
            
            if (Math.abs(targetPitch) > 0.01f) {
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(targetPitch));
            }
            
            poseStack.translate(0.0D, -1.0D, 0.0D); // Move pivot back
        }
    }
}
