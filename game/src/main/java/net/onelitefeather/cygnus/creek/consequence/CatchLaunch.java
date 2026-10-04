package net.onelitefeather.cygnus.creek.consequence;

import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;

/**
 * Throws a caught survivor up into the air.
 * <p>
 * {@link FaceLock} only turns the head and never holds the body, and slowness only changes
 * walking, so neither gets in the way of the throw: the velocity packet sets the client's motion
 * outright. There is no fall damage to suppress: Minestom has none and Cygnus adds none. A height
 * of 0 turns the launch off: nothing happens, which is not a failure.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.1.0
 * @since 2.15.0
 */
public final class CatchLaunch implements CatchTrick {

    /** How much of the upward speed is kept each tick, as in vanilla. */
    static final double DRAG = 0.98D;

    /** How much a rising body loses per tick, in blocks per tick, as in vanilla. */
    static final double GRAVITY = 0.08D;

    private static final int SEARCH_STEPS = 80;
    private static final double MAX_SPEED_PER_TICK = 5.0D;

    private final double velocity;

    /**
     * Sets up the launch.
     *
     * @param height how high the survivor is thrown, in blocks; 0 or less turns the launch off
     */
    public CatchLaunch(double height) {
        this.velocity = launchSpeed(height);
    }

    @Override
    public boolean perform(Player survivor) {
        if (this.velocity > 0.0D) {
            survivor.setVelocity(new Vec(0.0D, this.velocity, 0.0D));
        }
        return true;
    }

    /**
     * Works out how fast to launch something so that it peaks at a height.
     * <p>
     * The client moves a body by its speed and then applies gravity and drag every tick, so
     * the apex is found by running that through {@link #apexOf(double)} and searching for the speed.
     * </p>
     *
     * @param height the wanted apex above the start, in blocks
     * @return the upward speed in blocks per second, as Minestom wants it; 0 for no height
     */
    static double launchSpeed(double height) {
        if (height <= 0.0D) return 0.0D;
        double low = 0.0D;
        double high = MAX_SPEED_PER_TICK;
        for (int i = 0; i < SEARCH_STEPS; i++) {
            double middle = (low + high) / 2.0D;
            if (apexOf(middle) < height) low = middle; else high = middle;
        }
        return high * 20.0D;
    }

    /**
     * Runs the vertical physics until the body stops rising.
     *
     * @param perTick the upward speed, in blocks per tick
     * @return how high it gets, in blocks
     */
    static double apexOf(double perTick) {
        double speed = perTick;
        double height = 0.0D;
        while (speed > 0.0D) {
            height += speed;
            speed = (speed - GRAVITY) * DRAG;
        }
        return height;
    }
}
