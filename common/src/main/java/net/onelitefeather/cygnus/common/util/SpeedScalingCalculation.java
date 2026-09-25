package net.onelitefeather.cygnus.common.util;

import net.minestom.server.MinecraftServer;

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
     * From four survivors on there is no bonus.
     *
     * @return the additional movement speed that should be given to the player
     */
    public static double getAdditionalSpeed() {
        int playerCount = Math.clamp(MinecraftServer.getConnectionManager().getOnlinePlayerCount() - 1, 0, 4);
        return MAX_SPEED_BONUS * (1.0 - (double) playerCount / 4);
    }

    private SpeedScalingCalculation() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}
