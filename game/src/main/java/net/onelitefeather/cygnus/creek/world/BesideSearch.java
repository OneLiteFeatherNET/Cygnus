package net.onelitefeather.cygnus.creek.world;

import net.minestom.server.coordinate.Pos;
import net.onelitefeather.cygnus.utils.Heading;

import java.util.Optional;
import java.util.function.Function;
import java.util.random.RandomGenerator;

/**
 * Tries random spots off to the side of someone's view until one fits. The creek looks for a spot
 * nobody sees, the lure for one a living player could stand on, so what fits is up to the caller.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.21.0
 */
public final class BesideSearch {

    /** How many candidate spots are tried per search. */
    static final int ATTEMPTS = 16;

    private BesideSearch() {
    }

    /**
     * Finds a spot off to the side of someone's view.
     *
     * @param target      where the target's feet are, their yaw is where they look
     * @param minDistance how close to the target the spot may be at the least
     * @param maxDistance how far from the target the spot may be at the most
     * @param minAngle    how far off to the side of their view it has to be at the least, in degrees
     * @param maxAngle    how far off to the side of their view it may be at the most, in degrees
     * @param fit         turns a candidate into a spot, or empty if it does not fit
     * @param random      the random source
     * @return the first spot that fits, or empty if no candidate did
     */
    public static Optional<Pos> find(Pos target, double minDistance, double maxDistance, double minAngle,
                                     double maxAngle, Function<Pos, Optional<Pos>> fit, RandomGenerator random) {
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            double offset = random.nextDouble(minAngle, maxAngle) * (random.nextBoolean() ? 1.0D : -1.0D);
            double distance = random.nextDouble(minDistance, maxDistance);
            Optional<Pos> spot = fit.apply(Heading.step(target, target.yaw() + offset, distance));
            if (spot.isPresent()) return spot;
        }
        return Optional.empty();
    }
}
