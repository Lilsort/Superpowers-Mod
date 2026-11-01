package com.lilsort.superpowers.grab;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class GrabManager {

    public static void tick(ServerPlayer player) {
        if (!(player instanceof GrabPlayer grabPlayer)) return;

        if (!grabPlayer.canGrab()) {
            grabPlayer.releaseEntity();
            return;
        }

        LivingEntity currentTarget = grabPlayer.getGrabbedEntity();

        if (grabPlayer.isTryingToGrab() && currentTarget == null) {
            // Raycast / Boxcast to find a target
            Vec3 eyePos = player.getEyePosition();
            Vec3 lookDir = player.getLookAngle();
            Vec3 grabCenter = eyePos.add(lookDir.scale(1.5));
            AABB grabBox = new AABB(
                grabCenter.x - 1.5, grabCenter.y - 1.5, grabCenter.z - 1.5,
                grabCenter.x + 1.5, grabCenter.y + 1.5, grabCenter.z + 1.5
            );

            List<Entity> entities = player.level().getEntities(player, grabBox);
            for (Entity entity : entities) {
                if (entity instanceof LivingEntity target && target.isAlive()) {
                    boolean isAlreadyGrabbed = false;
                    for (ServerPlayer otherPlayer : player.server.getPlayerList().getPlayers()) {
                        if (otherPlayer != player && otherPlayer instanceof GrabPlayer otherGrabPlayer) {
                            if (otherGrabPlayer.getGrabbedEntity() == target) {
                                isAlreadyGrabbed = true;
                                break;
                            }
                        }
                    }

                    if (!isAlreadyGrabbed) {
                        if (target instanceof GrabPlayer targetGrabPlayer) {
                            if (isHoldingRecursively(targetGrabPlayer, player)) {
                                continue; // Prevent circular grab loops
                            }
                            if (targetGrabPlayer.isTryingToGrab()) {
                                targetGrabPlayer.setTryingToGrab(false);
                            }
                        }

                        grabPlayer.setGrabbedEntity(target);
                        grabPlayer.setTryingToGrab(false);
                        target.fallDistance = 0.0f;
                        
                        if (target instanceof ServerPlayer grabbedPlayer) {
                            com.lilsort.superpowers.grab.GrabbedPosSyncS2CPacket.send(grabbedPlayer, player.position(), player.getDeltaMovement(), true);
                        }
                        break;
                    }
                }
            }
            // Do NOT set tryingToGrab to false here. 
            // If they are trying to grab but didn't find anything, keep the hand raised until they find something or press 'C' again!
        }

        if (currentTarget != null) {
            if (!currentTarget.isAlive() || currentTarget.isRemoved() || (currentTarget instanceof net.minecraft.world.entity.player.Player targetPlayer && targetPlayer.isSpectator())) {
                grabPlayer.releaseEntity();
                return;
            }

            // Move the target
            Vec3 eyePos = player.getEyePosition();
            Vec3 lookDir = player.getLookAngle();
            Vec3 handPos = eyePos.add(lookDir.scale(1.5));

            double neckOffset = currentTarget.getBbHeight() * 0.85;
            Vec3 targetPos = new Vec3(handPos.x, handPos.y - neckOffset, handPos.z);

            if (currentTarget instanceof ServerPlayer grabbedPlayer) {
                currentTarget.setPos(targetPos.x, targetPos.y, targetPos.z);
                currentTarget.setDeltaMovement(player.getDeltaMovement());
                currentTarget.hasImpulse = true;
                currentTarget.hurtMarked = true;
                currentTarget.fallDistance = 0.0F;
                com.lilsort.superpowers.grab.GrabbedPosSyncS2CPacket.send(grabbedPlayer, handPos, player.getDeltaMovement(), true);
            } else {
                // For mobs: use move() so they respect block collisions
                Vec3 movement = targetPos.subtract(currentTarget.position());
                currentTarget.move(MoverType.PLAYER, movement);
            }

            // Sync velocity so it doesn't fall downwards rapidly
            currentTarget.setDeltaMovement(player.getDeltaMovement());
            currentTarget.hasImpulse = true;
            currentTarget.fallDistance = 0.0F;
            // Prevent the entity from being pushed by anything while grabbed
            currentTarget.hurtMarked = true;

            // Sync facing (mobs only)
            if (!(currentTarget instanceof net.minecraft.world.entity.player.Player)) {
                float faceYaw = player.getYRot() + 180.0F;
                currentTarget.setYRot(faceYaw);
                currentTarget.setYHeadRot(faceYaw);
                currentTarget.yBodyRot = faceYaw;
            }
        }
    }

    private static boolean isHoldingRecursively(GrabPlayer holder, Entity target) {
        LivingEntity grabbed = holder.getGrabbedEntity();
        if (grabbed == target) {
            return true;
        }
        if (grabbed instanceof GrabPlayer subHolder) {
            return isHoldingRecursively(subHolder, target);
        }
        return false;
    }
}
