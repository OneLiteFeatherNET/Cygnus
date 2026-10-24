package net.onelitefeather.cygnus.creek;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreekRespiteTest {

    private static final UUID SURVIVOR = UUID.randomUUID();

    @Test
    @DisplayName("A survivor is spared up to the given time, not at it")
    void sparesUntilTheGivenTime() {
        CreekRespite respite = new CreekRespite();
        respite.spare(SURVIVOR, 1000L);

        assertTrue(respite.spares(SURVIVOR, 999L));
        assertFalse(respite.spares(SURVIVOR, 1000L));
        assertFalse(respite.spares(UUID.randomUUID(), 0L));
    }

    @Test
    @DisplayName("Clearing forgets every spared survivor")
    void clearForgetsEverySurvivor() {
        CreekRespite respite = new CreekRespite();
        respite.spare(SURVIVOR, 1000L);

        respite.clear();

        assertFalse(respite.spares(SURVIVOR, 0L));
    }
}
