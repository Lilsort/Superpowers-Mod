package com.lilsort.superpowers.client.mixin;

import com.lilsort.superpowers.client.flight.FlightClientPlayer;
import com.lilsort.superpowers.flight.FlightPlayer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.lilsort.superpowers.client.sound.FlightSoundManager;

@Mixin(AbstractClientPlayer.class)
public abstract class FlightClientPlayerMixin implements FlightClientPlayer {
    
    @Unique
    private float sp_supermanProgress = 0.0f;
    @Unique
    private float sp_supermanProgressO = 0.0f;
    
    @Unique
    private float sp_hoverTime = 0.0f;
    @Unique
    private float sp_hoverTimeO = 0.0f;
    
    @Unique
    private float sp_flightAnimWeight = 1.0f;
    @Unique
    private float sp_flightAnimWeightO = 1.0f;

    @Unique
    private float sp_flightMoveYaw = 0.0f;
    @Unique
    private float sp_flightMoveYawO = 0.0f;

    @Unique
    private float sp_flightCameraRoll = 0.0f;
    @Unique
    private float sp_flightCameraRollO = 0.0f;

    @Unique
    private float sp_flightBodyRoll = 0.0f;
    @Unique
    private float sp_flightBodyRollO = 0.0f;
    
    @Unique
    private float sp_lastYRot = 0.0f;
    
    @Unique
    private float sp_flightMoveProgress = 0.0f;
    @Unique
    private float sp_flightMoveProgressO = 0.0f;

    @Unique
    private boolean sp_wasSprinting = false;

    @Inject(method = "tick", at = @At("TAIL"))
    private void superpowers$clientTick(CallbackInfo ci) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        FlightPlayer flightPlayer = (FlightPlayer) player;

        this.sp_supermanProgressO = this.sp_supermanProgress;
        this.sp_hoverTimeO = this.sp_hoverTime;
        this.sp_flightAnimWeightO = this.sp_flightAnimWeight;
        this.sp_flightMoveYawO = this.sp_flightMoveYaw;
        this.sp_flightCameraRollO = this.sp_flightCameraRoll;
        this.sp_flightBodyRollO = this.sp_flightBodyRoll;
        this.sp_flightMoveProgressO = this.sp_flightMoveProgress;

        boolean isLocalPlayer = player == net.minecraft.client.Minecraft.getInstance().player;
        boolean isFlying = isLocalPlayer ? player.getAbilities().flying : flightPlayer.isActuallyFlying();
        boolean isSprinting = isLocalPlayer ? player.isSprinting() : flightPlayer.isActuallySprinting();
        boolean isSuperFlightEnabled = flightPlayer.isSuperFlightEnabled();

        // Tick loop sounds
        if (isSuperFlightEnabled) {
            FlightSoundManager.tick(player, isFlying);
        }

