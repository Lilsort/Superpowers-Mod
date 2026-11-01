package com.lilsort.superpowers.client.sound;

import com.lilsort.superpowers.laser.LaserPlayer;
import com.lilsort.superpowers.sound.LaserSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

public class LaserEntrySoundInstance extends AbstractTickableSoundInstance {

    private final Player player;
    private final LaserPlayer laserPlayer;
    private final boolean isLocalPlayer;

    public LaserEntrySoundInstance(Player player) {
        super(LaserSounds.LASER_ENTRY, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.player = player;
        this.laserPlayer = (LaserPlayer) player;
        this.isLocalPlayer = (player == Minecraft.getInstance().player);
        this.looping = false;
        this.volume = 1.0F;
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
        float delayTicks = this.laserPlayer.getLaserDelay() * 20.0F;
        boolean isActuallyFiring = this.laserPlayer.isFiringLaser() && (float) this.laserPlayer.getChargeTicks() >= delayTicks;
        if (!this.player.isRemoved() && isActuallyFiring) {
            if (!this.isLocalPlayer) {
                this.x = player.getX();
                this.y = player.getY() + player.getEyeHeight();
                this.z = player.getZ();
            }
        } else {
            this.stop();
        }
    }
}
