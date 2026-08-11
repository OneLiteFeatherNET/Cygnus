package net.onelitefeather.cygnus.common.util;

import net.minestom.server.MinecraftServer;
import net.minestom.server.timer.Task;
import org.jetbrains.annotations.Nullable;

import java.time.temporal.TemporalUnit;

/**
 * Owns a single Minestom repeating scheduler {@link Task}.
 * <p>
 * {@code AmbientProvider}, {@code TunnelVisionService}, {@code SlenderGazeService} and
 * {@code BloodSplatterService} each hand-rolled the same {@code @Nullable Task} field with a
 * guard-and-return {@code startTask()}/{@code stopTask()} pair. This type is that field, extracted
 * once: start and stop are both idempotent, so a caller never has to remember whether it already
 * called either of them.
 * </p>
 * <p>
 * The action to run is constructor-injected rather than passed to {@link #start(long, TemporalUnit)},
 * because every one of the four services above ran exactly one action for the lifetime of the task
 * and never swapped it out.
 * </p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * RepeatingTask task = new RepeatingTask(this::tick);
 * task.start(1, ChronoUnit.SECONDS);
 * // ...
 * task.stop();
 * }</pre>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
public final class RepeatingTask {

    private final Runnable action;
    private @Nullable Task task;

    /**
     * Creates a task that, once started, runs the given action on every repetition.
     *
     * @param action the action to run
     */
    public RepeatingTask(Runnable action) {
        this.action = action;
    }

    /**
     * Starts the task with the given period. Does nothing if the task is already running.
     *
     * @param period the amount of {@code unit}s between two runs
     * @param unit   the unit {@code period} is measured in
     */
    public void start(long period, TemporalUnit unit) {
        if (this.task != null) return;
        this.task = MinecraftServer.getSchedulerManager()
                .buildTask(this.action)
                .repeat(period, unit)
                .schedule();
    }

    /**
     * Stops the task. Does nothing if the task is not running.
     */
    public void stop() {
        if (this.task == null) return;
        this.task.cancel();
        this.task = null;
    }

    /**
     * @return {@code true} if the task is currently running
     */
    public boolean isRunning() {
        return this.task != null;
    }
}
