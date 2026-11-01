package com.lilsort.superpowers.laser;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Interface injected into Player via mixin.
 * Provides access to laser eye data for both server and client.
 */
public interface LaserPlayer {

    // Whether the admin has enabled laser ability for this player
    boolean isLaserEnabled();
    void setLaserEnabled(boolean enabled);

    // Whether the player is currently firing the laser (key held)
    boolean isFiringLaser();
    void setFiringLaser(boolean firing);

    // Eye positions in world space (set by client, used by server for raycast)
    Vec3 getLeftEyePos();
    void setLeftEyePos(Vec3 pos);
    Vec3 getSyncedLeftEyePos();

    Vec3 getRightEyePos();
    void setRightEyePos(Vec3 pos);
    Vec3 getSyncedRightEyePos();

    // Head look direction (normalized)
    Vec3 getHeadLookVector();
    void setHeadLookVector(Vec3 dir);
    Vec3 getSyncedHeadLookVector();

    // Hit distances (set by server, synced via DataTracker for client rendering)
    float getLeftLaserDistance();
    void setLeftLaserDistance(float dist);
    float getLerpedLeftLaserDistance(float tickDelta);

    float getRightLaserDistance();
    void setRightLaserDistance(float dist);
    float getLerpedRightLaserDistance(float tickDelta);

    // Head transformation matrix (client-only, for rendering)
    Matrix4f getHeadMatrix();
    void setHeadMatrix(Matrix4f matrix);

    // Laser visual offsets
    float getLaserGap();
    void setLaserGap(float gap);

    float getLaserOffset();
    void setLaserOffset(float offset);

    // Laser delay mechanics
    float getLaserDelay();
    void setLaserDelay(float delay);

    float getLaserDistance();
    void setLaserDistance(float distance);

    float getLaserDamage();
    void setLaserDamage(float damage);

    String getLaserColor();
    void setLaserColor(String hexColor);

    int getChargeTicks();
    void setChargeTicks(int ticks);
}
