package net.onelitefeather.cygnus.ambient;

/**
 * What the ambient sounds of a survivor may do right now, so they do not drown out a sound that
 * matters more.
 * <p>
 * The presets are ordered from the least to the most important. When several hold for a survivor
 * at once, the last one wins, see {@link #over(SoundPreset)}.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.21.0
 */
public enum SoundPreset {

    /** Nothing in particular: the fear alone paces the ambient sounds. */
    FREE(false, 1.0D, 1.0F),

    /** A creek stalks the survivor: the cave stays rare and quiet, and nothing eerie joins it. */
    STALK(false, 0.3D, 0.5F),

    /** A creek stalks the survivor in a mood that wants silence: nothing plays. */
    HUSH(true, 0.0D, 0.0F),

    /** A phantom just jumped at the survivor: a moment of silence lets it sink in. */
    SCARE(true, 0.0D, 0.0F),

    /** A creek hunts the survivor: only the creek is heard. */
    HUNT(true, 0.0D, 0.0F);

    private final boolean silent;
    private final double maxFear;
    private final float volume;

    SoundPreset(boolean silent, double maxFear, float volume) {
        this.silent = silent;
        this.maxFear = maxFear;
        this.volume = volume;
    }

    /**
     * Tells whether the ambient sounds pause.
     *
     * @return {@code true} if none play
     */
    public boolean silent() {
        return this.silent;
    }

    /**
     * Caps the fear the ambient sounds are paced by. Less fear means longer gaps and no eerie sounds.
     *
     * @param fear how scared the survivor is, between {@code 0} and {@code 1}
     * @return the fear the ambient sounds go by
     */
    public double cap(double fear) {
        return Math.min(fear, this.maxFear);
    }

    /**
     * Tells how loud the ambient sounds are.
     *
     * @return the volume, {@code 1} for full
     */
    public float volume() {
        return this.volume;
    }

    /**
     * Picks the more important of two presets.
     *
     * @param other the other preset
     * @return this one if it matters at least as much, the other otherwise
     */
    public SoundPreset over(SoundPreset other) {
        return this.ordinal() >= other.ordinal() ? this : other;
    }
}
