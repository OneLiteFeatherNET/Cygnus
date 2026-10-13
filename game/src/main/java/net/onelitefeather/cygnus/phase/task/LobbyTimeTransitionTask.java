package net.onelitefeather.cygnus.phase.task;

import net.onelitefeather.cygnus.telemetry.TickSectionNames;
import net.onelitefeather.cygnus.telemetry.TickSections;
import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.Instance;
import net.onelitefeather.cygnus.common.util.Helper;

import java.util.function.Supplier;

/**
 * Task that smoothly transitions the world time of an {@link Instance} to a target timestamp
 * over a specified duration in seconds using tick-aligned linear interpolation.
 *
 * @author theEvilReaper
 * @version 1.1.0
 * @since 2.7.0
 */
public final class LobbyTimeTransitionTask {

    private static final int TICKS_PER_SECOND = 20;

    private final Supplier<Instance> instanceSupplier;
    private final long targetTime;
    private final int totalTicks;
    private final Runnable tickRunnable;

    private long initialTime;
    private int currentTick = 0;
    private boolean running = false;
    private boolean initialized = false;

    /**
     * Creates a new instance of {@link LobbyTimeTransitionTask} targeting {@link Helper#MIDNIGHT_TIME}.
     *
     * @param instanceSupplier supplier for the target instance
     * @param durationSeconds duration of the transition in seconds
     */
    public LobbyTimeTransitionTask(Supplier<Instance> instanceSupplier, int durationSeconds) {
        this(instanceSupplier, Helper.MIDNIGHT_TIME, durationSeconds);
    }

    /**
     * Creates a new instance of {@link LobbyTimeTransitionTask}.
     *
     * @param instanceSupplier supplier for the target instance
     * @param targetTime      the target world time in ticks
     * @param durationSeconds duration of the transition in seconds
     */
    public LobbyTimeTransitionTask(Supplier<Instance> instanceSupplier, long targetTime, int durationSeconds) {
        this(instanceSupplier, targetTime, durationSeconds, TickSections.NONE);
    }

    /**
     * Creates a new instance of {@link LobbyTimeTransitionTask} with its ticks measured for the slow
     * tick report.
     *
     * @param instanceSupplier supplier for the target instance
     * @param targetTime       the target world time in ticks
     * @param durationSeconds  duration of the transition in seconds
     * @param sections         measures how long each tick of the transition takes
     * @since 2.15.0
     */
    public LobbyTimeTransitionTask(Supplier<Instance> instanceSupplier, long targetTime, int durationSeconds,
                                   TickSections sections) {
        this.instanceSupplier = instanceSupplier;
        this.targetTime = targetTime;
        this.totalTicks = Math.max(1, durationSeconds * TICKS_PER_SECOND);

        Runnable step = sections.wrap(TickSectionNames.LOBBY_TIME, this::advanceTime);

        this.tickRunnable = new Runnable() {
            @Override
            public void run() {
                if (!running) {
                    return;
                }

                step.run();

                if (currentTick < totalTicks && running) {
                    MinecraftServer.getSchedulerManager().scheduleNextTick(this);
                } else {
                    running = false;
                }
            }
        };
    }

    /**
     * Moves the world time one tick closer to the target.
     */
    private void advanceTime() {
        Instance instance = this.instanceSupplier.get();

        if (!this.initialized) {
            this.initialTime = instance.getTime();
            this.initialized = true;
        }

        this.currentTick++;
        double progress = Math.min(1.0, (double) this.currentTick / this.totalTicks);
        long calculatedTime = (long) (this.initialTime + (progress * (this.targetTime - this.initialTime)));
        instance.setTime(calculatedTime);
    }

    /**
     * Starts the smooth time transition if not already running.
     */
    public void start() {
        if (this.running) {
            return;
        }

        Instance instance = instanceSupplier.get();

        this.running = true;
        this.initialized = false;
        this.currentTick = 0;
        var defaultClock = instance.defaultClock();
        if (defaultClock != null) {
            defaultClock.rate(0f);
        }
        MinecraftServer.getSchedulerManager().scheduleNextTick(this.tickRunnable);
    }

    /**
     * Stops the time transition task.
     */
    public void stop() {
        this.running = false;
    }

    /**
     * Stops the transition task and resets the world time to its initial value.
     */
    public void reset() {
        stop();
        if (initialized) {
            Instance instance = instanceSupplier.get();
            instance.setTime(initialTime);
            initialized = false;
            currentTick = 0;
        }
    }

    /**
     * Checks whether the transition task is currently running.
     *
     * @return true if running, otherwise false
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Returns the target world time.
     *
     * @return target world time in ticks
     */
    public long getTargetTime() {
        return targetTime;
    }
}
