package com.lilsort.superpowers.laser;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Utility class for performing laser raycasts.
 */
public class LaserRaycastUtil {

    /**
     * Result of a laser raycast, containing the hit distance and any hit entity or block.
     */
    public record LaserHitResult(float distance, Entity hitEntity, Vec3 hitPos, BlockHitResult blockHit) {}

    /**
     * Performs a combined block + entity raycast from start along direction for maxDistance.
     * Spectator players are ignored. The closest hit (block or entity) wins.
     *
     * @param level      The server level
     * @param shooter    The entity shooting the laser (excluded from hits)
     * @param start      Start position (eye position)
     * @param direction  Normalized look direction
     * @param maxDistance Maximum laser range in blocks
     * @return LaserHitResult with distance and optional hit entity
     */
    public static LaserHitResult performRaycast(Level level, Entity shooter, Vec3 start, Vec3 direction, double maxDistance) {
        Vec3 end = start.add(direction.scale(maxDistance));

        // 1. Block raycast (including fluids)
        BlockHitResult blockHit = level.clip(new ClipContext(
                start, end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.ANY,
                shooter
        ));

        // Effective end is either the block hit or max distance
        Vec3 effectiveEnd = (blockHit.getType() != HitResult.Type.MISS)
                ? blockHit.getLocation()
                : end;
        double effectiveDistSq = start.distanceToSqr(effectiveEnd);

        // 2. Entity raycast (only up to block hit)
        AABB searchBox = new AABB(start, effectiveEnd).inflate(1.0);
        List<Entity> entities = level.getEntities(shooter, searchBox, entity -> {
            if (!entity.isAlive()) return false;
            if (!entity.isPickable()) return false;
            // Skip spectator players entirely
            if (entity instanceof ServerPlayer sp) {
                return sp.gameMode.getGameModeForPlayer() != GameType.SPECTATOR;
            }
            return true;
        });

        Entity closestEntity = null;
        Vec3 closestHitPos = null;
        double closestEntityDistSq = effectiveDistSq;

        for (Entity entity : entities) {
            AABB box = entity.getBoundingBox().inflate(entity.getPickRadius());
            Optional<Vec3> hit = box.clip(start, effectiveEnd);
            if (hit.isPresent()) {
                double distSq = start.distanceToSqr(hit.get());
                if (distSq < closestEntityDistSq) {
                    closestEntityDistSq = distSq;
                    closestEntity = entity;
                    closestHitPos = hit.get();
                }
            }
        }

        // 3. Determine result
        if (closestEntity != null) {
            float dist = (float) Math.sqrt(closestEntityDistSq);
            return new LaserHitResult(dist, closestEntity, closestHitPos, null);
        } else if (blockHit.getType() != HitResult.Type.MISS) {
            float dist = (float) start.distanceTo(blockHit.getLocation());
            return new LaserHitResult(dist, null, blockHit.getLocation(), blockHit);
        } else {
            return new LaserHitResult((float) maxDistance, null, end, null);
        }
    }
}
