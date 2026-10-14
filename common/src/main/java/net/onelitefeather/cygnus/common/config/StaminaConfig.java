package net.onelitefeather.cygnus.common.config;

import static net.onelitefeather.cygnus.common.config.ConfigChecks.between;
import static net.onelitefeather.cygnus.common.config.ConfigChecks.notNegative;
import static net.onelitefeather.cygnus.common.config.ConfigChecks.positive;
import static net.onelitefeather.cygnus.common.config.ConfigChecks.share;

/**
 * Settings for the sprint of the survivors and the appearing of the slender.
 * <p>
 * Tuned so a survivor who is hunted gets a real chance to get away: the sprint comes back long
 * before the bar is full again, and the slender cannot chain one appearance onto the next.
 * </p>
 *
 * @param sprintResumeShare              how full a survivor's bar must be to sprint again, above 0 and at most 1
 * @param regenPerSecond                 how many points of the survivor's bar come back per second, out of 20
 * @param slenderReappearCooldownSeconds how long the slender stays hidden at least once he vanished, 0 to turn it off
 * @param slenderDamageRange             how far around him the draining slender hurts survivors, in blocks, 1 to 16
 * @author theEvilReaper
 * @version 1.1.0
 * @since 2.16.0
 */
public record StaminaConfig(
        double sprintResumeShare,
        double regenPerSecond,
        int slenderReappearCooldownSeconds,
        int slenderDamageRange
) {

    /** The reach the slender gets when nothing says otherwise. Widened from 3 for the playtest. */
    public static final int DEFAULT_SLENDER_DAMAGE_RANGE = 4;

    /** Beyond this the slender would hurt survivors from across a room. */
    public static final int MAX_SLENDER_DAMAGE_RANGE = 16;

    /**
     * The default settings, from the playtest in which hunted survivors had no chance to escape.
     */
    public static final StaminaConfig DEFAULT = new StaminaConfig(0.3D, 1.25D, 5, DEFAULT_SLENDER_DAMAGE_RANGE);

    /**
     * Creates the settings with the default damage range.
     *
     * @param sprintResumeShare              how full a survivor's bar must be to sprint again
     * @param regenPerSecond                 how many points of the survivor's bar come back per second
     * @param slenderReappearCooldownSeconds how long the slender stays hidden at least once he vanished
     * @since 2.15.0
     */
    public StaminaConfig(double sprintResumeShare, double regenPerSecond, int slenderReappearCooldownSeconds) {
        this(sprintResumeShare, regenPerSecond, slenderReappearCooldownSeconds, DEFAULT_SLENDER_DAMAGE_RANGE);
    }

    /**
     * Checks the values.
     *
     * @throws IllegalArgumentException if a value is out of range
     */
    public StaminaConfig {
        share("sprintResumeShare", sprintResumeShare);
        positive("regenPerSecond", regenPerSecond);
        notNegative("slenderReappearCooldownSeconds", slenderReappearCooldownSeconds);
        between("slenderDamageRange", slenderDamageRange, 1, MAX_SLENDER_DAMAGE_RANGE);
    }

    /**
     * Reads the sprint settings. The section carries the {@code stamina.} prefix.
     *
     * @param section the {@code stamina.} part of the config
     * @return the sprint settings
     */
    public static StaminaConfig read(ConfigSection section) {
        return section.orDefault("stamina", () -> new StaminaConfig(
                section.getDouble("sprintResumeShare", DEFAULT.sprintResumeShare()),
                section.getDouble("regenPerSecond", DEFAULT.regenPerSecond()),
                section.getInt("slenderReappearCooldownSeconds", DEFAULT.slenderReappearCooldownSeconds()),
                section.getInt("slenderDamageRange", DEFAULT.slenderDamageRange())
        ), DEFAULT);
    }
}
