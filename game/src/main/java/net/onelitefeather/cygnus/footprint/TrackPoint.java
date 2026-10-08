package net.onelitefeather.cygnus.footprint;

import net.minestom.server.coordinate.Pos;

/**
 * A point a survivor walked through.
 *
 * @param position   where the survivor was
 * @param timeMillis when they were there
 * @author Joltra
 * @version 1.0.0
 * @since 2.17.0
 */
record TrackPoint(Pos position, long timeMillis) {
}
