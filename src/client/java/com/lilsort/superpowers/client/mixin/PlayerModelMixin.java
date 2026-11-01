package com.lilsort.superpowers.client.mixin;

import com.lilsort.superpowers.client.flight.FlightClientPlayer;
import com.lilsort.superpowers.flight.FlightPlayer;
import com.lilsort.superpowers.grab.GrabPlayer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin<T extends LivingEntity> extends HumanoidModel<T> {

    @org.spongepowered.asm.mixin.Shadow
    public net.minecraft.client.model.geom.ModelPart leftSleeve;
    @org.spongepowered.asm.mixin.Shadow
    public net.minecraft.client.model.geom.ModelPart rightSleeve;
    @org.spongepowered.asm.mixin.Shadow
    public net.minecraft.client.model.geom.ModelPart leftPants;
    @org.spongepowered.asm.mixin.Shadow
    public net.minecraft.client.model.geom.ModelPart rightPants;
    @org.spongepowered.asm.mixin.Shadow
    public net.minecraft.client.model.geom.ModelPart jacket;

    public PlayerModelMixin(net.minecraft.client.model.geom.ModelPart root) {
        super(root);
    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("HEAD"))
    private void superpowers$disableCrouchWhenFlying(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (entity instanceof Player player && player instanceof FlightPlayer flightPlayer) {
            boolean isLocalPlayer = player == net.minecraft.client.Minecraft.getInstance().player;
            boolean isFlying = isLocalPlayer ? player.getAbilities().flying : flightPlayer.isActuallyFlying();
            if (isFlying && flightPlayer.isSuperFlightEnabled()) {
                this.crouching = false;
            }
        }
    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void superpowers$setupFlightAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof Player player)) return;
        if (!(player instanceof FlightPlayer flightPlayer)) return;
        
        if (!flightPlayer.isSuperFlightEnabled()) {
            // Apply grab pose even when super flight is disabled (vanilla movement/flight)
            float grabWeight = 0.0f;
            if (entity instanceof GrabPlayer gp) {
                boolean isGrabbing = gp.getGrabbedEntity() != null || gp.isTryingToGrab();
                grabWeight = com.lilsort.superpowers.client.render.animation.GrabAnimationManager.calculateWeight(entity, isGrabbing);
            }
            if (grabWeight > 0.001F) {
                float cameraPitchRad = entity.getXRot() * (float)Math.PI / 180.0F;
                float targetPitch = -1.8453F + cameraPitchRad;
                float targetYaw = this.head.yRot + 0.35F;
                float targetRoll = 0.0F;

                this.leftArm.xRot = net.minecraft.util.Mth.lerp(grabWeight, this.leftArm.xRot, targetPitch);
                this.leftArm.yRot = net.minecraft.util.Mth.lerp(grabWeight, this.leftArm.yRot, targetYaw);
                this.leftArm.zRot = net.minecraft.util.Mth.lerp(grabWeight, this.leftArm.zRot, targetRoll);
                this.leftSleeve.copyFrom(this.leftArm);
            }
            return;
        }

        FlightClientPlayer fcp = (FlightClientPlayer) player;
        
        // This is called every frame, we can calculate the posing here
        float partialTicks = net.minecraft.client.Minecraft.getInstance().getFrameTime();
        float sprintProgress = fcp.getSupermanProgress(partialTicks);
        float w = fcp.getFlightAnimWeight(partialTicks);

        // Store original vanilla rotations
        float oRArmX = this.rightArm.xRot, oRArmY = this.rightArm.yRot, oRArmZ = this.rightArm.zRot;
        float oLArmX = this.leftArm.xRot, oLArmY = this.leftArm.yRot, oLArmZ = this.leftArm.zRot;
        float oRLegX = this.rightLeg.xRot, oRLegY = this.rightLeg.yRot, oRLegZ = this.rightLeg.zRot;
        float oRLegYPos = this.rightLeg.y, oRLegZPos = this.rightLeg.z, oRLegXPos = this.rightLeg.x;
        float oLLegX = this.leftLeg.xRot, oLLegY = this.leftLeg.yRot, oLLegZ = this.leftLeg.zRot;
        float oLLegYPos = this.leftLeg.y, oLLegZPos = this.leftLeg.z, oLLegXPos = this.leftLeg.x;
        float oHeadX = this.head.xRot;

        // We will lerp between 3 states: Hover, Forward Flight, Sprint
        // getDeltaMovement() on client can be unreliable,
        float moveProgress = fcp.getFlightMoveProgress(partialTicks);
        // If sprinting, it overrides moveProgress visually
        
        // --- 1. Hover Pose (moveProgress = 0, sprintProgress = 0) ---
        // Dynamically bobs using a sine wave based on ageInTicks
        float S = (float) Math.sin(ageInTicks / 10.0f);
        float hRArmX = -0.175f - S * 0.020f, hRArmY = 0.175f, hRArmZ = 0.157f + S * 0.021f;
        float hLArmX = 0.087f + S * 0.016f, hLArmY = -0.349f, hLArmZ = -0.227f - S * 0.035f;
        float hRLegX = 0.349f + S * 0.070f, hRLegY = 0.087f, hRLegZ = 0.0f;
        float hRLegPosY = 11.0f - S * 0.2f, hRLegPosZ = -2.4f - S * 0.75f, hRLegPosX = -2.3f;
        float hLLegX = 0.0875f - S * 0.0525f, hLLegY = -0.140f, hLLegZ = 0.0f;
        float hLLegPosY = 11.5f - S * 0.1f, hLLegPosZ = -0.9f, hLLegPosX = 2.1f;
        
        // --- 2. Directional Flight Poses (moveProgress = 1, sprintProgress = 0) ---
        // Forward (W)
        float fwRArmX = 0.106f, fwRArmY = 0.0f, fwRArmZ = 0.158f;
        float fwLArmX = 0.106f, fwLArmY = 0.0f, fwLArmZ = -0.158f;
        float fwRLegX = 0.317f, fwRLegY = 0.0f, fwRLegZ = 0.053f;
        float fwLLegX = 0.211f, fwLLegY = 0.0f, fwLLegZ = -0.053f;
        
        // Right (D)
        float rRArmX = 0.011f, rRArmY = 0.0f, rRArmZ = -0.006f;
        float rLArmX = 0.149f, rLArmY = 0.0f, rLArmZ = -0.270f;
        float rRLegX = 0.239f, rRLegY = 0.0f, rRLegZ = -0.237f;
        float rLLegX = 0.160f, rLLegY = 0.0f, rLLegZ = -0.316f;

        // Backward (S)
        float bwRArmX = -0.141f, bwRArmY = 0.0f, bwRArmZ = -0.074f;
        float bwLArmX = 0.038f, bwLArmY = 0.0f, bwLArmZ = -0.282f;
        float bwRLegX = -0.154f, bwRLegY = 0.0f, bwRLegZ = -0.331f;
        float bwLLegX = -0.103f, bwLLegY = 0.0f, bwLLegZ = -0.382f;

        // Left (A)
        float lRArmX = 0.146f, lRArmY = 0.0f, lRArmZ = 0.266f;
        float lLArmX = 0.010f, lLArmY = 0.0f, lLArmZ = 0.005f;
        float lRLegX = 0.235f, lRLegY = 0.0f, lRLegZ = 0.310f;
        float lLLegX = 0.156f, lLLegY = 0.0f, lLLegZ = 0.232f;

        // Calculate directional weights based on flightMoveYaw
        float diffYaw = fcp.getFlightMoveYaw(partialTicks);
        float diffRad = (float) Math.toRadians(diffYaw);
        
        float wF = Math.max(0.0f, (float) Math.cos(diffRad));
        float wB = Math.max(0.0f, -(float) Math.cos(diffRad));
        float wR = Math.max(0.0f, (float) Math.sin(diffRad));
        float wL = Math.max(0.0f, -(float) Math.sin(diffRad));

        float totalW = wF + wB + wR + wL;
        if (totalW > 0.001f) {
            wF /= totalW; wB /= totalW; wR /= totalW; wL /= totalW;
        } else {
            wF = 1.0f;
        }

        // Blended Directional Flight Pose
        float fRArmX = fwRArmX * wF + bwRArmX * wB + rRArmX * wR + lRArmX * wL;
        float fRArmY = fwRArmY * wF + bwRArmY * wB + rRArmY * wR + lRArmY * wL;
        float fRArmZ = fwRArmZ * wF + bwRArmZ * wB + rRArmZ * wR + lRArmZ * wL;

        float fLArmX = fwLArmX * wF + bwLArmX * wB + rLArmX * wR + lLArmX * wL;
        float fLArmY = fwLArmY * wF + bwLArmY * wB + rLArmY * wR + lLArmY * wL;
        float fLArmZ = fwLArmZ * wF + bwLArmZ * wB + rLArmZ * wR + lLArmZ * wL;

        float fRLegX = fwRLegX * wF + bwRLegX * wB + rRLegX * wR + lRLegX * wL;
        float fRLegY = fwRLegY * wF + bwRLegY * wB + rRLegY * wR + lRLegY * wL;
        float fRLegZ = fwRLegZ * wF + bwRLegZ * wB + rRLegZ * wR + lRLegZ * wL;

        float fLLegX = fwLLegX * wF + bwLLegX * wB + rLLegX * wR + lLLegX * wL;
        float fLLegY = fwLLegY * wF + bwLLegY * wB + rLLegY * wR + lLLegY * wL;
        float fLLegZ = fwLLegZ * wF + bwLLegZ * wB + rLLegZ * wR + lLLegZ * wL;

        // Position offsets are the same for all directional flights
        float fRLegPosY = 12.0f, fRLegPosZ = 0.1f, fRLegPosX = -1.9f;
        float fLLegPosY = 12.0f, fLLegPosZ = 0.1f, fLLegPosX = 1.9f;
        
        // --- 3. Sprint Pose (sprintProgress = 1) ---
        // Angles mapped directly from Homelander's boost/default.fsk:
        float sRArmX = -2.879f, sRArmY = 0.261f, sRArmZ = 0.034f;
        float sLArmX = 0.139f, sLArmY = -0.314f, sLArmZ = -0.209f;
        float sRLegX = 0.261f, sRLegY = 0.034f, sRLegZ = 0.052f;
        float sRLegPosY = 12.0f, sRLegPosZ = 0.1f, sRLegPosX = -1.9f;
        float sLLegX = 0.261f, sLLegY = -0.034f, sLLegZ = -0.052f;
        float sLLegPosY = 12.0f, sLLegPosZ = 0.1f, sLLegPosX = 1.9f;
        
        // First, lerp between Hover and Forward Flight based on moveProgress
        float curRArmX = net.minecraft.util.Mth.lerp(moveProgress, hRArmX, fRArmX);
        float curRArmY = net.minecraft.util.Mth.lerp(moveProgress, hRArmY, fRArmY);
        float curRArmZ = net.minecraft.util.Mth.lerp(moveProgress, hRArmZ, fRArmZ);
        
        float curLArmX = net.minecraft.util.Mth.lerp(moveProgress, hLArmX, fLArmX);
        float curLArmY = net.minecraft.util.Mth.lerp(moveProgress, hLArmY, fLArmY);
        float curLArmZ = net.minecraft.util.Mth.lerp(moveProgress, hLArmZ, fLArmZ);
        
        float curRLegX = net.minecraft.util.Mth.lerp(moveProgress, hRLegX, fRLegX);
        float curRLegY = net.minecraft.util.Mth.lerp(moveProgress, hRLegY, fRLegY);
        float curRLegZ = net.minecraft.util.Mth.lerp(moveProgress, hRLegZ, fRLegZ);
        float curRLegPosY = net.minecraft.util.Mth.lerp(moveProgress, hRLegPosY, fRLegPosY);
        float curRLegPosZ = net.minecraft.util.Mth.lerp(moveProgress, hRLegPosZ, fRLegPosZ);
        float curRLegPosX = net.minecraft.util.Mth.lerp(moveProgress, hRLegPosX, fRLegPosX);
        
        float curLLegX = net.minecraft.util.Mth.lerp(moveProgress, hLLegX, fLLegX);
        float curLLegY = net.minecraft.util.Mth.lerp(moveProgress, hLLegY, fLLegY);
        float curLLegZ = net.minecraft.util.Mth.lerp(moveProgress, hLLegZ, fLLegZ);
        float curLLegPosY = net.minecraft.util.Mth.lerp(moveProgress, hLLegPosY, fLLegPosY);
        float curLLegPosZ = net.minecraft.util.Mth.lerp(moveProgress, hLLegPosZ, fLLegPosZ);
        float curLLegPosX = net.minecraft.util.Mth.lerp(moveProgress, hLLegPosX, fLLegPosX);

        // Then, lerp to Sprint Pose based on sprintProgress
        float flightRArmX = net.minecraft.util.Mth.lerp(sprintProgress, curRArmX, sRArmX);
        float flightRArmY = net.minecraft.util.Mth.lerp(sprintProgress, curRArmY, sRArmY);
        float flightRArmZ = net.minecraft.util.Mth.lerp(sprintProgress, curRArmZ, sRArmZ);
        
        float flightLArmX = net.minecraft.util.Mth.lerp(sprintProgress, curLArmX, sLArmX);
        float flightLArmY = net.minecraft.util.Mth.lerp(sprintProgress, curLArmY, sLArmY);
        float flightLArmZ = net.minecraft.util.Mth.lerp(sprintProgress, curLArmZ, sLArmZ);
        
        float flightRLegX = net.minecraft.util.Mth.lerp(sprintProgress, curRLegX, sRLegX);
        float flightRLegY = net.minecraft.util.Mth.lerp(sprintProgress, curRLegY, sRLegY);
        float flightRLegZ = net.minecraft.util.Mth.lerp(sprintProgress, curRLegZ, sRLegZ);
        float flightRLegYPos = net.minecraft.util.Mth.lerp(sprintProgress, curRLegPosY, sRLegPosY);
        float flightRLegZPos = net.minecraft.util.Mth.lerp(sprintProgress, curRLegPosZ, sRLegPosZ);
        float flightRLegXPos = net.minecraft.util.Mth.lerp(sprintProgress, curRLegPosX, sRLegPosX);
        
        float flightLLegX = net.minecraft.util.Mth.lerp(sprintProgress, curLLegX, sLLegX);
        float flightLLegY = net.minecraft.util.Mth.lerp(sprintProgress, curLLegY, sLLegY);
        float flightLLegZ = net.minecraft.util.Mth.lerp(sprintProgress, curLLegZ, sLLegZ);
        float flightLLegYPos = net.minecraft.util.Mth.lerp(sprintProgress, curLLegPosY, sLLegPosY);
        float flightLLegZPos = net.minecraft.util.Mth.lerp(sprintProgress, curLLegPosZ, sLLegPosZ);
        float flightLLegXPos = net.minecraft.util.Mth.lerp(sprintProgress, curLLegPosX, sLLegPosX);

        // Compensate head pitch based on body pitch
        // Normal flight: counteract the body's pitch (-15 degrees) so the head stays straight
        float basePitchRad = moveProgress * -0.2618f * w;
        float baseHeadX = oHeadX + basePitchRad;
        
        // Sprint flight: use the dramatic -50 degree pose from the fsk file
        float sprintHeadX = -0.873f;
        
        float flightHeadX = net.minecraft.util.Mth.lerp(sprintProgress, baseHeadX, sprintHeadX);

        // Apply weight to smoothly transition between vanilla pose and flight pose
        this.rightArm.xRot = net.minecraft.util.Mth.lerp(w, oRArmX, flightRArmX);
        this.rightArm.yRot = net.minecraft.util.Mth.lerp(w, oRArmY, flightRArmY);
        this.rightArm.zRot = net.minecraft.util.Mth.lerp(w, oRArmZ, flightRArmZ);
        
        float currentFlightLArmX = net.minecraft.util.Mth.lerp(w, oLArmX, flightLArmX);
        float currentFlightLArmY = net.minecraft.util.Mth.lerp(w, oLArmY, flightLArmY);
        float currentFlightLArmZ = net.minecraft.util.Mth.lerp(w, oLArmZ, flightLArmZ);

        float grabWeight = 0.0f;
        if (entity instanceof GrabPlayer gp) {
            boolean isGrabbing = gp.getGrabbedEntity() != null || gp.isTryingToGrab();
            grabWeight = com.lilsort.superpowers.client.render.animation.GrabAnimationManager.calculateWeight(entity, isGrabbing);
        }
        
        if (grabWeight > 0.001F) {
            float rendererPitchRad = 0.0f;
            float rendererRollRad = 0.0f;
            if (entity instanceof FlightClientPlayer fcp2) {
                float pt = net.minecraft.client.Minecraft.getInstance().getFrameTime();
                float sp = fcp2.getSupermanProgress(pt);
                float fw = fcp2.getFlightAnimWeight(pt);
                float mp = fcp2.getFlightMoveProgress(pt);
                float roll2 = fcp2.getFlightBodyRoll(pt);
                
                float bPitch = mp * -15.0f * fw;
                float sPitch = sp * (-90.0f - entity.getXRot()) * fw;
                
                rendererPitchRad = (float) Math.toRadians(net.minecraft.util.Mth.lerp(sp, bPitch, sPitch));
                rendererRollRad = (float) Math.toRadians(sp * roll2);
            }
            float cameraPitchRad = entity.getXRot() * (float)Math.PI / 180.0F;
            float targetPitch = -1.8453F + cameraPitchRad + rendererPitchRad;
            float targetYaw = this.head.yRot + 0.35F;
            float targetRoll = 0.0F - rendererRollRad;

            this.leftArm.xRot = net.minecraft.util.Mth.lerp(grabWeight, currentFlightLArmX, targetPitch);
            this.leftArm.yRot = net.minecraft.util.Mth.lerp(grabWeight, currentFlightLArmY, targetYaw);
            this.leftArm.zRot = net.minecraft.util.Mth.lerp(grabWeight, currentFlightLArmZ, targetRoll);
        } else {
            this.leftArm.xRot = currentFlightLArmX;
            this.leftArm.yRot = currentFlightLArmY;
            this.leftArm.zRot = currentFlightLArmZ;
        }
        
        this.rightLeg.xRot = net.minecraft.util.Mth.lerp(w, oRLegX, flightRLegX);
        this.rightLeg.yRot = net.minecraft.util.Mth.lerp(w, oRLegY, flightRLegY);
        this.rightLeg.zRot = net.minecraft.util.Mth.lerp(w, oRLegZ, flightRLegZ);
        this.rightLeg.y = net.minecraft.util.Mth.lerp(w, oRLegYPos, flightRLegYPos);
        this.rightLeg.z = net.minecraft.util.Mth.lerp(w, oRLegZPos, flightRLegZPos);
        this.rightLeg.x = net.minecraft.util.Mth.lerp(w, oRLegXPos, flightRLegXPos);
        
        this.leftLeg.xRot = net.minecraft.util.Mth.lerp(w, oLLegX, flightLLegX);
        this.leftLeg.yRot = net.minecraft.util.Mth.lerp(w, oLLegY, flightLLegY);
        this.leftLeg.zRot = net.minecraft.util.Mth.lerp(w, oLLegZ, flightLLegZ);
        this.leftLeg.y = net.minecraft.util.Mth.lerp(w, oLLegYPos, flightLLegYPos);
        this.leftLeg.z = net.minecraft.util.Mth.lerp(w, oLLegZPos, flightLLegZPos);
        this.leftLeg.x = net.minecraft.util.Mth.lerp(w, oLLegXPos, flightLLegXPos);
        
        this.head.xRot = net.minecraft.util.Mth.lerp(w, oHeadX, flightHeadX);
        this.hat.xRot = this.head.xRot;

        // Apply updated limb rotations to the second layer (sleeves, pants, jacket)
        this.rightSleeve.copyFrom(this.rightArm);
        this.leftSleeve.copyFrom(this.leftArm);
        this.rightPants.copyFrom(this.rightLeg);
        this.leftPants.copyFrom(this.leftLeg);
        this.jacket.copyFrom(this.body);
    }
}
