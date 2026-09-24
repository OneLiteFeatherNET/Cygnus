package net.onelitefeather.cygnus.creek.world;

import net.minestom.server.coordinate.Pos;

import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * Finds spots where the creek can appear without anyone seeing it happen.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class SpotFinder {

    /** How many candidate spots are tried per step. */
    static final int ATTEMPTS = 16;

    /** Height of the middle of the creek's body. This is the point a survivor would see. */
    public static final double BODY_CENTRE = 1.4D;

    private final CreekSight sight;
    private final Ground ground;

    /**
     * Creates the spot finder.
     *
     * @param sight  the view check a spot must pass
     * @param ground moves candidates onto a floor
     */
    public SpotFinder(CreekSight sight, Ground ground) {
        this.sight = sight;
        this.ground = ground;
    }

    /**
     * Checks that nobody would see the creek at a spot.
     *
     * @param spot          the position of the creek's feet
     * @param observerEyes  the eye positions of everyone who could see the creek
     * @param personalSpace the minimum distance to every observer
     * @return {@code true} if the spot is far enough away and outside every view cone
     */
    public boolean isHidden(Pos spot, List<Pos> observerEyes, double personalSpace) {
        Pos body = spot.add(0, BODY_CENTRE, 0);
        for (Pos eyes : observerEyes) {
            if (eyes.distance(spot) < personalSpace) return false;
            if (this.sight.inView(eyes, body)) return false;
        }
        return true;
    }

    /**
     * Moves a candidate onto the floor and keeps it only if nobody would see the creek there.
     *
     * @param candidate     the spot to try
     * @param observerEyes  the eye positions of everyone who could see the creek
     * @param personalSpace the minimum distance to every observer
     * @return the hidden spot, or empty
     */
    public Optional<Pos> hiddenSpotAt(Pos candidate, List<Pos> observerEyes, double personalSpace) {
        return this.ground.settle(candidate).filter(spot -> this.isHidden(spot, observerEyes, personalSpace));
    }

    /**
     * Finds a hidden spot to the side of a target's view.
     *
     * @param target        the target's feet; its yaw is the view direction
     * @param minDistance   the minimum distance to the target
     * @param maxDistance   the maximum distance to the target
     * @param minAngle      the minimum angle from the target's view direction, in degrees
     * @param maxAngle      the maximum angle from the target's view direction, in degrees
     * @param observerEyes  the eye positions of everyone who could see the creek
     * @param personalSpace the minimum distance to every observer
     * @param random        the random source
     * @return a hidden spot, or empty if no candidate worked
     */
    public Optional<Pos> beside(Pos target, double minDistance, double maxDistance, double minAngle,
                                double maxAngle, List<Pos> observerEyes, double personalSpace,
                                RandomGenerator random) {
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            double offset = random.nextDouble(minAngle, maxAngle) * (random.nextBoolean() ? 1.0D : -1.0D);
            double distance = random.nextDouble(minDistance, maxDistance);
            Optional<Pos> spot = this.hiddenSpotAt(step(target, target.yaw() + offset, distance), observerEyes, personalSpace);
            if (spot.isPresent()) return spot;
        }
        return Optional.empty();
    }

    private static Pos step(Pos from, double yaw, double distance) {
        double radians = Math.toRadians(yaw);
        // Minecraft's yaw goes clockwise from south (+Z), so the direction is (-sin, cos).
        return new Pos(from.x() - Math.sin(radians) * distance, from.y(), from.z() + Math.cos(radians) * distance);
    }
}
