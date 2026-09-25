package net.onelitefeather.cygnus.common.creek;

import net.minestom.server.coordinate.Vec;

/**
 * One point of a creek route: where the creek's feet stand, and how long it waits there.
 * <p>
 * The fields are flat so the JSON keeps the {@code x}, {@code y}, {@code z} layout of older files.
 * A file without {@code pauseMillis} loads with a pause of 0.
 * </p>
 *
 * @param x           the x coordinate
 * @param y           the y coordinate
 * @param z           the z coordinate
 * @param pauseMillis how long the creek waits after reaching this point, in milliseconds
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public record CreekWaypoint(double x, double y, double z, int pauseMillis) {

    /**
     * Creates a waypoint without a pause.
     *
     * @param position where the creek's feet stand
     * @return the waypoint
     */
    public static CreekWaypoint of(Vec position) {
        return new CreekWaypoint(position.x(), position.y(), position.z(), 0);
    }

    /**
     * Returns where the creek's feet stand.
     *
     * @return the position
     */
    public Vec position() {
        return new Vec(this.x, this.y, this.z);
    }

    /**
     * Returns a copy of this waypoint with another pause.
     *
     * @param pauseMillis the new pause, in milliseconds
     * @return the copy
     */
    public CreekWaypoint withPause(int pauseMillis) {
        return new CreekWaypoint(this.x, this.y, this.z, pauseMillis);
    }
}
