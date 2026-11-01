package com.lilsort.superpowers.sound;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public class FlightSounds {

    // FixedRangeEvent(16) = SoundEngine accepts play() at up to 16 blocks regardless of volume.
    // Actual attenuation curve is controlled by attenuation_distance in sounds.json.
    public static final SoundEvent FLIGHT_LOOP = SoundEvent.createFixedRangeEvent(
            new ResourceLocation("superpowers", "flight_loop"), 16.0F);
    public static final SoundEvent CAPE_LOOP = SoundEvent.createVariableRangeEvent(
            new ResourceLocation("superpowers", "cape_loop"));
    public static final SoundEvent FLIGHT_BOOST = SoundEvent.createFixedRangeEvent(
            new ResourceLocation("superpowers", "flight_boost"), 16.0F);
    public static final SoundEvent FLIGHT_BOOST_SUPER = SoundEvent.createFixedRangeEvent(
            new ResourceLocation("superpowers", "flight_boost_super"), 16.0F);

    public static void init() {
        Registry.register(BuiltInRegistries.SOUND_EVENT,
                new ResourceLocation("superpowers", "flight_loop"), FLIGHT_LOOP);
        Registry.register(BuiltInRegistries.SOUND_EVENT,
                new ResourceLocation("superpowers", "cape_loop"), CAPE_LOOP);
        Registry.register(BuiltInRegistries.SOUND_EVENT,
                new ResourceLocation("superpowers", "flight_boost"), FLIGHT_BOOST);
        Registry.register(BuiltInRegistries.SOUND_EVENT,
                new ResourceLocation("superpowers", "flight_boost_super"), FLIGHT_BOOST_SUPER);
    }
}
