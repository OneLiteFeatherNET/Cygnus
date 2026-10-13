package net.onelitefeather.cygnus.common.config;

import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

/**
 * Settings for rounds with only a few survivors.
 * <p>
 * A round with at most {@link GameConfig#SMALL_ROUND_SURVIVORS} survivors needs only eight pages,
 * and its survivors get extra health. The keys carry the
 * {@code eightPages.} prefix, for example {@code eightPages.bonusOneSurvivor}.
 * </p>
 * <p>
 * A bonus may end on a half heart. The round then gets it rounded to a whole heart, up or down by
 * chance, see {@link #wholeHeartBonusFor(int)}. On average the survivors still get the configured
 * bonus.
 * </p>
 *
 * @param bonusOneSurvivor    extra health points for a lone survivor, a multiple of 5
 * @param bonusTwoSurvivors   extra health points for each of two survivors, a multiple of 5
 * @param bonusThreeSurvivors extra health points for each of three survivors, a multiple of 5
 * @author theEvilReaper
 * @version 1.3.0
 * @since 2.17.0
 */
public record EightPagesConfig(int bonusOneSurvivor, int bonusTwoSurvivors, int bonusThreeSurvivors) {

    /**
     * Health points per Minecraft health point. A bonus has to be a multiple of it, so the hearts
     * always come out in half heart steps.
     */
    private static final int POINTS_PER_HEALTH = 5;

    /** Health points per heart. */
    private static final int POINTS_PER_HEART = POINTS_PER_HEALTH * 2;

    /** The default settings from the design. */
    public static final EightPagesConfig DEFAULT = new EightPagesConfig(50, 25, 0);

    /**
     * Checks the values.
     *
     * @throws IllegalArgumentException if a value is out of range
     */
    public EightPagesConfig {
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
        // One case for every survivor count up to GameConfig.SMALL_ROUND_SURVIVORS
        return switch (survivors) {
            case 1 -> this.bonusOneSurvivor;
            case 2 -> this.bonusTwoSurvivors;
            case 3 -> this.bonusThreeSurvivors;
            default -> 0;
        };
    }

    /**
     * Returns the extra health points for a round with the given number of survivors, as whole hearts.
     * <p>
     * A bonus that ends on a half heart is rounded to the heart above or below, each with the same
     * chance. Called once per round, so every survivor of the round gets the same bonus.
     * </p>
     *
     * @param survivors the survivors at the start of the round
     * @return the bonus, a multiple of {@value #POINTS_PER_HEART}
     */
    public int wholeHeartBonusFor(int survivors) {
        return wholeHeartBonusFor(survivors, ThreadLocalRandom.current());
    }

    /**
     * Returns the bonus as whole hearts with the given random source, see {@link #wholeHeartBonusFor(int)}.
     *
     * @param survivors the survivors at the start of the round
     * @param random    the source that decides between rounding up and down
     * @return the bonus, a multiple of {@value #POINTS_PER_HEART}
     */
    int wholeHeartBonusFor(int survivors, RandomGenerator random) {
        int bonus = bonusFor(survivors);
        int halfHeart = bonus % POINTS_PER_HEART;
        if (halfHeart == 0) return bonus;
        return random.nextBoolean() ? bonus - halfHeart + POINTS_PER_HEART : bonus - halfHeart;
    }

    private static void bonus(String name, int value) {
        if (value < 0 || value % POINTS_PER_HEALTH != 0) {
            throw new IllegalArgumentException(name + " (" + value + ") must be a multiple of "
                    + POINTS_PER_HEALTH + " and not negative");
        }
    }
}
