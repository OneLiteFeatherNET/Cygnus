package net.onelitefeather.cygnus.common.util;

import net.minestom.server.MinecraftServer;
import net.onelitefeather.cygnus.common.config.GameConfig;

/**
 * This class is responsible for calculating the additional movement speed that should be given to the player based on the number of players online.
 * The formula is: MAX_SPEED_BONUS * (1 - survivors / (SMALL_ROUND_SURVIVORS + 1)), so only a small
 * round gets a bonus.
 *
 * @author theEvilReaper
 * @version 1.1.0
 * @since 1.0.0
 */
public final class SpeedScalingCalculation {

    private static final double MAX_SPEED_BONUS = 0.01;

    /** The first survivor count without a bonus, right after the small rounds. */
    private static final int NO_BONUS_SURVIVORS = GameConfig.SMALL_ROUND_SURVIVORS + 1;

    /**
     * Calculates the additional movement speed that should be given to the player based on the number of players online.
     *
     * Above {@link GameConfig#SMALL_ROUND_SURVIVORS} survivors there is no bonus.
     *
     * @return the additional movement speed that should be given to the player
     */
    public static double getAdditionalSpeed() {
        int survivors = Math.clamp(MinecraftServer.getConnectionManager().getOnlinePlayerCount() - 1, 0, NO_BONUS_SURVIVORS);
        return MAX_SPEED_BONUS * (1.0 - (double) survivors / NO_BONUS_SURVIVORS);
    }

    private SpeedScalingCalculation() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}
