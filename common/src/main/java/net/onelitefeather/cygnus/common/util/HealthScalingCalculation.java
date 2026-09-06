package net.onelitefeather.cygnus.common.util;

import net.minestom.server.MinecraftServer;
import net.onelitefeather.cygnus.common.config.GameConfig;

import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

/**
 * This class is responsible for calculating the additional health that should be given to the player based on the number of pages they have collected.
 * The health scaling is based on the number of players online.
 * The formula is: MAX_HEALTH * (1 - playerCount / 4)
 * The raw result is randomly rounded to the nearest whole heart (2 HP steps) so players never end up with a half heart.
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
     * @return the additional health that should be given to the player, always a multiple of 2 (whole hearts)
     */
    public static float getAdditionalHealth(int pageCount) {
        if (pageCount > GameConfig.MIN_PAGE_COUNT) return 0.0f;
        int playerCount = Math.min(MinecraftServer.getConnectionManager().getOnlinePlayerCount() - 1, 4);
        float rawBonus = MAX_HEALTH * (1.0f - (float) playerCount / 4);
        return roundToWholeHeart(rawBonus, ThreadLocalRandom.current());
    }

    /**
     * Rounds a health bonus to the nearest whole heart (2 HP steps).
     * If the bonus lands exactly between two hearts, the direction is chosen randomly, weighted by how close it
     * is to either side, instead of always rounding the same way.
     *
     * @param bonus  the raw, possibly half-heart, health bonus
     * @param random the random source used to decide the rounding direction
     * @return the rounded bonus, always a multiple of 2
     */
    static float roundToWholeHeart(float bonus, RandomGenerator random) {
        float halfHearts = bonus / 2.0f;
        float flooredHalfHearts = (float) Math.floor(halfHearts);
        float remainder = halfHearts - flooredHalfHearts;
        if (remainder == 0.0f) return bonus;
        boolean roundUp = random.nextFloat() < remainder;
        return (roundUp ? flooredHalfHearts + 1 : flooredHalfHearts) * 2.0f;
    }

    private HealthScalingCalculation() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}
