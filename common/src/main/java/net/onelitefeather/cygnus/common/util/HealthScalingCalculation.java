package net.onelitefeather.cygnus.common.util;

import net.minestom.server.MinecraftServer;
import net.onelitefeather.cygnus.common.config.GameConfig;

/**
 * This class is responsible for calculating the additional health that should be given to the player based on the number of pages they have collected.
 * The health scaling is based on the number of players online.
 * The formula is: MAX_HEALTH * (1 - playerCount / 4)
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
public final class HealthScalingCalculation {

    private static final float MAX_HEALTH = 20.0f;

    /**
     * Calculates the additional health that should be given to the player based on the number of pages they have collected.
     *
     * @param pageCount the number of pages for the game
     * @return the additional health that should be given to the player
     */
    public static float getAdditionalHealth(int pageCount) {
        if (pageCount > GameConfig.MIN_PAGE_COUNT) return 0.0f;
        int playerCount = Math.min(MinecraftServer.getConnectionManager().getOnlinePlayerCount() - 1, 4);
        return MAX_HEALTH * (1.0f - (float) playerCount / 4);
    }

    private HealthScalingCalculation() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}
