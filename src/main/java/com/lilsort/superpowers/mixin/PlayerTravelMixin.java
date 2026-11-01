package com.lilsort.superpowers.mixin;

import com.lilsort.superpowers.flight.FlightPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerTravelMixin extends LivingEntity {

    @Unique
    private float sp_currentFlightSpeed = 0.0f;

    protected PlayerTravelMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Unique
    private boolean sp_wasSprintFlying = false;

    /**
     * Override the player's bounding box dimensions directly when sprint-flying.
     * Returns a 0.6x0.6 box (same as swimming) but WITHOUT changing the pose,
     * so our custom flight animations are not affected.
     */
    @Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
    private void superpowers$shrinkHitbox(Pose pose, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<net.minecraft.world.entity.EntityDimensions> cir) {
        Player player = (Player) (Object) this;
        if (player instanceof FlightPlayer fp && fp.isSuperFlightEnabled()
                && player.getAbilities().flying && player.isSprinting()) {
            // 0.6 wide x 0.6 tall — fits through a 1-block gap
            cir.setReturnValue(net.minecraft.world.entity.EntityDimensions.scalable(0.6F, 0.6F));
        }
    }

    /**
     * Override eye height when sprint-flying so the camera sits at the center
     * of the shrunken hitbox instead of at 1.62 (standing eye height).
     * Vanilla Entity.tick() automatically smooth-lerps to this value!
     */
    @Inject(method = "getStandingEyeHeight", at = @At("HEAD"), cancellable = true)
    private void superpowers$lowerEyeHeight(Pose pose, net.minecraft.world.entity.EntityDimensions dimensions, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Float> cir) {
        Player player = (Player) (Object) this;
        if (player instanceof FlightPlayer fp && fp.isSuperFlightEnabled()
                && player.getAbilities().flying && player.isSprinting()) {
            cir.setReturnValue(0.4F); // Camera goes to 0.4, perfectly aligned with 0.6 hitbox
        }
    }

    /**
     * Track sprint-flight state transitions in tick() so refreshDimensions() is always called,
     * even when the player lands or stops flying. This prevents the "stuck small hitbox" bug.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void superpowers$trackSprintFlightHitbox(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        boolean isSprintFlying = false;
        if (player instanceof FlightPlayer fp && fp.isSuperFlightEnabled()
                && player.getAbilities().flying && player.isSprinting()) {
            isSprintFlying = true;
        }
        if (isSprintFlying != sp_wasSprintFlying) {
            sp_wasSprintFlying = isSprintFlying;
            player.refreshDimensions();
        }
    }

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void superpowers$customTravel(Vec3 travelVector, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        FlightPlayer flightPlayer = (FlightPlayer) player;

        if (player.getAbilities().flying && flightPlayer.isSuperFlightEnabled()) {

            if (player.isEffectiveAi() || player.isControlledByLocalInstance()) {
                // Free-cam 3D flight
                double forward = travelVector.z; // W/S (forward/backward)
                double strafe = travelVector.x;  // A/D (left/right)
                double up = travelVector.y;      // Space/Shift (up/down)

                float targetAccel = player.getAbilities().getFlyingSpeed() * 1.5f * flightPlayer.getFlightSpeedMultiplier(); // Slightly slower default
                if (player.isSprinting()) {
                    targetAccel *= 4.0f; // Sprint flight
                }

                // Look vectors
                Vec3 lookVec = player.getLookAngle();
                // Right vector is cross product of look and UP (0,1,0)
                double rightX = -Math.cos(Math.toRadians(player.getYRot()));
                double rightZ = -Math.sin(Math.toRadians(player.getYRot()));
                Vec3 rightVec = new Vec3(rightX, 0, rightZ).normalize();

                Vec3 inputVector = Vec3.ZERO;
                if (forward != 0) {
                    inputVector = inputVector.add(lookVec.scale(forward));
                }
                if (strafe != 0) {
                    inputVector = inputVector.add(rightVec.scale(-strafe)); // Fix inverted left/right
                }
                if (up != 0) {
                    inputVector = inputVector.add(0, up, 0); // Vertical adjustment for space/shift
                }

                Vec3 currentVelocity = player.getDeltaMovement();
                float friction = 0.85f; // Base friction

                if (inputVector.lengthSqr() > 0) {
                    Vec3 normInput = inputVector.normalize();
                    
                    // Ramp up current flight speed (exponential approach)
                    this.sp_currentFlightSpeed += (targetAccel - this.sp_currentFlightSpeed) * 0.08f;
                    
                    // Turn sharpness logic (Air braking)
                    if (currentVelocity.lengthSqr() > 0.01) {
                        Vec3 normVel = currentVelocity.normalize();
                        double dot = normVel.dot(normInput); // 1.0 (straight), 0.0 (90 deg), -1.0 (180 deg)
                        
                        // Map dot from [-1, 1] to [0.0, 1.0]
                        double dotMapped = (dot + 1.0) / 2.0;
                        
                        // Increase friction when turning sharply (lower value = stops faster)
                        friction = (float) net.minecraft.util.Mth.lerp(dotMapped, 0.65, 0.85);
                        
                        // Lose some acceleration momentum on sharp turns
                        this.sp_currentFlightSpeed *= (float) net.minecraft.util.Mth.lerp(dotMapped, 0.92, 1.0);
                    }
                    
                    inputVector = normInput.scale(this.sp_currentFlightSpeed);
                } else {
                    // Decelerate internal speed quickly when no keys are pressed
                    this.sp_currentFlightSpeed += (0.0f - this.sp_currentFlightSpeed) * 0.2f;
                    
                    if (!player.isSprinting() && !player.isShiftKeyDown()) {
                        // Physical hover bobbing when completely still
                        double bob = Math.sin(player.tickCount * 0.1) * 0.005;
                        inputVector = inputVector.add(0, bob, 0);
                    }
                }

                currentVelocity = currentVelocity.scale(friction).add(inputVector);

                player.setDeltaMovement(currentVelocity);
                player.move(MoverType.SELF, currentVelocity);
                
                // CRITICAL: Update walk animation so limbSwingAmount still increments!
                // Pass 0.0f so it calculates speed correctly from position delta, rather than forcing 1.0f (which breaks hover)
                this.updateWalkAnimation(0.0f);
                
                ci.cancel(); // Cancel vanilla travel
            }
        }
    }
}
