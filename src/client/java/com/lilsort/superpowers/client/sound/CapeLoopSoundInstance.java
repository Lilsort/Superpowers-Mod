package com.lilsort.superpowers.client.sound;

import com.lilsort.superpowers.flight.FlightPlayer;
import com.lilsort.superpowers.sound.FlightSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * Cape flutter sound — plays only for the local player (first person).
 * Always uses relative=true so there is no 3D panning.
 */
public class CapeLoopSoundInstance extends AbstractTickableSoundInstance {

    private final Player player;
    private final FlightPlayer flightPlayer;
    private float targetVolume = 0.0F;
    private float targetPitch = 1.0F;

    public CapeLoopSoundInstance(Player player) {
        super(FlightSounds.CAPE_LOOP, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.player = player;
        this.flightPlayer = (FlightPlayer) player;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.001F;
        this.pitch = 1.0F;
        this.relative = true;
        this.x = 0.0D;
        this.y = 0.0D;
        this.z = 0.0D;
    }

    @Override
    public void tick() {
        // Stop only when player is removed or flight is fully disabled
        if (player.isRemoved() || !flightPlayer.isSuperFlightEnabled() || !player.getAbilities().flying) {
            this.stop();
            return;
        }

        // Cape flutters more violently at higher speeds, drops to 0 when hovering
        double speedSq = player.getDeltaMovement().lengthSqr();
        float speed = (float) Math.sqrt(speedSq);

        // Limit volume if not sprinting
        float sprintMultiplier = player.isSprinting() ? 1.0F : 0.6F;

        this.targetVolume = Mth.clamp((speed - 0.05F) * 2.0F, 0.0F, 1.0F) * sprintMultiplier;
        this.targetPitch = Mth.clamp(0.9F + speed * 0.3F, 0.9F, 1.2F);

        // Smooth lerp — fade out faster than fade in
        float volumeLerpRate = this.targetVolume < this.volume ? 0.7F : 0.3F;
        this.volume = Mth.lerp(volumeLerpRate, this.volume, this.targetVolume);
        this.pitch = Mth.lerp(0.1F, this.pitch, this.targetPitch);
    }

    /** Public wrapper for the protected stop() method. */
    public void stopSound() {
        this.stop();
    }
}
