package net.onelitefeather.cygnus.common.util;

import net.minestom.server.MinecraftServer;
import net.onelitefeather.cygnus.common.config.GameConfig;

/**
 * This class is responsible for calculating the additional movement speed that should be given to the player based on the number of players online.
 * The formula is: MAX_SPEED_BONUS * (1 - playerCount / 4)
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
public final class SpeedScalingCalculation {

    private static final double MAX_SPEED_BONUS = 0.01;

    /**
     * Calculates the additional movement speed that should be given to the player based on the number of players online.
     *
     * @param pageCount the number of pages for the game
     * @return the additional movement speed that should be given to the player
     */
    public static double getAdditionalSpeed(int pageCount) {
        if (pageCount > GameConfig.MIN_PAGE_COUNT) return 0.0;
        int playerCount = Math.min(MinecraftServer.getConnectionManager().getOnlinePlayerCount() - 1, 4);
        return MAX_SPEED_BONUS * (1.0 - (double) playerCount / 4);
    }

    private SpeedScalingCalculation() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}
