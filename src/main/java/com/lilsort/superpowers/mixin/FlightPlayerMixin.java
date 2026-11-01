package com.lilsort.superpowers.mixin;

import com.lilsort.superpowers.flight.FlightPlayer;
import com.lilsort.superpowers.laser.LaserPlayer;
import com.lilsort.superpowers.grab.GrabPlayer;
import com.lilsort.superpowers.grab.GrabbedPosSyncS2CPacket;
import com.lilsort.superpowers.network.SuperpowerStateSyncS2CPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class FlightPlayerMixin extends LivingEntity implements FlightPlayer, LaserPlayer, GrabPlayer {

    protected FlightPlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    // --- Flight Fields ---
    @Unique
    private boolean sp_superFlightEnabled = false;
    @Unique
    private float sp_flightSpeed = 1.0f;
    @Unique
    private boolean sp_actuallyFlying = false;
    @Unique
    private boolean sp_actuallySprinting = false;

    // --- Laser Fields ---
    @Unique
    private boolean sp_laserEnabled = false;
    @Unique
    private boolean sp_firingLaser = false;
    @Unique
    private float sp_laserGap = 1.5f;
    @Unique
    private float sp_laserOffset = -3.5f;
    @Unique
    private float sp_laserDelay = 0.0f;
    @Unique
    private float sp_laserMaxDistance = 64.0f;
    @Unique
    private float sp_laserDamage = 10.0f;
    @Unique
    private String sp_laserColor = "FF0000";
    @Unique
    private float sp_leftLaserDist = 64.0f;
    @Unique
    private float sp_rightLaserDist = 64.0f;

    // --- Grab Fields ---
    @Unique
    private boolean sp_canGrab = false;
    @Unique
    private boolean sp_tryingToGrab = false;
    @Unique
    private LivingEntity sp_grabbedEntity = null;

    // --- Laser non-synced fields ---
    @Unique
    private Vec3 sp_leftEyePos = Vec3.ZERO;
    @Unique
    private Vec3 sp_rightEyePos = Vec3.ZERO;
    @Unique
    private Vec3 sp_headLookVector = Vec3.ZERO;
    @Unique
    private Matrix4f sp_headMatrix = new Matrix4f();
    @Unique
    private int sp_chargeTicks = 0;
    @Unique
    private float prevLeftLaserDist = 64.0f;
    @Unique
    private float currentLeftLaserDist = 64.0f;
    @Unique
    private float prevRightLaserDist = 64.0f;
    @Unique
    private float currentRightLaserDist = 64.0f;

    // --- Grab non-synced fields ---
    @Unique
    private Vec3 calculatedHandPos;
    @Unique
    private Vec3 firstPersonLocalHandPos;

    @Unique
    private void superpowers$sync() {
        if (!this.level().isClientSide() && (Object) this instanceof ServerPlayer serverPlayer) {
            if (serverPlayer.connection != null) {
                SuperpowerStateSyncS2CPacket.send(serverPlayer);
            }
        }
    }

    @Unique
    private boolean superpowers$initialized = false;

    @Unique
    private boolean superpowers$isSpectatorSafe() {
        if (!this.superpowers$initialized) {
            return false;
        }
        if ((Object) this instanceof ServerPlayer serverPlayer) {
            if (serverPlayer.connection == null) {
                return false;
            }
        }
        return this.isSpectator();
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void superpowers$saveData(CompoundTag tag, CallbackInfo ci) {
        tag.putBoolean("SuperpowersFlightEnabled", this.sp_superFlightEnabled);
        tag.putFloat("SuperpowersFlightSpeed", this.sp_flightSpeed);

        tag.putBoolean("SuperpowersLaserEnabled", this.sp_laserEnabled);
        tag.putFloat("SuperpowersLaserGap", this.sp_laserGap);
        tag.putFloat("SuperpowersLaserOffset", this.sp_laserOffset);
        tag.putFloat("SuperpowersLaserDelay", this.sp_laserDelay);
        tag.putFloat("SuperpowersLaserDistance", this.sp_laserMaxDistance);
        tag.putFloat("SuperpowersLaserDamage", this.sp_laserDamage);
        tag.putString("SuperpowersLaserColor", this.sp_laserColor);

        tag.putBoolean("superpowers_CanGrab", this.sp_canGrab);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void superpowers$readData(CompoundTag tag, CallbackInfo ci) {
        this.superpowers$initialized = true;
        if (tag.contains("SuperpowersFlightEnabled")) {
            this.setSuperFlightEnabled(tag.getBoolean("SuperpowersFlightEnabled"));
        }
        if (tag.contains("SuperpowersFlightSpeed")) {
            this.setFlightSpeedMultiplier(tag.getFloat("SuperpowersFlightSpeed"));
        }

        if (tag.contains("SuperpowersLaserEnabled")) {
            this.setLaserEnabled(tag.getBoolean("SuperpowersLaserEnabled"));
        }
        if (tag.contains("SuperpowersLaserGap")) {
            this.setLaserGap(tag.getFloat("SuperpowersLaserGap"));
        }
        if (tag.contains("SuperpowersLaserOffset")) {
            this.setLaserOffset(tag.getFloat("SuperpowersLaserOffset"));
        }
        if (tag.contains("SuperpowersLaserDelay")) {
            this.setLaserDelay(tag.getFloat("SuperpowersLaserDelay"));
        }
        if (tag.contains("SuperpowersLaserDistance")) {
            this.setLaserDistance(tag.getFloat("SuperpowersLaserDistance"));
        }
        if (tag.contains("SuperpowersLaserDamage")) {
            this.setLaserDamage(tag.getFloat("SuperpowersLaserDamage"));
        }
        if (tag.contains("SuperpowersLaserColor")) {
            this.setLaserColor(tag.getString("SuperpowersLaserColor"));
        }

        if (tag.contains("superpowers_CanGrab")) {
            setCanGrab(tag.getBoolean("superpowers_CanGrab"));
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void superpowers$tick(CallbackInfo ci) {
        this.superpowers$initialized = true;
        // --- Spectator Check ---
        if (this.superpowers$isSpectatorSafe()) {
            boolean changed = false;
            if (this.sp_firingLaser) { this.sp_firingLaser = false; changed = true; }
            if (this.sp_tryingToGrab) { this.sp_tryingToGrab = false; changed = true; }
            if (this.sp_grabbedEntity != null) {
                this.releaseEntity();
                changed = true;
            }
            if (changed) {
                this.superpowers$sync();
            }
        }

        // --- Flight Tick ---
        if (!this.level().isClientSide) {
            Player player = (Player) (Object) this;
            if (this.isSuperFlightEnabled()) {
                if (!player.getAbilities().mayfly) {
                    player.getAbilities().mayfly = true;
                    player.onUpdateAbilities();
                }
                // Sync flying/sprinting to all clients via setter-driven S2C sync
                boolean flying = player.getAbilities().flying;
                boolean sprinting = player.isSprinting();
                this.setActuallyFlying(flying);
                this.setActuallySprinting(sprinting);
            } else {
                this.setActuallyFlying(false);
                this.setActuallySprinting(false);
            }
        }

        // --- Laser Tick ---
        if (this.isFiringLaser()) {
            this.sp_chargeTicks++;
        } else {
            this.sp_chargeTicks = 0;
        }
        
        if (this.level().isClientSide()) {
            this.prevLeftLaserDist = this.currentLeftLaserDist;
            this.currentLeftLaserDist = this.getLeftLaserDistance();
            this.prevRightLaserDist = this.currentRightLaserDist;
            this.currentRightLaserDist = this.getRightLaserDistance();
        }
    }

    // --- FlightPlayer interface implementation ---

    @Override
    public boolean isSuperFlightEnabled() {
        if (this.superpowers$isSpectatorSafe()) return false;
        return this.sp_superFlightEnabled;
    }

    @Override
    public void setSuperFlightEnabled(boolean enabled) {
        if (this.sp_superFlightEnabled != enabled) {
            this.sp_superFlightEnabled = enabled;
            this.superpowers$sync();
        }
    }

    @Override
    public float getFlightSpeedMultiplier() {
        return this.sp_flightSpeed;
    }

    @Override
    public void setFlightSpeedMultiplier(float multiplier) {
        if (this.sp_flightSpeed != multiplier) {
            this.sp_flightSpeed = multiplier;
            this.superpowers$sync();
        }
    }

    @Override
    public boolean isActuallyFlying() {
        return this.sp_actuallyFlying;
    }

    @Override
    public void setActuallyFlying(boolean flying) {
        if (this.sp_actuallyFlying != flying) {
            this.sp_actuallyFlying = flying;
            this.superpowers$sync();
        }
    }

    @Override
    public boolean isActuallySprinting() {
        return this.sp_actuallySprinting;
    }

    @Override
    public void setActuallySprinting(boolean sprinting) {
        if (this.sp_actuallySprinting != sprinting) {
            this.sp_actuallySprinting = sprinting;
            this.superpowers$sync();
        }
    }

    // --- LaserPlayer interface implementation ---

    @Override
    public boolean isLaserEnabled() {
        if (this.superpowers$isSpectatorSafe()) return false;
        return this.sp_laserEnabled;
    }

    @Override
    public void setLaserEnabled(boolean enabled) {
        if (this.sp_laserEnabled != enabled) {
            this.sp_laserEnabled = enabled;
            this.superpowers$sync();
        }
    }

    @Override
    public boolean isFiringLaser() {
        if (this.superpowers$isSpectatorSafe()) return false;
        return this.sp_firingLaser;
    }

    @Override
    public void setFiringLaser(boolean firing) {
        if (this.sp_firingLaser != firing) {
            this.sp_firingLaser = firing;
            this.superpowers$sync();
        }
    }

    @Override
    public Vec3 getLeftEyePos() {
        return sp_leftEyePos;
    }

    @Override
    public Vec3 getSyncedLeftEyePos() {
        return getLeftEyePos();
    }

    @Override
    public void setLeftEyePos(Vec3 pos) {
        this.sp_leftEyePos = pos;
    }

    @Override
    public Vec3 getRightEyePos() {
        return sp_rightEyePos;
    }

    @Override
    public Vec3 getSyncedRightEyePos() {
        return getRightEyePos();
    }

    @Override
    public void setRightEyePos(Vec3 pos) {
        this.sp_rightEyePos = pos;
    }

    @Override
    public Vec3 getHeadLookVector() {
        return sp_headLookVector;
    }

    @Override
    public Vec3 getSyncedHeadLookVector() {
        return getHeadLookVector();
    }

    @Override
    public void setHeadLookVector(Vec3 dir) {
        this.sp_headLookVector = dir;
    }

    @Override
    public float getLeftLaserDistance() {
        return this.sp_leftLaserDist;
    }

    @Override
    public void setLeftLaserDistance(float dist) {
        if (this.sp_leftLaserDist != dist) {
            this.sp_leftLaserDist = dist;
            this.superpowers$sync();
        }
    }

    @Override
    public float getLerpedLeftLaserDistance(float tickDelta) {
        return net.minecraft.util.Mth.lerp(tickDelta, this.prevLeftLaserDist, this.currentLeftLaserDist);
    }

    @Override
    public float getRightLaserDistance() {
        return this.sp_rightLaserDist;
    }

    @Override
    public void setRightLaserDistance(float dist) {
        if (this.sp_rightLaserDist != dist) {
            this.sp_rightLaserDist = dist;
            this.superpowers$sync();
        }
    }

    @Override
    public float getLerpedRightLaserDistance(float tickDelta) {
        return net.minecraft.util.Mth.lerp(tickDelta, this.prevRightLaserDist, this.currentRightLaserDist);
    }

    @Override
    public Matrix4f getHeadMatrix() {
        return sp_headMatrix;
    }

    @Override
    public void setHeadMatrix(Matrix4f matrix) {
        this.sp_headMatrix = matrix;
    }

    @Override
    public float getLaserGap() {
        return this.sp_laserGap;
    }

    @Override
    public void setLaserGap(float gap) {
        if (this.sp_laserGap != gap) {
            this.sp_laserGap = gap;
            this.superpowers$sync();
        }
    }

    @Override
    public float getLaserOffset() {
        return this.sp_laserOffset;
    }

    @Override
    public void setLaserOffset(float offset) {
        if (this.sp_laserOffset != offset) {
            this.sp_laserOffset = offset;
            this.superpowers$sync();
        }
    }

    @Override
    public float getLaserDelay() {
        return this.sp_laserDelay;
    }

    @Override
    public void setLaserDelay(float delay) {
        if (this.sp_laserDelay != delay) {
            this.sp_laserDelay = delay;
            this.superpowers$sync();
        }
    }

    @Override
    public float getLaserDistance() {
        return this.sp_laserMaxDistance;
    }

    @Override
    public void setLaserDistance(float distance) {
        if (this.sp_laserMaxDistance != distance) {
            this.sp_laserMaxDistance = distance;
            this.superpowers$sync();
        }
    }

    @Override
    public float getLaserDamage() {
        return this.sp_laserDamage;
    }

    @Override
    public void setLaserDamage(float damage) {
        if (this.sp_laserDamage != damage) {
            this.sp_laserDamage = damage;
            this.superpowers$sync();
        }
    }

    @Override
    public String getLaserColor() {
        return this.sp_laserColor;
    }

    @Override
    public void setLaserColor(String hexColor) {
        if (!java.util.Objects.equals(this.sp_laserColor, hexColor)) {
            this.sp_laserColor = hexColor;
            this.superpowers$sync();
        }
    }

    @Override
    public int getChargeTicks() {
        return this.sp_chargeTicks;
    }

    @Override
    public void setChargeTicks(int ticks) {
        this.sp_chargeTicks = ticks;
    }

    // --- GrabPlayer interface implementation ---

    @Override
    public boolean canGrab() {
        if (this.superpowers$isSpectatorSafe()) return false;
        return this.sp_canGrab;
    }

    @Override
    public void setCanGrab(boolean canGrab) {
        if (this.sp_canGrab != canGrab) {
            this.sp_canGrab = canGrab;
            this.superpowers$sync();
        }
    }

    @Override
    public boolean isTryingToGrab() {
        if (this.superpowers$isSpectatorSafe()) return false;
        return this.sp_tryingToGrab;
    }

    @Override
    public void setTryingToGrab(boolean trying) {
        if (this.sp_tryingToGrab != trying) {
            this.sp_tryingToGrab = trying;
            this.superpowers$sync();
        }
    }

    @Override
    public LivingEntity getGrabbedEntity() {
        return this.sp_grabbedEntity;
    }

    @Override
    public void setGrabbedEntity(LivingEntity entity) {
        if (this.sp_grabbedEntity != entity) {
            this.sp_grabbedEntity = entity;
            this.superpowers$sync();
        }
    }

    @Override
    public void releaseEntity() {
        LivingEntity current = getGrabbedEntity();
        if (current instanceof net.minecraft.server.level.ServerPlayer serverTarget) {
            GrabbedPosSyncS2CPacket.send(serverTarget, Vec3.ZERO, Vec3.ZERO, false);
        }
        setTryingToGrab(false);
        setGrabbedEntity(null);
    }

    @Override
    public Vec3 getCalculatedHandPos() {
        return calculatedHandPos;
    }

    @Override
    public void setCalculatedHandPos(Vec3 pos) {
        this.calculatedHandPos = pos;
    }

    @Override
    public Vec3 getFirstPersonLocalHandPos() {
        return firstPersonLocalHandPos;
    }

    @Override
    public void setFirstPersonLocalHandPos(Vec3 pos) {
        this.firstPersonLocalHandPos = pos;
    }

    @Override
    public boolean isGrabbedBySomeone() {
        for (Player p : this.level().players()) {
            if (p instanceof GrabPlayer gp && gp.getGrabbedEntity() == (Object) this) {
                return true;
            }
        }
        return false;
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void superpowers$onAttack(net.minecraft.world.entity.Entity target, CallbackInfo ci) {
        if (this.isGrabbedBySomeone()) {
            ci.cancel();
        }
    }
}
