package net.onelitefeather.cygnus.utils;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;

/**
 * Turns a yaw into the direction it faces on the ground.
 * <p>
 * Minecraft's yaw goes clockwise from south (+Z), so the direction is (-sin, cos).
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.21.0
 */
public final class Heading {

    private Heading() {
    }

    /**
     * Tells which way a yaw faces, flat on the ground.
     *
     * @param yaw the yaw in degrees
     * @return the direction, one block long and without height
     */
    public static Vec forward(double yaw) {
        double radians = Math.toRadians(yaw);
        return new Vec(-Math.sin(radians), 0.0D, Math.cos(radians));
    }

    /**
     * Walks a distance from a position in the direction of a yaw, at the same height.
     *
     * @param from     where to start
     * @param yaw      which way to walk, in degrees
     * @param distance how far to walk, in blocks
     * @return where the walk ends, without a view
     */
    public static Pos step(Pos from, double yaw, double distance) {
        Vec direction = forward(yaw);
        return new Pos(from.x() + direction.x() * distance, from.y(), from.z() + direction.z() * distance);
    }
}
