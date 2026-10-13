package net.onelitefeather.cygnus.footprint;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Lays out the scent: a short line of prints in front of the slender that points towards a track.
 * <p>
 * The line only gives a direction. It starts right in front of the slender and stays short, so it
 * says nothing about how far away the track is. The direction is turned by a random angle, so
 * the slender still has to search.
 * </p>
 *
 * @author Joltra
 * @version 1.0.0
 * @since 2.16.0
 */
final class ScentTrail {

    /** How far in front of the slender the first print lies, in blocks. */
    static final double START_BLOCKS = 2.0D;

    /** How far apart two prints lie, in blocks. */
    static final double SPACING_BLOCKS = 2.0D;

    private static final double SAME_SPOT_SQUARED = 1.0E-6D;

    /**
     * Lays out the prints.
     *
     * @param slender       where the slender stands
     * @param target        the track to point at
     * @param prints        how many prints to lay out
     * @param spreadDegrees how far the line may turn away from the target, to either side
     * @param random        rolls the turn
     * @return the positions for the prints, at the height of the slender, or none if the target lies right above or below him
     */
    static List<Pos> spots(Pos slender, Point target, int prints, double spreadDegrees, RandomGenerator random) {
        double dx = target.x() - slender.x();
        double dz = target.z() - slender.z();
        if (dx * dx + dz * dz < SAME_SPOT_SQUARED) return List.of();

        double turn = Math.toRadians((random.nextDouble() * 2.0D - 1.0D) * spreadDegrees);
        double angle = Math.atan2(dz, dx) + turn;
        double stepX = Math.cos(angle);
        double stepZ = Math.sin(angle);
        List<Pos> spots = new ArrayList<>(prints);
        for (int i = 0; i < prints; i++) {
            double distance = START_BLOCKS + i * SPACING_BLOCKS;
            spots.add(slender.add(stepX * distance, 0.0D, stepZ * distance));
        }
        return spots;
    }

    private ScentTrail() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}
