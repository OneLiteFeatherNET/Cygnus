package net.onelitefeather.cygnus.stamina;

import net.minestom.server.MinecraftServer;
import net.minestom.server.timer.Task;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import org.jetbrains.annotations.NotNull;

import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * The StaminaBar class is designed to manage and display stamina-related information for a player in a game.
 * It offers a customizable mechanism to update and render stamina data periodically.
 * The core functionality of the class revolves around a timer that ticks at regular intervals defined by the provided period parameter.
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 **/
public abstract sealed class StaminaBar implements Runnable permits SlenderBar, FoodBar {

    protected final CygnusPlayer player;
    private final ChronoUnit chronoUnit;
    protected int period;
    protected Status status;
    private Task task;

    /**
     * Creates a new reference from an {@link StaminaBar}.
     * @param player the player who owns the bar
     * @param chronoUnit the tick interval for the bar
     * @param period the tick period for the par
     */
    protected StaminaBar(@NotNull CygnusPlayer player, @NotNull ChronoUnit chronoUnit, int period) {
        this.player = player;
        this.chronoUnit = chronoUnit;
        this.period = period;
    }

    protected abstract void onStart();

    /**
     * Creates a new {@link Task} which executes the {@link StaminaBar#consume()} method on each iteration.
     */
    public void start() {
        if (task != null) return;
        this.onStart();
        task = MinecraftServer.getSchedulerManager()
                .buildTask(this::consume)
                .repeat(this.period, this.chronoUnit).schedule();
    }

    /**
     * Stops the active {@link Task} from the {@link StaminaBar}.
     */
    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    /**
     * The implementation of this method includes the logic which is executed on each tick of the {@link StaminaBar}.
     */
    public abstract void consume();

    /**
     * Triggers on each tick the {@link StaminaBar#consume()} method.
     */
    @Override
    public void run() {
        this.consume();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StaminaBar that = (StaminaBar) o;
        return Objects.equals(player.getUuid(), that.player.getUuid());
    }

    @Override
    public int hashCode() {
        return player.getUuid().hashCode();
    }

    /**
     * The enum contains all statuses which an {@link StaminaBar} can have.
     * @author theEvilReaper
     * @version 1.0.0
     * @since 1.0.0
     **/
    public enum Status {

        READY, DRAINING, REGENERATING
    }
}
