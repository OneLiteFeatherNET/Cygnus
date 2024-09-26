package net.onelitefeather.cygnus.common.util;

import net.minestom.server.utils.Direction;
import org.jetbrains.annotations.NotNull;

/**
 * The {@link DirectionFaceHelper} class provides utility methods to handle the direction of a face.
 * It's used to parse a face from a string and to check if a face is valid.
 *
 * @version 1.0.0
 * @since 1.0.0
 */
public final class DirectionFaceHelper {

    /**
     * Parses a face from a string. If the string is empty, the method returns {@link Direction#NORTH}.
     *
     * @param face the face to parse
     * @return the parsed face
     */
    public static @NotNull Direction parseDirection(@NotNull String face) {
        if (face.trim().isEmpty()) return Direction.NORTH;

        Direction direction = null;

        var horizontalValues = Direction.HORIZONTAL;

        for (int i = 0; i < horizontalValues.length && direction == null; i++) {
            if (!horizontalValues[i].name().equals(face)) continue;
            direction = horizontalValues[i];
        }
        return direction == null ? Direction.NORTH : direction;
    }

    /**
     * Checks if the given pitch is valid for a face.
     *
     * @param pitch the pitch to check
     * @return true if the pitch is valid, otherwise false
     */
    public static boolean isValidFace(double pitch) {
        return pitch >= -50 && pitch <= 50;
    }

    /**
     * Gets the invalid direction for a given pitch.
     *
     * @param pitch the pitch to check
     * @return the invalid direction
     */
    public static @NotNull Direction getInvalidDirection(double pitch) {
        return pitch > 50 ? Direction.DOWN : Direction.UP;
    }

    private DirectionFaceHelper() {
        // Prevent instantiation
    }
}
