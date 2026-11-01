package com.lilsort.superpowers.client.flight;

public interface FlightClientPlayer {
    float getSupermanProgress(float partialTicks);
    float getHoverBobTime(float partialTicks);
    float getFlightAnimWeight(float partialTicks);
    float getFlightMoveYaw(float partialTicks);
    float getFlightMoveProgress(float partialTicks);
    float getFlightCameraRoll(float partialTicks);
    float getFlightBodyRoll(float partialTicks);
}
