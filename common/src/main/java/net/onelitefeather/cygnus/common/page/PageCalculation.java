package net.onelitefeather.cygnus.common.page;

import net.minestom.server.MinecraftServer;
import net.onelitefeather.cygnus.common.config.GameConfig;

/**
 * This class calculates the amount of pages that should be allocated for the dynamic page system.
 * The amount of pages is calculated based on the amount of players online.
 * If the amount of players online is less than 4, the minimum amount of pages will be returned.
 * Otherwise, the amount of pages will be calculated based on the amount of players online.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
public final class PageCalculation {

    private static final int PLAYER_SIZE_FOR_DYNAMIC_PAGE_ALLOCATION = 4;
    private static final int PAGE_COUNT_MULTIPLIER = 2;

    /**
     * Calculates the amount of pages that should be allocated for the dynamic page system.
     *
     * @return the amount of pages that should be allocated
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