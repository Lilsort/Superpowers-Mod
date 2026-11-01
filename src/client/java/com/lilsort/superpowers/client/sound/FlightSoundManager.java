package com.lilsort.superpowers.client.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FlightSoundManager {

    private static final Map<UUID, FlightLoopSoundInstance> FLIGHT_LOOPS = new HashMap<>();
    private static CapeLoopSoundInstance capeLoop = null;

    public static void tick(Player player, boolean isFlying) {
        if (!player.level().isClientSide) return;
        
        UUID uuid = player.getUUID();
        Minecraft mc = Minecraft.getInstance();
        boolean isLocalPlayer = (player == mc.player);

        if (isFlying) {
            // Check if player is within audible range (e.g., 24 blocks, gives a buffer over the 16 block attenuation)
            boolean inRange = isLocalPlayer || mc.player.distanceToSqr(player) <= 24.0 * 24.0;

            if (inRange) {
                // If we don't have the sound tracked, or it was culled/stopped by the Sound Engine, restart it!
                if (!FLIGHT_LOOPS.containsKey(uuid) || !mc.getSoundManager().isActive(FLIGHT_LOOPS.get(uuid))) {
                    FlightLoopSoundInstance flightLoop = new FlightLoopSoundInstance(player);
                    FLIGHT_LOOPS.put(uuid, flightLoop);
                    mc.getSoundManager().play(flightLoop);
                }
            } else {
                // Out of range — stop and remove the sound to save resources
                FlightLoopSoundInstance loop = FLIGHT_LOOPS.remove(uuid);
                if (loop != null) {
                    loop.stopSound();
                }
            }

            // Cape loop only for the local player
            if (isLocalPlayer) {
                if (capeLoop == null || !mc.getSoundManager().isActive(capeLoop)) {
                    capeLoop = new CapeLoopSoundInstance(player);
                    mc.getSoundManager().play(capeLoop);
                }
            }
        } else {
            // Player is NOT flying — force-stop all their sounds immediately
            FlightLoopSoundInstance loop = FLIGHT_LOOPS.remove(uuid);
            if (loop != null) {
                loop.stopSound();
            }
            if (isLocalPlayer && capeLoop != null) {
                capeLoop.stopSound();
                capeLoop = null;
            }
        }
    }
}
