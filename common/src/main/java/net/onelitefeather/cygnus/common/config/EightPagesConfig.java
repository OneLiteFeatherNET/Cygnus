package net.onelitefeather.cygnus.common.config;

/**
 * Settings for rounds with only a few survivors.
 * <p>
 * Such a round needs only eight pages, and its survivors get extra health. The keys carry the
 * {@code eightPages.} prefix, for example {@code eightPages.bonusOneSurvivor}.
 * </p>
 *
 * @param maxSurvivors        a round with at most this many survivors at its start needs only eight pages
 * @param bonusOneSurvivor    extra health points for a lone survivor, a multiple of 5
 * @param bonusTwoSurvivors   extra health points for each of two survivors, a multiple of 5
 * @param bonusThreeSurvivors extra health points for each of three survivors, a multiple of 5
 * @author theEvilReaper
 * @version 1.1.0
 * @since 2.17.0
 */
public record EightPagesConfig(int maxSurvivors, int bonusOneSurvivor, int bonusTwoSurvivors, int bonusThreeSurvivors) {

    /**
     * Health points per Minecraft health point. A bonus has to be a multiple of it, so the hearts
     * always come out in half heart steps.
     */
    private static final int POINTS_PER_HEALTH = 5;

    /** The default settings from the design. */
    public static final EightPagesConfig DEFAULT = new EightPagesConfig(3, 50, 25, 0);

    /**
     * Checks the values.
     *
     * @throws IllegalArgumentException if a value is out of range
     */
    public EightPagesConfig {
        if (maxSurvivors < 0) {
            throw new IllegalArgumentException("maxSurvivors (" + maxSurvivors + ") must not be negative");
        }
        bonus("bonusOneSurvivor", bonusOneSurvivor);
        bonus("bonusTwoSurvivors", bonusTwoSurvivors);
        bonus("bonusThreeSurvivors", bonusThreeSurvivors);
    }

    /**
     * Reads the settings. The section carries the {@code eightPages.} prefix. Invalid values fall
     * back to the defaults.
     *
     * @param section the {@code eightPages.} part of the config
     * @return the settings
     */
    public static EightPagesConfig read(ConfigSection section) {
        EightPagesConfig defaults = DEFAULT;
        return section.orDefault("eight pages", () -> new EightPagesConfig(
                section.getInt("maxSurvivors", defaults.maxSurvivors()),
                section.getInt("bonusOneSurvivor", defaults.bonusOneSurvivor()),
                section.getInt("bonusTwoSurvivors", defaults.bonusTwoSurvivors()),
                section.getInt("bonusThreeSurvivors", defaults.bonusThreeSurvivors())
        ), defaults);
    }

    /**
     * Returns the extra health points for a round with the given number of survivors.
     *
     * @param survivors the survivors at the start of the round
     * @return the bonus, {@code 0} for every count without a configured one
     */
    public int bonusFor(int survivors) {
        return switch (survivors) {
            case 1 -> this.bonusOneSurvivor;
            case 2 -> this.bonusTwoSurvivors;
            case 3 -> this.bonusThreeSurvivors;
            default -> 0;
        };
    }

    private static void bonus(String name, int value) {
        if (value < 0 || value % POINTS_PER_HEALTH != 0) {
            throw new IllegalArgumentException(name + " (" + value + ") must be a multiple of "
                    + POINTS_PER_HEALTH + " and not negative");
        }
    }
}
