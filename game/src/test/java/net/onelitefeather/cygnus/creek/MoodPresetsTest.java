package net.onelitefeather.cygnus.creek;

import net.onelitefeather.cygnus.ambient.SoundPreset;
import net.onelitefeather.cygnus.creek.state.CreekMood;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MoodPresetsTest {

    private static final UUID SURVIVOR = UUID.randomUUID();

    @Test
    @DisplayName("The shadow turns a stalk into silence")
    void shadowHushesTheStalk() {
        MoodPresets presets = new MoodPresets((_, _) -> SoundPreset.STALK, () -> CreekMood.SHADOW);

        assertEquals(SoundPreset.HUSH, presets.presetOf(SURVIVOR, 0L));
    }

    @Test
    @DisplayName("Every other mood keeps the quiet stalk")
    void otherMoodsKeepTheStalk() {
        for (CreekMood mood : CreekMood.values()) {
            if (mood == CreekMood.SHADOW) continue;
            MoodPresets presets = new MoodPresets((_, _) -> SoundPreset.STALK, () -> mood);
            assertEquals(SoundPreset.STALK, presets.presetOf(SURVIVOR, 0L), mood.name());
        }
    }

    @Test
    @DisplayName("The shadow leaves everything but a stalk alone")
    void shadowOnlyTouchesTheStalk() {
        for (SoundPreset preset : SoundPreset.values()) {
            if (preset == SoundPreset.STALK) continue;
            MoodPresets presets = new MoodPresets((_, _) -> preset, () -> CreekMood.SHADOW);
            assertEquals(preset, presets.presetOf(SURVIVOR, 0L), preset.name());
        }
    }
}