        // Superman progression and Boost sounds
        if (isSuperFlightEnabled && isFlying) {
            if (isSprinting) {
                this.sp_supermanProgress += 0.1f;
                if (!this.sp_wasSprinting) {
                    player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
                            com.lilsort.superpowers.sound.FlightSounds.FLIGHT_BOOST,
                            net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F, false);
                    player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
                            com.lilsort.superpowers.sound.FlightSounds.FLIGHT_BOOST_SUPER,
                            net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F, false);
                }
            } else {
                this.sp_supermanProgress -= 0.1f;
            }
            this.sp_wasSprinting = isSprinting;
        } else {
            this.sp_supermanProgress -= 0.1f;
            this.sp_wasSprinting = false;
        }
        this.sp_supermanProgress = Mth.clamp(this.sp_supermanProgress, 0.0f, 1.0f);

        // Hover progression
        // We only bob if flying, but not moving significantly (hovering)
        double speed = player.getDeltaMovement().lengthSqr();
        if (isSuperFlightEnabled && isFlying && speed < 0.01) {
            this.sp_hoverTime += 1.0f;
        } else {
            // Smoothly return hoverTime to 0? Or just leave it and let it resume later. 
            // Better let it continue to tick but don't apply it if moving. Or smoothly return to neutral.
            // Let's just keep ticking it so we can fade out the weight.
            this.sp_hoverTime += 1.0f; 
        }

        // Flight animation weight (fades out when not flying)
        if (!isFlying) {
            this.sp_flightAnimWeight -= 0.15f;
        } else {
            this.sp_flightAnimWeight += 0.15f;
        }
        this.sp_flightAnimWeight = Mth.clamp(this.sp_flightAnimWeight, 0.0f, 1.0f);

        // Track movement yaw (for strafing/backward animations)
        // Use position deltas (xo, zo) instead of getDeltaMovement() as delta movement 
        // can be zero for remote players or consumed during the tick.
        double dx = player.getX() - player.xo;
        double dz = player.getZ() - player.zo;
        
        float horizontalSpeed = (float) Math.sqrt(dx * dx + dz * dz);
        float targetMoveProgress = Math.min(horizontalSpeed * 6.0f, 1.0f);
        if (targetMoveProgress < 0.15f) targetMoveProgress = 0.0f; // Snap to hover
        this.sp_flightMoveProgress += (targetMoveProgress - this.sp_flightMoveProgress) * 0.2f;

        if (horizontalSpeed > 0.02f) { 
            float absoluteMoveYaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
            float targetRelativeYaw = net.minecraft.util.Mth.wrapDegrees(absoluteMoveYaw - player.yBodyRot);
            this.sp_flightMoveYaw += net.minecraft.util.Mth.wrapDegrees(targetRelativeYaw - this.sp_flightMoveYaw) * 0.2f;
        } else {
            // When completely stopped, slowly return to forward (0 relative)
            this.sp_flightMoveYaw += net.minecraft.util.Mth.wrapDegrees(0.0f - this.sp_flightMoveYaw) * 0.1f;
        }
        
        this.sp_flightMoveYaw = net.minecraft.util.Mth.wrapDegrees(this.sp_flightMoveYaw);

        // Camera and Body roll during sprint flight
        if (isFlying && isSprinting) {
            float deltaYaw = player.getYRot() - this.sp_lastYRot;
            
            // Camera roll: clamped to 50
            float targetCamRoll = deltaYaw * 6.0f;
            targetCamRoll = Mth.clamp(targetCamRoll, -50.0f, 50.0f);
            this.sp_flightCameraRoll += (targetCamRoll - this.sp_flightCameraRoll) * 0.2f;

            // Body roll: clamped to 95 degrees max
            // Inverted (-deltaYaw) so turning left banks left (back up, belly down)
            float targetBodyRoll = -deltaYaw * 12.0f; 
            targetBodyRoll = Mth.clamp(targetBodyRoll, -95.0f, 95.0f);
            this.sp_flightBodyRoll += (targetBodyRoll - this.sp_flightBodyRoll) * 0.15f;
        } else {
            this.sp_flightCameraRoll += (0.0f - this.sp_flightCameraRoll) * 0.1f;
            
            float centerDiff = net.minecraft.util.Mth.wrapDegrees(0.0f - this.sp_flightBodyRoll);
            this.sp_flightBodyRoll += centerDiff * 0.1f;
        }
        
        this.sp_flightBodyRoll = net.minecraft.util.Mth.clamp(this.sp_flightBodyRoll, -95.0f, 95.0f);
        
        this.sp_lastYRot = player.getYRot();
    }

    @Override
    public float getSupermanProgress(float partialTicks) {
        return Mth.lerp(partialTicks, this.sp_supermanProgressO, this.sp_supermanProgress);
    }

    @Override
    public float getHoverBobTime(float partialTicks) {
        return Mth.lerp(partialTicks, this.sp_hoverTimeO, this.sp_hoverTime);
    }

    @Override
    public float getFlightAnimWeight(float partialTicks) {
        return Mth.lerp(partialTicks, this.sp_flightAnimWeightO, this.sp_flightAnimWeight);
    }

    @Override
    public float getFlightMoveYaw(float partialTicks) {
        float diff = net.minecraft.util.Mth.wrapDegrees(this.sp_flightMoveYaw - this.sp_flightMoveYawO);
        return this.sp_flightMoveYawO + diff * partialTicks;
    }

    @Override
    public float getFlightMoveProgress(float partialTicks) {
        return net.minecraft.util.Mth.lerp(partialTicks, this.sp_flightMoveProgressO, this.sp_flightMoveProgress);
    }

    @Override
    public float getFlightCameraRoll(float partialTicks) {
        return Mth.lerp(partialTicks, this.sp_flightCameraRollO, this.sp_flightCameraRoll);
    }

    @Override
    public float getFlightBodyRoll(float partialTicks) {
        // Must wrap degrees when lerping manually to prevent 360 jumps
        float diff = net.minecraft.util.Mth.wrapDegrees(this.sp_flightBodyRoll - this.sp_flightBodyRollO);
        return this.sp_flightBodyRollO + diff * partialTicks;
    }
}
