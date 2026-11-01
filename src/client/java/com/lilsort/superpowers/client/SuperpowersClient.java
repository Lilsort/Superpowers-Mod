package com.lilsort.superpowers.client;

import com.lilsort.superpowers.client.grab.GrabKeybind;
import com.lilsort.superpowers.client.input.LaserKeybindHandler;
import com.lilsort.superpowers.client.render.LaserRenderManager;
import com.lilsort.superpowers.client.sound.LaserSoundManager;
import com.lilsort.superpowers.grab.GrabbedPosSyncS2CPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class SuperpowersClient implements ClientModInitializer {
    public static Vec3 exactHandPos = null;
    public static Vec3 exactVelocity = Vec3.ZERO;
    public static Vec3 prevHandPos = null;
    public static Vec3 currentHandPos = null;
    public static boolean iAmBeingGrabbed = false;

	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.

		// Register laser keybind tick handler
		LaserKeybindHandler.register();
		GrabKeybind.register();

		// Register laser beam renderer
		LaserRenderManager.register();

		// Register sound manager tick
		ClientTickEvents.END_CLIENT_TICK.register(mc -> LaserSoundManager.tick());

        ClientPlayNetworking.registerGlobalReceiver(com.lilsort.superpowers.network.SuperpowerStateSyncS2CPacket.ID, (client, handler, buf, responseSender) -> {
            int playerId = buf.readInt();
            
            boolean superFlightEnabled = buf.readBoolean();
            boolean actuallyFlying = buf.readBoolean();
            boolean actuallySprinting = buf.readBoolean();
            float flightSpeed = buf.readFloat();

            boolean laserEnabled = buf.readBoolean();
            boolean firingLaser = buf.readBoolean();
            float laserGap = buf.readFloat();
            float laserOffset = buf.readFloat();
            float laserDelay = buf.readFloat();
            float laserDistance = buf.readFloat();
            float laserDamage = buf.readFloat();
            String laserColor = buf.readUtf();
            float leftLaserDist = buf.readFloat();
            float rightLaserDist = buf.readFloat();

            boolean canGrab = buf.readBoolean();
            boolean tryingToGrab = buf.readBoolean();
            int grabbedEntityId = buf.readInt();

            client.execute(() -> {
                if (client.level != null) {
                    net.minecraft.world.entity.Entity entity = client.level.getEntity(playerId);
                    if (entity instanceof com.lilsort.superpowers.flight.FlightPlayer fp && 
                        entity instanceof com.lilsort.superpowers.laser.LaserPlayer lp && 
                        entity instanceof com.lilsort.superpowers.grab.GrabPlayer gp) {
                        
                        fp.setSuperFlightEnabled(superFlightEnabled);
                        fp.setActuallyFlying(actuallyFlying);
                        fp.setActuallySprinting(actuallySprinting);
                        fp.setFlightSpeedMultiplier(flightSpeed);

                        lp.setLaserEnabled(laserEnabled);
                        lp.setFiringLaser(firingLaser);
                        lp.setLaserGap(laserGap);
                        lp.setLaserOffset(laserOffset);
                        lp.setLaserDelay(laserDelay);
                        lp.setLaserDistance(laserDistance);
                        lp.setLaserDamage(laserDamage);
                        lp.setLaserColor(laserColor);
                        lp.setLeftLaserDistance(leftLaserDist);
                        lp.setRightLaserDistance(rightLaserDist);

                        gp.setCanGrab(canGrab);
                        gp.setTryingToGrab(tryingToGrab);
                        gp.setGrabbedEntity(grabbedEntityId == -1 ? null : (net.minecraft.world.entity.LivingEntity) client.level.getEntity(grabbedEntityId));
                    }
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(GrabbedPosSyncS2CPacket.ID, (client, handler, buf, responseSender) -> {
            boolean isGrabbed = buf.readBoolean();
            if (isGrabbed) {
                double x = buf.readDouble();
                double y = buf.readDouble();
                double z = buf.readDouble();
                double vx = buf.readDouble();
                double vy = buf.readDouble();
                double vz = buf.readDouble();
                client.execute(() -> {
                    iAmBeingGrabbed = true;
                    exactHandPos = new Vec3(x, y, z);
                    exactVelocity = new Vec3(vx, vy, vz);
                });
            } else {
                client.execute(() -> {
                    iAmBeingGrabbed = false;
                    exactHandPos = null;
                    currentHandPos = null;
                    prevHandPos = null;
                });
            }
        });

        WorldRenderEvents.START.register(context -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                if (iAmBeingGrabbed && prevHandPos != null && currentHandPos != null) {
                    float tickDelta = context.tickDelta();
                    double lerpX = Mth.lerp(tickDelta, prevHandPos.x(), currentHandPos.x());
                    double lerpY = Mth.lerp(tickDelta, prevHandPos.y(), currentHandPos.y());
                    double lerpZ = Mth.lerp(tickDelta, prevHandPos.z(), currentHandPos.z());
                    
                    double neckOffset = client.player.getBbHeight() * 0.85;
                    double finalY = lerpY - neckOffset;
                    
                    client.player.setPosRaw(lerpX, finalY, lerpZ);
                    client.player.setDeltaMovement(exactVelocity);
                    client.player.fallDistance = 0.0F;
                    
                    client.player.xo = lerpX;
                    client.player.yo = finalY;
                    client.player.zo = lerpZ;
                    client.player.xOld = lerpX;
                    client.player.yOld = finalY;
                    client.player.zOld = lerpZ;
                }
            }
        });

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (iAmBeingGrabbed && exactHandPos != null && client.player != null) {
                if (client.player.isCreative() && client.player.input.shiftKeyDown) {
                    ClientPlayNetworking.send(com.lilsort.superpowers.grab.GrabBreakC2SPacket.ID, net.fabricmc.fabric.api.networking.v1.PacketByteBufs.empty());
                }

                if (currentHandPos == null) {
                    currentHandPos = exactHandPos;
                    prevHandPos = exactHandPos;
                } else {
                    prevHandPos = currentHandPos;
                    currentHandPos = exactHandPos;
                }
                client.player.setDeltaMovement(Vec3.ZERO);
                client.player.fallDistance = 0.0F;
            } else {
                currentHandPos = null;
                prevHandPos = null;
            }
        });
	}
}