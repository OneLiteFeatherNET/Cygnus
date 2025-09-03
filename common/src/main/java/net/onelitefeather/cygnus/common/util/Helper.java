package net.onelitefeather.cygnus.common.util;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.utils.Direction;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 **/
public final class Helper {

    public static final byte ZERO_ID = 0;
    public static final byte ONE_ID = 1;
    @Deprecated
    public static final byte SLENDER_ID = 0;
    @Deprecated
    public static final byte SURVIVOR_ID = 1;
    public static final long MIDNIGHT_TIME = 18000L;
    public static final long NEW_MOON_TIME = 114000L;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final double PAGE_VISIBLE_OFFSET = -0.030;
    private static final int MIN_OFFSET = 1;
    private static final int MAX_OFFSET = 30;
    private static final float MIN_SOUND_VALUE = .1f;
    private static final float MAX_SOUND_VALUE = 1.0f;

    private Helper() {
    }

    public static int calculateOffsetTime(int timeValue) {
        return timeValue + ((timeValue * SECURE_RANDOM.nextInt(MIN_OFFSET, MAX_OFFSET)) / 100);
    }

    public static float getRandomPitchValue() {
        return SECURE_RANDOM.nextFloat(MIN_SOUND_VALUE, MAX_SOUND_VALUE);
    }

    public static int getRandomInt(int maximumValue) {
        return ThreadLocalRandom.current().nextInt(1, maximumValue);
    }

    @Contract(pure = true)
    public static @NotNull Pos updatePosition(@NotNull Pos pos, @NotNull Direction direction) {
        return switch (direction) {
            case NORTH -> pos.add(0.5, .5, 1); // ?
            case SOUTH -> pos.add(0.5, .5, PAGE_VISIBLE_OFFSET); //Yes //z = 1
            case EAST -> pos.add(PAGE_VISIBLE_OFFSET, .5, 0.5).withView(-90, 0); // Yes // x = -1
            case WEST -> pos.add(1, .5, 0.5).withView(-90, 0); //?*/
            default -> throw new IllegalArgumentException("Found a direction that is not supported");
        };
    }
}
