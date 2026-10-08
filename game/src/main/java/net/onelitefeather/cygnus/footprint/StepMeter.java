package net.onelitefeather.cygnus.footprint;

import net.minestom.server.coordinate.Point;

/**
 * Adds up the distance a player walks and reports every time a full step is done.
 * <p>
 * Minestom has no step event, so the distance comes from the moves. Only the horizontal part
 * counts, jumping or falling on the spot is not walking. A single move longer than the teleport
 * limit is not walked at all. So a creek throw neither completes a step nor leaves a point in the
 * middle of nowhere.
 * </p>
 *
 * @author Joltra
 * @version 1.0.0
 * @since 2.16.0
 */
final class StepMeter {

    private final double stepBlocks;
    private final double teleportBlocks;
    private double walked;

    /**
     * Creates a meter.
     *
     * @param stepBlocks     the length of a step, in blocks
     * @param teleportBlocks a single move longer than this is a teleport
     */
    StepMeter(double stepBlocks, double teleportBlocks) {
        this.stepBlocks = stepBlocks;
        this.teleportBlocks = teleportBlocks;
    }

    /**
     * Adds a move.
     *
     * @param from where the move started
     * @param to   where the move ended
     * @return {@code true} if the move completed a step
     */
    boolean advance(Point from, Point to) {
        double dx = to.x() - from.x();
        double dz = to.z() - from.z();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance > this.teleportBlocks) return false;
        this.walked += distance;
        if (this.walked < this.stepBlocks) return false;
        this.walked = 0.0D;
        return true;
    }

    /**
     * Drops the distance walked so far.
     */
    void reset() {
        this.walked = 0.0D;
    }
}
