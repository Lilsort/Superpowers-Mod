package com.lilsort.superpowers.client.sound;

import com.lilsort.superpowers.laser.LaserPlayer;
import com.lilsort.superpowers.sound.LaserSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public class LaserLoopSoundInstance extends AbstractTickableSoundInstance {

    private final Player player;
    private final LaserPlayer laserPlayer;
    private final boolean isLocalPlayer;
    private float targetVolume = 1.0F;
    private float targetPitch = 1.0F;

    public LaserLoopSoundInstance(Player player) {
        super(LaserSounds.LASER_LOOP, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.player = player;
        this.laserPlayer = (LaserPlayer) player;
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
        if (player.isRemoved()) {
            this.stop();
            return;
        }

        float delayTicks = this.laserPlayer.getLaserDelay() * 20.0F;
        int chargeTicks = this.laserPlayer.getChargeTicks();

        if (!this.laserPlayer.isFiringLaser()) {
            this.targetVolume = 0.0F;
        } else if ((float) chargeTicks < delayTicks && delayTicks > 0.0F) {
            float progress = (float) chargeTicks / delayTicks;
            this.targetVolume = 0.2F + 0.8F * progress;
            this.targetPitch = 0.5F + 0.5F * progress;
        } else {
            this.targetVolume = 1.0F;
            this.targetPitch = 1.0F;
        }

        this.volume = Mth.lerp(0.3F, this.volume, this.targetVolume);
        this.pitch = Mth.lerp(0.3F, this.pitch, this.targetPitch);

        if (this.targetVolume == 0.0F && this.volume <= 0.01F) {
            this.stop();
        } else if (!this.isLocalPlayer) {
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
