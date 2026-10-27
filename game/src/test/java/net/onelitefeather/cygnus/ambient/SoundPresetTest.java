package net.onelitefeather.cygnus.ambient;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SoundPresetTest {

    @Test
    @DisplayName("The more important preset wins, in either order")
    void moreImportantWins() {
        assertEquals(SoundPreset.HUNT, SoundPreset.STALK.over(SoundPreset.HUNT));
        assertEquals(SoundPreset.HUNT, SoundPreset.HUNT.over(SoundPreset.STALK));
        assertEquals(SoundPreset.SCARE, SoundPreset.SCARE.over(SoundPreset.FREE));
        assertEquals(SoundPreset.HUSH, SoundPreset.HUSH.over(SoundPreset.STALK));
        assertEquals(SoundPreset.SCARE, SoundPreset.HUSH.over(SoundPreset.SCARE));
    }

    @Test
    @DisplayName("A stalk keeps the fear below the eerie sounds, the free preset leaves it")
    void stalkCapsTheFear() {
        assertEquals(0.3D, SoundPreset.STALK.cap(1.0D));
        assertEquals(0.1D, SoundPreset.STALK.cap(0.1D));
        assertEquals(1.0D, SoundPreset.FREE.cap(1.0D));
    }

    @Test
    @DisplayName("Two combined sources give the more important preset")
    void combinedSourcesPickTheMoreImportant() {
        SoundPresetSource stalk = (_, _) -> SoundPreset.STALK;
        SoundPresetSource scare = (_, _) -> SoundPreset.SCARE;

        assertEquals(SoundPreset.SCARE, stalk.with(scare).presetOf(UUID.randomUUID(), 0L));
        assertEquals(SoundPreset.SCARE, scare.with(stalk).presetOf(UUID.randomUUID(), 0L));
        assertEquals(SoundPreset.STALK, stalk.with(SoundPresetSource.NONE).presetOf(UUID.randomUUID(), 0L));
    }
}
