package net.onelitefeather.cygnus.ambient;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScareSilenceTest {

    private static final UUID SURVIVOR = UUID.randomUUID();

    @Test
    @DisplayName("A jump scare silences for a few seconds")
    void silencesAfterAScare() {
        ScareSilence silence = new ScareSilence(_ -> 10_000L);

        assertEquals(SoundPreset.SCARE, silence.presetOf(SURVIVOR, 10_000L + ScareSilence.SILENCE_MILLIS - 1));
        assertEquals(SoundPreset.FREE, silence.presetOf(SURVIVOR, 10_000L + ScareSilence.SILENCE_MILLIS));
    }

    @Test
    @DisplayName("Without a jump scare nothing is silenced")
    void freeWithoutAScare() {
        assertEquals(SoundPreset.FREE, new ScareSilence(_ -> 0L).presetOf(SURVIVOR, 1_000L));
    }
}
