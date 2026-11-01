package com.lilsort.superpowers.client.mixin.grab;

import com.lilsort.superpowers.client.flight.FlightClientPlayer;
import com.lilsort.superpowers.client.render.animation.GrabAnimationManager;
import com.lilsort.superpowers.grab.GrabPlayer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(HumanoidModel.class)
public abstract class GrabModelMixin<T extends LivingEntity> {

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void superpowers$applyGrabPose(T entity, float f, float g, float h, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (entity instanceof GrabPlayer grabPlayer) {
            boolean isGrabbing = grabPlayer.getGrabbedEntity() != null || grabPlayer.isTryingToGrab();
            
            float weight = GrabAnimationManager.calculateWeight(entity, isGrabbing);
            
            if (weight > 0.001F || isGrabbing) {
                HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
                
                // Skip PlayerModel because PlayerModelMixin handles its grab pose (lerping it with the flight pose).
                // This mixin remains active for Armor and other generic HumanoidModels.
                if (model instanceof net.minecraft.client.model.PlayerModel) {
                    return;
                }
                
                float rendererPitchRad = 0.0f;
                float rendererRollRad = 0.0f;

                if (entity instanceof FlightClientPlayer fcp) {
                    float pt = net.minecraft.client.Minecraft.getInstance().getFrameTime();
                    float sprintProgress = fcp.getSupermanProgress(pt);
                    float w = fcp.getFlightAnimWeight(pt);
                    float moveProgress = fcp.getFlightMoveProgress(pt);
                    float roll = fcp.getFlightBodyRoll(pt);
                    
                    float basePitch = moveProgress * -15.0f * w;
                    float sprintPitch = sprintProgress * (-90.0f - entity.getXRot()) * w;
                    
                    float rendererPitchDegrees = net.minecraft.util.Mth.lerp(sprintProgress, basePitch, sprintPitch);
                    float rendererRollDegrees = sprintProgress * roll;
                    
                    rendererPitchRad = (float) Math.toRadians(rendererPitchDegrees);
                    rendererRollRad = (float) Math.toRadians(rendererRollDegrees);
                }

                // If not flying, head.xRot exactly matches camera pitch. 
                // But during flight, head.xRot is overridden, so we use the actual camera pitch (entity.getXRot).
                float cameraPitchRad = entity.getXRot() * (float)Math.PI / 180.0F;
                
                // Subtract renderer rotations to ensure the arm always points straight at the crosshair
                // Note: We ADD rendererPitchRad because the body's pitch is negative when flying forward.
                // Adding it counteracts the body's rotation so the arm stays locked to the camera direction without spinning 360 degrees.
                float targetPitch = -1.8453F + cameraPitchRad + rendererPitchRad;
                float targetYaw = model.head.yRot + 0.35F;
                float targetRoll = 0.0F - rendererRollRad;

                // Smoothly interpolate the left arm
                model.leftArm.xRot = net.minecraft.util.Mth.lerp(weight, model.leftArm.xRot, targetPitch);
                model.leftArm.yRot = net.minecraft.util.Mth.lerp(weight, model.leftArm.yRot, targetYaw);
                model.leftArm.zRot = net.minecraft.util.Mth.lerp(weight, model.leftArm.zRot, targetRoll);
                
                // Copy left arm pose to left sleeve (if applicable)
                if (model instanceof net.minecraft.client.model.PlayerModel<?> playerModel) {
                    playerModel.leftSleeve.copyFrom(model.leftArm);
                }
            }
        }
    }
}
