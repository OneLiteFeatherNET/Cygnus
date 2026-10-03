package net.onelitefeather.cygnus.common.config;

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
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.16.0
 */
public record StaminaConfig(
        double sprintResumeShare,
        double regenPerSecond,
        int slenderReappearCooldownSeconds
) {

    /**
     * The default settings, from the playtest in which hunted survivors had no chance to escape.
     */
    public static final StaminaConfig DEFAULT = new StaminaConfig(0.3D, 1.25D, 5);

    /**
     * Checks the values.
     *
     * @throws IllegalArgumentException if a value is out of range
     */
    public StaminaConfig {
        if (sprintResumeShare <= 0.0D || sprintResumeShare > 1.0D) {
            throw new IllegalArgumentException("sprintResumeShare (" + sprintResumeShare + ") must be above 0.0 and at most 1.0");
        }
        if (regenPerSecond <= 0.0D) {
            throw new IllegalArgumentException("regenPerSecond (" + regenPerSecond + ") must be above 0.0");
        }
        if (slenderReappearCooldownSeconds < 0) {
            throw new IllegalArgumentException("slenderReappearCooldownSeconds (" + slenderReappearCooldownSeconds + ") must not be negative");
        }
    }
}
