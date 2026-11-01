package com.lilsort.superpowers.flight;

public interface FlightPlayer {
    boolean isSuperFlightEnabled();
    void setSuperFlightEnabled(boolean enabled);
    float getFlightSpeedMultiplier();
    void setFlightSpeedMultiplier(float multiplier);
    
    /** Synced flying state visible to ALL clients (unlike vanilla getAbilities().flying) */
    boolean isActuallyFlying();
    void setActuallyFlying(boolean flying);
    
    /** Synced sprinting state visible to ALL clients */
    boolean isActuallySprinting();
    void setActuallySprinting(boolean sprinting);
}
