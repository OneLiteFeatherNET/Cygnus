package net.onelitefeather.cygnus.ambient;

import java.util.UUID;

/**
 * Tells which {@link SoundPreset} holds for a survivor.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.21.0
 */
@FunctionalInterface
public interface SoundPresetSource {

    /** Leaves the ambient sounds to the fear alone. */
    SoundPresetSource NONE = (_, _) -> SoundPreset.FREE;

    /**
     * Tells which preset holds for a survivor.
     *
     * @param survivor the survivor
     * @param now      the current time in milliseconds
     * @return the preset
     */
    SoundPreset presetOf(UUID survivor, long now);

    /**
     * Combines this source with another, so the more important preset of the two holds.
     *
     * @param other the other source
     * @return the combined source
     */
    default SoundPresetSource with(SoundPresetSource other) {
        return (survivor, now) -> this.presetOf(survivor, now).over(other.presetOf(survivor, now));
    }
}
