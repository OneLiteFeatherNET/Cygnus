package net.onelitefeather.cygnus.utils;

import net.minestom.server.coordinate.Pos;

/**
 * Where a standing player's eyes are, worked out from their feet. Handy for figures that are only
 * packets and have no entity to ask.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.21.0
 */
public final class Eyes {

    /** How high a standing player's eyes are, in blocks. */
    public static final double HEIGHT = 1.62D;

    private Eyes() {
    }

    /**
     * Lifts a position from the feet to the eyes.
     *
     * @param feet where the feet are, including where they look
     * @return the eyes, looking the same way
     */
    public static Pos of(Pos feet) {
        return feet.add(0, HEIGHT, 0);
    }
}
