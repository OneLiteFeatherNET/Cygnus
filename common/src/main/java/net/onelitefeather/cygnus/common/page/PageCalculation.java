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

    private PageCalculation() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}