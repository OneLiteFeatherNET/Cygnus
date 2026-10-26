package net.onelitefeather.cygnus.ambient;

import java.util.UUID;
import java.util.function.ToLongFunction;

/**
 * Keeps the ambient sounds quiet for a few seconds after a phantom jumped at a survivor. The
 * turnaround scare is no jump in fear, so the fear alone would not know of it.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.21.0
 */
public final class ScareSilence implements SoundPresetSource {

    /** How long the silence after a jump scare lasts, in milliseconds. */
    static final long SILENCE_MILLIS = 4000L;

    private final ToLongFunction<UUID> lastScare;

    /**
     * Sets up the silence.
     *
     * @param lastScare tells when a survivor was last jump scared, in milliseconds, {@code 0} if never
     */
    public ScareSilence(ToLongFunction<UUID> lastScare) {
        this.lastScare = lastScare;
    }

    @Override
    public SoundPreset presetOf(UUID survivor, long now) {
        long scared = this.lastScare.applyAsLong(survivor);
        return scared > 0L && now - scared < SILENCE_MILLIS ? SoundPreset.SCARE : SoundPreset.FREE;
    }
}
