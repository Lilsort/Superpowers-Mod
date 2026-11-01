package com.lilsort.superpowers.client.sound;

import com.lilsort.superpowers.laser.LaserPlayer;
import com.lilsort.superpowers.sound.LaserSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.sounds.SoundSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LaserSoundManager {

    private static final Map<UUID, Boolean> chargingStates = new HashMap<>();
    private static final Map<UUID, Boolean> firingStates = new HashMap<>();
    private static final Map<UUID, LaserLoopSoundInstance> LASER_LOOPS = new HashMap<>();

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            chargingStates.clear();
            firingStates.clear();
            LASER_LOOPS.clear();
            return;
        }

        for (AbstractClientPlayer player : mc.level.players()) {
            if (!(player instanceof LaserPlayer laserPlayer)) continue;

            UUID uuid = player.getUUID();
            boolean isCharging = laserPlayer.isFiringLaser();
            float delayTicks = laserPlayer.getLaserDelay() * 20.0f;
            boolean isActuallyFiring = isCharging && laserPlayer.getChargeTicks() >= delayTicks;
            boolean wasCharging = chargingStates.getOrDefault(uuid, false);
            boolean wasActuallyFiring = firingStates.getOrDefault(uuid, false);

            boolean isLocalPlayer = (player == mc.player);

            if (isCharging) {
                // Ensure the loop is playing if they are in range, even if it was culled earlier
                boolean inRange = isLocalPlayer || mc.player.distanceToSqr(player) <= 80.0 * 80.0;
                
                if (inRange) {
                    if (!LASER_LOOPS.containsKey(uuid) || !mc.getSoundManager().isActive(LASER_LOOPS.get(uuid))) {
                        LaserLoopSoundInstance loop = new LaserLoopSoundInstance(player);
                        LASER_LOOPS.put(uuid, loop);
                        mc.getSoundManager().play(loop);
                    }
                } else {
                    LaserLoopSoundInstance loop = LASER_LOOPS.remove(uuid);
                    if (loop != null) loop.stopSound();
                }
            } else {
                LaserLoopSoundInstance loop = LASER_LOOPS.remove(uuid);
                if (loop != null) loop.stopSound();
            }

            if (isActuallyFiring && !wasActuallyFiring) {
                mc.getSoundManager().play(new LaserEntrySoundInstance(player));
            } else if (!isActuallyFiring && wasActuallyFiring) {
                mc.level.playLocalSound(
                        player.getX(), player.getY() + player.getEyeHeight(), player.getZ(),
                        LaserSounds.LASER_QUIT, SoundSource.PLAYERS,
                        1.0f, 1.0f, false
                );
            }

            chargingStates.put(uuid, isCharging);
            firingStates.put(uuid, isActuallyFiring);
        }
    }
}
