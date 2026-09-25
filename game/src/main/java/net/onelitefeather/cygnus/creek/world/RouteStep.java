package net.onelitefeather.cygnus.creek.world;

import net.minestom.server.coordinate.Pos;

/**
 * The next waypoint of the creek and how long it waits once it gets there.
 *
 * @param target      where the creek walks to
 * @param pauseMillis how long it waits after arriving, in milliseconds
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public record RouteStep(Pos target, int pauseMillis) {
}
