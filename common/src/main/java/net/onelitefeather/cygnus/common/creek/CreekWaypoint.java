package net.onelitefeather.cygnus.common.creek;

import net.minestom.server.coordinate.Vec;

/**
 * One point of a creek route: where the creek's feet stand, and how long it waits there.
 * <p>
 * In the JSON the position is a nested object written by the position adapter, as in
 * {@code {"position":{"x":…,"y":…,"z":…},"pauseMillis":…}}. A point without {@code pauseMillis}
 * loads with a pause of 0. A point without a position is reported by {@link CreekRoutesFile}.
 * </p>
 *
 * @param position    where the creek's feet stand
 * @param pauseMillis how long the creek waits after reaching this point, in milliseconds
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public record CreekWaypoint(Vec position, int pauseMillis) {

    /**
     * Creates a waypoint without a pause.
     *
     * @param position where the creek's feet stand
     * @return the waypoint
     */
    public static CreekWaypoint of(Vec position) {
        return new CreekWaypoint(position, 0);
    }

    /**
     * Returns a copy of this waypoint with another pause.
     *
     * @param pauseMillis the new pause, in milliseconds
     * @return the copy
     */
    public CreekWaypoint withPause(int pauseMillis) {
        return new CreekWaypoint(this.position, pauseMillis);
    }
}
