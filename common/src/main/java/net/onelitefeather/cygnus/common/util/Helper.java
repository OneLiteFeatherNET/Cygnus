package net.onelitefeather.cygnus.common.util;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.utils.Direction;
import org.jetbrains.annotations.Contract;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility class containing various helpers for coordinates, random calculations,
 * and game-specific identifiers or timings.
 *
 * @author theEvilReaper
 * @version 1.0.2
 * @since 1.0.0
 **/
public final class Helper {

    /**
     * Constant byte representing the value zero.
     */
    public static final byte ZERO_ID = 0;

    /**
     * Constant byte representing the value one.
     */
    public static final byte ONE_ID = 1;

    /**
     * The ID representing the Slender team.
     * @deprecated Use {@link #ZERO_ID} or team configurations instead.
     */
    @Deprecated
    public static final byte SLENDER_ID = 0;

    /**
     * The ID representing the Survivor team.
     * @deprecated Use {@link #ONE_ID} or team configurations instead.
     */
    @Deprecated
    public static final byte SURVIVOR_ID = 1;

    /**
     * Minecraft time value representing midnight (18000 ticks).
     */
    public static final long MIDNIGHT_TIME = 18000L;

    /**
     * Minecraft time value representing a new moon time (114000 ticks).
     */
    public static final long NEW_MOON_TIME = 114000L;

    private static final double PAGE_VISIBLE_OFFSET = -0.030;
    private static final int MIN_OFFSET = 1;
    private static final int MAX_OFFSET = 30;
    private static final float MIN_SOUND_VALUE = 0.1f;
    private static final float MAX_SOUND_VALUE = 1.0f;

    private Helper() {
        // Prevent instantiation of utility class
    }

    /**
     * Calculates a randomized offset time by applying a random percentage increase
     * between {@value #MIN_OFFSET}% and {@value #MAX_OFFSET}% to the base time value.
     *
     * @param timeValue the base time value to calculate the offset for
     * @return the calculated offset time
     */
    public static int calculateOffsetTime(int timeValue) {
        return timeValue + ((timeValue * ThreadLocalRandom.current().nextInt(MIN_OFFSET, MAX_OFFSET)) / 100);
    }

    /**
     * Generates a random pitch value for playing sounds.
     * The value is guaranteed to be between {@value #MIN_SOUND_VALUE} (inclusive)
     * and {@value #MAX_SOUND_VALUE} (exclusive).
     *
     * @return a random float value representing the pitch
     */
    public static float getRandomPitchValue() {
        return ThreadLocalRandom.current().nextFloat(MIN_SOUND_VALUE, MAX_SOUND_VALUE);
    }

    /**
     * Returns a pseudorandom, uniformly distributed integer between {@code 0} (inclusive)
     * and the specified {@code maximumValue} (exclusive).
     *
     * @param maximumValue the exclusive upper bound for the random integer
     * @return a random integer between {@code 0} (inclusive) and {@code maximumValue} (exclusive)
     */
    public static int getRandomInt(int maximumValue) {
        return ThreadLocalRandom.current().nextInt(0, maximumValue);
    }

    /**
     * Adjusts the placement coordinates of a collectible page entity based on the
     * block face/direction it is attached to, ensuring it aligns correctly and remains visible.
     *
     * @param pos       the original position of the page block
     * @param direction the direction the block/wall is facing
     * @return the adjusted coordinates {@link Pos} ready for page spawning
     * @throws IllegalArgumentException if the provided direction is not supported
     */
    @Contract(pure = true)
    public static Pos updatePosition(Pos pos, Direction direction) {
        return switch (direction) {
            case NORTH -> pos.add(0.5, 0.5, 1.0);
            case SOUTH -> pos.add(0.5, 0.5, PAGE_VISIBLE_OFFSET);
            case EAST -> pos.add(PAGE_VISIBLE_OFFSET, 0.5, 0.5).withView(-90, 0);
            case WEST -> pos.add(1.0, 0.5, 0.5).withView(-90, 0);
            default -> throw new IllegalArgumentException("Found a direction that is not supported: " + direction);
        };
    }
}
