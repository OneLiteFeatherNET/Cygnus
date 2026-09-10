package net.onelitefeather.cygnus.common.page;

import net.onelitefeather.cygnus.common.config.GameConfig;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility class for calculating page brightness levels and initial lighting.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class PageLightUtil {

    public static final int DEFAULT_MIN_INITIAL_BLOCK_LIGHT = 8;
    public static final int DEFAULT_MAX_INITIAL_BLOCK_LIGHT = 12;
    public static final int DEFAULT_MIN_BLOCK_LIGHT = 1;
    public static final int PAGE_SKY_LIGHT = 0;

    private PageLightUtil() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * Generates a random initial block light level using default bounds
     * ([{@link #DEFAULT_MIN_INITIAL_BLOCK_LIGHT}, {@link #DEFAULT_MAX_INITIAL_BLOCK_LIGHT}]).
     *
     * @return the generated block light level
     */
    public static int randomInitialBlockLight() {
        return randomInitialBlockLight(DEFAULT_MIN_INITIAL_BLOCK_LIGHT, DEFAULT_MAX_INITIAL_BLOCK_LIGHT);
    }

    /**
     * Generates a random initial block light level within the specified bounds (inclusive).
     *
     * @param min the minimum block light level
     * @param max the maximum block light level
     * @return the generated block light level
     */
    public static int randomInitialBlockLight(int min, int max) {
        int actualMin = Math.min(min, max);
        int actualMax = Math.max(min, max);

        if (actualMin == actualMax) {
            return actualMin;
        }

        return ThreadLocalRandom.current().nextInt(actualMin, actualMax + 1);
    }

    /**
     * Calculates the block light level based on remaining TTL using {@value #DEFAULT_MIN_BLOCK_LIGHT}
     * as the lower bound.
     *
     * @param initialBlockLight the initial block light the page started with
     * @param currentTickTime   the elapsed tick seconds
     * @param ttlTime           the total TTL in seconds
     * @return the calculated block light level
     */
    public static int calculateBlockLight(int initialBlockLight, int currentTickTime, int ttlTime) {
        return calculateBlockLight(initialBlockLight, DEFAULT_MIN_BLOCK_LIGHT, currentTickTime, ttlTime);
    }

    /**
     * Calculates the block light level linearly interpolating between initial light and minimum light
     * according to the remaining fraction of TTL.
     *
     * @param initialBlockLight the initial block light the page started with
     * @param minBlockLight     the minimum block light level when TTL expires
     * @param currentTickTime   the elapsed tick seconds
     * @param ttlTime           the total TTL in seconds
     * @return the calculated block light level
     */
    public static int calculateBlockLight(int initialBlockLight, int minBlockLight, int currentTickTime, int ttlTime) {
        if (ttlTime <= 0) {
            return minBlockLight;
        }

        if (currentTickTime <= 0) {
            return initialBlockLight;
        }

        if (currentTickTime >= ttlTime) {
            return minBlockLight;
        }

        double remainingRatio = 1.0 - ((double) currentTickTime / ttlTime);
        int calculated = (int) Math.round(minBlockLight + remainingRatio * (initialBlockLight - minBlockLight));

        int lowerBound = Math.min(minBlockLight, initialBlockLight);
        int upperBound = Math.max(minBlockLight, initialBlockLight);

        return Math.clamp(calculated, lowerBound, upperBound);
    }
}
