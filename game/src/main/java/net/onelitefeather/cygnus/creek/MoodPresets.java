package net.onelitefeather.cygnus.creek;

import net.onelitefeather.cygnus.ambient.SoundPreset;
import net.onelitefeather.cygnus.ambient.SoundPresetSource;
import net.onelitefeather.cygnus.creek.state.CreekMood;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Lets the creek's mood color the sound presets: a mood that stalks in silence turns a stalk into
 * {@link SoundPreset#HUSH}. Everything else is left as it is.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.21.0
 */
final class MoodPresets implements SoundPresetSource {

    private final SoundPresetSource base;
    private final Supplier<CreekMood> mood;

    /**
     * Wraps a source of presets.
     *
     * @param base the presets before the mood
     * @param mood the creek's mood right now
     */
    MoodPresets(SoundPresetSource base, Supplier<CreekMood> mood) {
        this.base = base;
        this.mood = mood;
    }

    @Override
    public SoundPreset presetOf(UUID survivor, long now) {
        SoundPreset preset = this.base.presetOf(survivor, now);
        if (preset == SoundPreset.STALK && this.mood.get().traits().silentStalk()) return SoundPreset.HUSH;
        return preset;
    }
}
