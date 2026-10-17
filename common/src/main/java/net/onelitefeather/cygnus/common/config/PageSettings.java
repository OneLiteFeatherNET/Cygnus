package net.onelitefeather.cygnus.common.config;

import static net.onelitefeather.cygnus.common.config.ConfigChecks.between;

/**
 * The page values a map sets for itself, read from its {@code pages.json}.
 * <p>
 * They depend on how big the map is and how far a player can see on it, so one number for every
 * map fits none of them well. A value the file leaves out takes its default.
 * </p>
 *
 * @param proximityRange      how far away a page can still be heard, in blocks
 * @param minActivePages      how many pages are in the world at once in a small round
 * @param ttlSeconds          how long a page stays in one spot before it moves on, in seconds
 * @param respawnDelaySeconds how long a found page stays hidden before it comes back, in seconds.
 *                            {@link GameConfig#PAGE_RESPAWN_DELAY_JITTER} is rolled on top of it
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.17.0
 */
public record PageSettings(int proximityRange, int minActivePages, int ttlSeconds, int respawnDelaySeconds) {

    /** Beyond this a single page would be audible across a good part of the map. */
    public static final int MAX_PROXIMITY_RANGE = 64;

    /** More pages at once and a round is over before the slender gets going. */
    public static final int MAX_MIN_ACTIVE_PAGES = 16;

    /** Shorter and a page moves on before anyone can walk up to it. */
    public static final int MIN_TTL_SECONDS = 10;

    /** Longer and a page might as well never move. */
    public static final int MAX_TTL_SECONDS = 300;

    /** Lower and the rolled jitter could make the delay negative. */
    public static final int MIN_RESPAWN_DELAY_SECONDS = GameConfig.PAGE_RESPAWN_DELAY_JITTER;

    /** Longer and a found page is gone for the rest of a short round. */
    public static final int MAX_RESPAWN_DELAY_SECONDS = 120;

    /** The values every map used before maps had settings of their own. */
    public static final PageSettings DEFAULT = new PageSettings(20, 4, 60, 15);

    /**
     * Checks every value against its bounds.
     *
     * @throws IllegalArgumentException if a value is out of range
     */
    public PageSettings {
        between("proximityRange", proximityRange, 1, MAX_PROXIMITY_RANGE);
        between("minActivePages", minActivePages, 1, MAX_MIN_ACTIVE_PAGES);
        between("ttlSeconds", ttlSeconds, MIN_TTL_SECONDS, MAX_TTL_SECONDS);
        between("respawnDelaySeconds", respawnDelaySeconds, MIN_RESPAWN_DELAY_SECONDS, MAX_RESPAWN_DELAY_SECONDS);
    }
}
