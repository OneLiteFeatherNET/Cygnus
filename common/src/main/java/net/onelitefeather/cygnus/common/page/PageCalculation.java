package net.onelitefeather.cygnus.common.page;

import net.minestom.server.MinecraftServer;
import net.onelitefeather.cygnus.common.config.GameConfig;

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

    private static final int PLAYER_SIZE_FOR_ACTIVE_PAGE_SCALING = 8;
    private static final int ACTIVE_PAGE_COUNT_MULTIPLIER = 1;

    /**
     * Calculates the number of pages to allocate for the dynamic page system.
     * <p>
     * If the number of online players (excluding one) is less than {@value #PLAYER_SIZE_FOR_DYNAMIC_PAGE_ALLOCATION},
     * {@link GameConfig#MIN_PAGE_COUNT} is returned. Otherwise, the page count is determined by
     * multiplying the adjusted player count by {@value #PAGE_COUNT_MULTIPLIER}.
     *
     * @return the number of pages to allocate, at least {@link GameConfig#MIN_PAGE_COUNT}
     */
    public static int calculatePageAmount() {
        int currentPlayers = MinecraftServer.getConnectionManager().getOnlinePlayers().size();

        if (currentPlayers - 1 < PLAYER_SIZE_FOR_DYNAMIC_PAGE_ALLOCATION) {
            return GameConfig.MIN_PAGE_COUNT;
        } else {
            return (currentPlayers - 1) * PAGE_COUNT_MULTIPLIER;
        }
    }

    /**
     * Calculates how many pages should be concurrently active (spawned in the world at once).
     * <p>
     * This stays below {@link #calculatePageAmount()} on purpose: it governs how many pages exist
     * in the world at the same time, not the total pool a round draws from. If the number of online
     * players (excluding one) is less than {@value #PLAYER_SIZE_FOR_ACTIVE_PAGE_SCALING},
     * {@link GameConfig#MIN_ACTIVE_PAGE_COUNT} is returned. Otherwise, the count is determined by
     * multiplying the adjusted player count by {@value #ACTIVE_PAGE_COUNT_MULTIPLIER}.
     *
     * @return the number of pages to keep active at once, at least {@link GameConfig#MIN_ACTIVE_PAGE_COUNT}
     */
    public static int calculateActivePageAmount() {
        int currentPlayers = MinecraftServer.getConnectionManager().getOnlinePlayers().size();

        if (currentPlayers - 1 < PLAYER_SIZE_FOR_ACTIVE_PAGE_SCALING) {
            return GameConfig.MIN_ACTIVE_PAGE_COUNT;
        } else {
            return (currentPlayers - 1) * ACTIVE_PAGE_COUNT_MULTIPLIER;
        }
    }

    private PageCalculation() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}