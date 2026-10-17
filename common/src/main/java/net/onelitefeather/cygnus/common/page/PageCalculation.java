package net.onelitefeather.cygnus.common.page;

import net.minestom.server.MinecraftServer;
import net.onelitefeather.cygnus.common.config.GameConfig;

import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

/**
 * Utility class for calculating how many pages a round needs and how many are active at once.
 *
 * @author theEvilReaper
 * @version 1.3.0
 * @since 1.0.0
 */
public final class PageCalculation {

    /** The pages of an Eight Pages round before its small offset, and the base the normal round builds on. */
    private static final int EIGHT_PAGES = 8;

    /** The largest amount an Eight Pages round may add on top of its {@value #EIGHT_PAGES} pages. */
    private static final int EIGHT_PAGES_OFFSET_MAX = 2;

    /** Pages each survivor above the Eight Pages boundary adds. */
    private static final int PAGES_PER_EXTRA_SURVIVOR = 4;

    /**
     * The largest amount a normal round may add on top of its base amount, so the total can't be
     * worked out in advance from the player count alone.
     * <p>
     * Not exposed through {@link GameConfig}: unlike the other page settings, this one is
     * deliberately not something a server operator should be able to turn off or tune.
     * </p>
     * <p>
     * It only applies to normal rounds. An Eight Pages round has its own smaller offset.
     * </p>
     */
    private static final int PAGE_COUNT_JITTER_MAX = 4;

    private static final int PLAYER_SIZE_FOR_ACTIVE_PAGE_SCALING = 8;

    /**
     * How many extra survivors above {@value #PLAYER_SIZE_FOR_ACTIVE_PAGE_SCALING} it takes for
     * {@link #calculateActivePageAmount(int)} to add one more page.
     * <p>
     * Playtesting a steeper step (one active page per extra survivor) reached 16 concurrently
     * active pages, which felt like clutter; this keeps the top of the range below 10.
     * </p>
     */
    private static final int ACTIVE_PAGE_COUNT_STEP_SIZE = 3;

    /**
     * Calculates the number of pages a round needs.
     * <p>
     * An Eight Pages round has {@value #EIGHT_PAGES} pages plus 0 to {@value #EIGHT_PAGES_OFFSET_MAX}.
     * A normal round builds on them: {@value #PAGES_PER_EXTRA_SURVIVOR} more for every survivor
     * above {@link GameConfig#SMALL_ROUND_SURVIVORS}, plus 0 to {@value #PAGE_COUNT_JITTER_MAX}.
     * Anchored to the boundary, the step from one mode to the other never jumps.
     * </p>
     *
     * @param survivors the survivors at the start of the round
     * @return the number of pages
     */
    public static int calculatePageAmount(int survivors) {
        return calculatePageAmount(survivors, ThreadLocalRandom.current());
    }

    /**
     * Calculates the number of pages with the given random source, see {@link #calculatePageAmount(int)}.
     *
     * @param survivors the survivors at the start of the round
     * @param random    the source of the random part
     * @return the number of pages
     */
    static int calculatePageAmount(int survivors, RandomGenerator random) {
        if (survivors <= GameConfig.SMALL_ROUND_SURVIVORS) {
            return EIGHT_PAGES + random.nextInt(EIGHT_PAGES_OFFSET_MAX + 1);
        }
        return EIGHT_PAGES + (survivors - GameConfig.SMALL_ROUND_SURVIVORS) * PAGES_PER_EXTRA_SURVIVOR
                + random.nextInt(PAGE_COUNT_JITTER_MAX + 1);
    }

    /**
     * Calculates how many pages should be concurrently active (spawned in the world at once).
     * <p>
     * This stays below {@link #calculatePageAmount(int)} on purpose: it governs how many pages exist
     * in the world at the same time, not the total pool a round draws from. If the number of online
     * players (excluding one) is less than {@value #PLAYER_SIZE_FOR_ACTIVE_PAGE_SCALING},
     * {@code minActivePages} is returned. Otherwise, one page is added for every
     * {@value #ACTIVE_PAGE_COUNT_STEP_SIZE} survivors past that threshold, so the count stays flat
     * for most of the range and only rises near the top of a full lobby.
     *
     * @param minActivePages how many pages a small round keeps active, from the map's page settings
     * @return the number of pages to keep active at once, at least {@code minActivePages}
     */
    public static int calculateActivePageAmount(int minActivePages) {
        int currentPlayers = MinecraftServer.getConnectionManager().getOnlinePlayers().size();
        int survivors = currentPlayers - 1;

        if (survivors < PLAYER_SIZE_FOR_ACTIVE_PAGE_SCALING) {
            return minActivePages;
        }
        return minActivePages + (survivors - PLAYER_SIZE_FOR_ACTIVE_PAGE_SCALING) / ACTIVE_PAGE_COUNT_STEP_SIZE;
    }

    private PageCalculation() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}