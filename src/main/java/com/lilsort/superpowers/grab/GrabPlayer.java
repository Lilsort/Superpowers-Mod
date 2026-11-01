package com.lilsort.superpowers.grab;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public interface GrabPlayer {
    boolean canGrab();
    void setCanGrab(boolean canGrab);

    boolean isTryingToGrab();
    void setTryingToGrab(boolean trying);

    LivingEntity getGrabbedEntity();
    void setGrabbedEntity(LivingEntity entity);

    void releaseEntity();

    Vec3 getCalculatedHandPos();
    void setCalculatedHandPos(Vec3 pos);

    Vec3 getFirstPersonLocalHandPos();
    void setFirstPersonLocalHandPos(Vec3 pos);

    default boolean isGrabbedBySomeone() {
        return false;
    }
}
