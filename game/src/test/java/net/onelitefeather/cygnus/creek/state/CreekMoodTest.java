package net.onelitefeather.cygnus.creek.state;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreekMoodTest {

    @Test
    @DisplayName("An untouched builder gives the normal creek")
    void untouchedBuilderIsNormal() {
        assertEquals(new CreekMood.Traits(1.0D, 1.0D, 1.0D, 1.0D, 0.0D, 1.0D, 1.0D, 0.25D, 0.5D, false),
                CreekMood.Traits.builder().build());
    }

    @Test
    @DisplayName("Each setter of the builder lands in its own value")
    void settersLandInTheirValue() {
        CreekMood.Traits traits = CreekMood.Traits.builder()
                .huntThreshold(1)
                .minStalk(2)
                .huntSpeed(3)
                .huntCooldown(4)
                .watchedHuntSpeed(5)
                .stalkDuration(6)
                .variantBreather(7)
                .spotMarkChance(8)
                .deadEndVanishChance(9)
                .silentStalk(true)
                .build();

        assertEquals(new CreekMood.Traits(1, 2, 3, 4, 5, 6, 7, 8, 9, true), traits);
    }

    @Test
    @DisplayName("Only the shadow stalks in silence")
    void onlyTheShadowStalksInSilence() {
        for (CreekMood mood : CreekMood.values()) {
            assertEquals(mood == CreekMood.SHADOW, mood.traits().silentStalk(), mood.name());
        }
    }
}
