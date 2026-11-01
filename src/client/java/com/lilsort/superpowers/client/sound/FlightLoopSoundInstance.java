package com.lilsort.superpowers.client.sound;

import com.lilsort.superpowers.flight.FlightPlayer;
import com.lilsort.superpowers.sound.FlightSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public class FlightLoopSoundInstance extends AbstractTickableSoundInstance {

    private final Player player;
    private final FlightPlayer flightPlayer;
    private final boolean isLocalPlayer;
    private float targetVolume = 0.0F;
    private float targetPitch = 1.0F;

    public FlightLoopSoundInstance(Player player) {
        super(FlightSounds.FLIGHT_LOOP, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.player = player;
        this.flightPlayer = (FlightPlayer) player;
        this.isLocalPlayer = (player == Minecraft.getInstance().player);
        this.looping = true;
        this.delay = 0;
        this.volume = 0.001F;
        this.pitch = 1.0F;

        if (this.isLocalPlayer) {
            this.relative = true;
            this.x = 0.0D;
            this.y = 0.0D;
            this.z = 0.0D;
        } else {
            this.relative = false;
            this.x = player.getX();
            this.y = player.getY() + player.getEyeHeight();
            this.z = player.getZ();
        }
    }

    @Override
    public void tick() {
        // Use the correct flying/sprinting checks depending on local vs remote player
        boolean isFlying = this.isLocalPlayer
                ? player.getAbilities().flying
                : flightPlayer.isActuallyFlying();
        boolean isSprinting = this.isLocalPlayer
                ? player.isSprinting()
                : flightPlayer.isActuallySprinting();

        // Stop only when player is removed or flight is fully disabled
        if (player.isRemoved() || !flightPlayer.isSuperFlightEnabled() || !isFlying) {
            this.stop();
            return;
        }

        // Calculate target volume based on speed
        double speedSq = player.getDeltaMovement().lengthSqr();
        float speed = (float) Math.sqrt(speedSq);

        // Dip volume during sharp turns
        float deltaYaw = Mth.wrapDegrees(player.getYRot() - player.yRotO);
        float turnPenalty = Mth.clamp(1.0F - (Math.abs(deltaYaw) / 15.0F), 0.2F, 1.0F);

        // Limit volume if not sprinting
        float sprintMultiplier = isSprinting ? 1.0F : 0.5F;

        this.targetVolume = Mth.clamp((speed - 0.05F) * 1.5F, 0.0F, 1.0F) * turnPenalty * sprintMultiplier;
        this.targetPitch = Mth.clamp(0.8F + speed * 0.2F, 0.8F, 1.5F);

        // Smooth lerp — fade out faster than fade in
        float volumeLerpRate = this.targetVolume < this.volume ? 0.7F : 0.3F;
        this.volume = Mth.lerp(volumeLerpRate, this.volume, this.targetVolume);
        this.pitch = Mth.lerp(0.2F, this.pitch, this.targetPitch);

        // Update position for other players
        if (!this.isLocalPlayer) {
            this.x = player.getX();
            this.y = player.getY() + player.getEyeHeight();
            this.z = player.getZ();
        }
    }

    /** Public wrapper for the protected stop() method. */
    public void stopSound() {
        this.stop();
    }
}
