package net.onelitefeather.cygnus.creek.world;

import net.minestom.server.coordinate.Pos;

/**
 * The creek's next waypoint and how long it rests once it gets there.
 *
 * @param target      where the creek walks to
 * @param pauseMillis how long it rests after arriving, in milliseconds
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public record RouteStep(Pos target, int pauseMillis) {
}
