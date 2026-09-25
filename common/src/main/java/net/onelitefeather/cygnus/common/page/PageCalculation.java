package net.onelitefeather.cygnus.common.page;

import net.minestom.server.MinecraftServer;
import net.onelitefeather.cygnus.common.config.GameConfig;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility class for calculating the number of pages allocated for the dynamic page system.
 * The page count is based on the number of current online players.
 *
 * @author theEvilReaper
 * @version 1.1.0
 * @since 1.0.0
 */

public final class PageCalculation {

    private static final int PLAYER_SIZE_FOR_DYNAMIC_PAGE_ALLOCATION = 4;
    private static final int PAGE_COUNT_MULTIPLIER = 2;

    /**
     * The largest amount {@link #calculatePageAmount()} may add on top of the base amount, so the
     * total can't be worked out in advance from the player count alone.
     * <p>
     * Not exposed through {@link GameConfig}: unlike the other page settings, this one is
     * deliberately not something a server operator should be able to turn off or tune.
     * </p>
     */
    private static final int PAGE_COUNT_JITTER_MAX = 2;

    private static final int PLAYER_SIZE_FOR_ACTIVE_PAGE_SCALING = 8;

    /**
     * How many extra survivors above {@value #PLAYER_SIZE_FOR_ACTIVE_PAGE_SCALING} it takes for
     * {@link #calculateActivePageAmount()} to add one more page.
     * <p>
     * Playtesting a steeper step (one active page per extra survivor) reached 16 concurrently
     * active pages, which felt like clutter; this keeps the top of the range below 10.
     * </p>
     */
    private static final int ACTIVE_PAGE_COUNT_STEP_SIZE = 3;

    /**
     * Calculates the number of pages to allocate for the dynamic page system.
     * <p>
     * If the number of online players (excluding one) is less than {@value #PLAYER_SIZE_FOR_DYNAMIC_PAGE_ALLOCATION},
     * the base amount is {@link GameConfig#MIN_PAGE_COUNT}. Otherwise, it is the adjusted player
     * count multiplied by {@value #PAGE_COUNT_MULTIPLIER}. A random amount between 0 and
     * {@value #PAGE_COUNT_JITTER_MAX} is then added on top, re-rolled every call, so the total
     * can't be derived from the player count alone.
     *
     * @return the number of pages to allocate, at least {@link GameConfig#MIN_PAGE_COUNT}
     */
    public static int calculatePageAmount() {
        int currentPlayers = MinecraftServer.getConnectionManager().getOnlinePlayers().size();

        int baseAmount = currentPlayers - 1 < PLAYER_SIZE_FOR_DYNAMIC_PAGE_ALLOCATION
                ? GameConfig.MIN_PAGE_COUNT
                : (currentPlayers - 1) * PAGE_COUNT_MULTIPLIER;

        return baseAmount + ThreadLocalRandom.current().nextInt(PAGE_COUNT_JITTER_MAX + 1);
    }

    /**
     * Calculates how many pages should be concurrently active (spawned in the world at once).
     * <p>
     * This stays below {@link #calculatePageAmount()} on purpose: it governs how many pages exist
     * in the world at the same time, not the total pool a round draws from. If the number of online
     * players (excluding one) is less than {@value #PLAYER_SIZE_FOR_ACTIVE_PAGE_SCALING},
     * {@link GameConfig#MIN_ACTIVE_PAGE_COUNT} is returned. Otherwise, one page is added for every
     * {@value #ACTIVE_PAGE_COUNT_STEP_SIZE} survivors past that threshold, so the count stays flat
     * for most of the range and only rises near the top of a full lobby.
     *
     * @return the number of pages to keep active at once, at least {@link GameConfig#MIN_ACTIVE_PAGE_COUNT}
     */
    public static int calculateActivePageAmount() {
        int currentPlayers = MinecraftServer.getConnectionManager().getOnlinePlayers().size();
        int survivors = currentPlayers - 1;

        if (survivors < PLAYER_SIZE_FOR_ACTIVE_PAGE_SCALING) {
            return GameConfig.MIN_ACTIVE_PAGE_COUNT;
        }
        return GameConfig.MIN_ACTIVE_PAGE_COUNT + (survivors - PLAYER_SIZE_FOR_ACTIVE_PAGE_SCALING) / ACTIVE_PAGE_COUNT_STEP_SIZE;
    }

    private PageCalculation() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}