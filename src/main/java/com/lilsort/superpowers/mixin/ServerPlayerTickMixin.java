package com.lilsort.superpowers.mixin;

import com.lilsort.superpowers.laser.LaserPlayer;
import com.lilsort.superpowers.laser.LaserRaycastUtil;
import com.lilsort.superpowers.grab.GrabManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;

/**
 * Server-side tick logic for laser damage.
 * On each tick, if the player is firing the laser:
 *   - Performs raycasts for both eyes
 *   - Kills entities on hit (instant kill)
 *   - Non-survival players block the beam but don't die
 *   - Spectator players are ignored (filtered in raycast)
 *   - Updates hit distances for client rendering via DataTracker
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerTickMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void superpowers$onTick(CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;

        GrabManager.tick(self);

        if (!(self instanceof LaserPlayer laserPlayer)) return;

        if (!laserPlayer.isLaserEnabled() || !laserPlayer.isFiringLaser()) {
            return;
        }

        float delayTicks = laserPlayer.getLaserDelay() * 20.0f;
        if (laserPlayer.getChargeTicks() < delayTicks) {
            // Still charging, do not fire raycasts yet
            return;
        }

        Vec3 lookDir = laserPlayer.getHeadLookVector();
        if (lookDir.equals(Vec3.ZERO)) return;
        lookDir = lookDir.normalize();

        double maxDistance = laserPlayer.getLaserDistance();

        // Left eye raycast
        Vec3 leftEye = laserPlayer.getLeftEyePos();
        if (!leftEye.equals(Vec3.ZERO)) {
            float leftDist = processLaserHit(self, leftEye, lookDir, maxDistance);
            laserPlayer.setLeftLaserDistance(leftDist);
        }

        // Right eye raycast
        Vec3 rightEye = laserPlayer.getRightEyePos();
        if (!rightEye.equals(Vec3.ZERO)) {
            float rightDist = processLaserHit(self, rightEye, lookDir, maxDistance);
            laserPlayer.setRightLaserDistance(rightDist);
        }
    }

    /**
     * Performs a raycast from the given eye position and handles entity kills.
     * Returns the hit distance for rendering.
     */
    private static float processLaserHit(ServerPlayer shooter, Vec3 eyePos, Vec3 lookDir, double maxDistance) {
        LaserRaycastUtil.LaserHitResult hit = LaserRaycastUtil.performRaycast(
                shooter.serverLevel(), shooter, eyePos, lookDir, maxDistance);

        ServerLevel serverLevel = shooter.serverLevel();
        Vec3 hitPos = hit.hitPos();
        float damage = ((LaserPlayer) shooter).getLaserDamage();

        if (hit.hitEntity() != null) {
            Entity entity = hit.hitEntity();

            // If damage is set to massive values (e.g. 1000+), we use outOfWorld to guarantee 
            // bypassing all armor, resistances, and even creative mode if it's not a player.
            net.minecraft.world.damagesource.DamageSource source = (damage >= 1000.0f)
                    ? shooter.damageSources().fellOutOfWorld()
                    : shooter.damageSources().playerAttack(shooter);

            entity.hurt(source, damage);
            entity.setSecondsOnFire(2);
            Vec3 push = lookDir.scale(0.2);
            entity.push(push.x, push.y, push.z);
            entity.hasImpulse = true;

            // Spawn entity hit particles (Blood/damage effect and Flame from original mod)
            serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR, hitPos.x, hitPos.y, hitPos.z, 3, 0.1, 0.1, 0.1, 0.05);
            serverLevel.sendParticles(ParticleTypes.FLAME, hitPos.x, hitPos.y, hitPos.z, 3, 0.1, 0.1, 0.1, 0.05);

        } else if (hit.blockHit() != null) {
            net.minecraft.world.phys.BlockHitResult blockHit = hit.blockHit();
            net.minecraft.core.BlockPos pos = blockHit.getBlockPos();
            net.minecraft.world.level.block.state.BlockState state = serverLevel.getBlockState(pos);

            if (!state.getFluidState().isEmpty() && state.getFluidState().is(net.minecraft.tags.FluidTags.WATER)) {
                // Water evaporation effect (visual and sound only, no destruction)
                serverLevel.sendParticles(ParticleTypes.CLOUD, hitPos.x, hitPos.y, hitPos.z, 5, 0.3, 0.3, 0.3, 0.05);
                serverLevel.playSound(null, pos, net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH, net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 1.5F + serverLevel.random.nextFloat() * 0.5F);
            } else {
                // Block hit (since we don't break blocks, we just spawn smoke and lava sparks as heat effect)
                // Decreased particles to 1 of each to optimize and avoid visual clutter
                serverLevel.sendParticles(ParticleTypes.SMOKE, hitPos.x, hitPos.y, hitPos.z, 1, 0.05, 0.05, 0.05, 0.01);
                serverLevel.sendParticles(ParticleTypes.LAVA, hitPos.x, hitPos.y, hitPos.z, 1, 0.05, 0.05, 0.05, 0.03);
            }
        }

        return hit.distance();
    }
}
