package net.onelitefeather.cygnus.phase;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.instance.Instance;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.map.event.GameMapLoadEvent;
import net.onelitefeather.cygnus.map.event.GamePrepareEvent;
import net.onelitefeather.cygnus.phase.task.LobbyCountdownSoundTask;
import net.onelitefeather.cygnus.phase.task.LobbyTimeTransitionTask;
import net.onelitefeather.cygnus.phase.task.LobbyWaitingTask;
import net.theevilreaper.xerus.api.phase.TickDirection;
import net.theevilreaper.xerus.api.phase.TimedPhase;

import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;

import static net.onelitefeather.cygnus.common.config.GameConfig.FORCE_START_TIME;

/**
 * Represents the lobby phase of the game.
 * <p>
 * During this phase, the game waits until enough players have joined to start the
 * match countdown. The phase updates the player level and experience bar to
 * visualize the remaining time until the game starts, manages the action bar
 * waiting display using a tick-aligned scheduler, smoothly transitions world time
 * towards midnight as the countdown nears completion, and plays a tension-building
 * horror sound cue during the last seconds before the match starts.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.4.0
 * @since 1.0.0
 */
public final class LobbyPhase extends TimedPhase {

    private static final int TIME_TRANSITION_START_SECONDS = LobbyCountdownSoundTask.WINDOW_START_SECONDS;

    private final int lobbyTime;
    private final int minPlayers;
    private final LobbyWaitingTask waitingDisplay;
    private final LobbyTimeTransitionTask timeTransitionTask;
    private final LobbyCountdownSoundTask countdownSoundTask;
    private boolean forceStarted;

    /**
     * Constructs a new LobbyPhase.
     *
     * @param gameConfig the configuration settings for the game
     */
    public LobbyPhase(GameConfig gameConfig) {
        this(gameConfig, () -> null);
    }

    /**
     * Constructs a new LobbyPhase with an instance supplier for time transitions.
     *
     * @param gameConfig       the configuration settings for the game
     * @param instanceSupplier supplier for the active instance
     */
    public LobbyPhase(GameConfig gameConfig, Supplier<Instance> instanceSupplier) {
        super("Lobby", ChronoUnit.SECONDS, 1);
        this.lobbyTime = gameConfig.lobbyTime();
        this.minPlayers = gameConfig.minPlayers();
        this.setPaused(true);
        this.setCurrentTicks(lobbyTime);
        this.setTickDirection(TickDirection.DOWN);

        // Instantiate the waiting display and time transition task
        this.waitingDisplay = new LobbyWaitingTask(this.minPlayers);
        this.waitingDisplay.update(MinecraftServer.getConnectionManager().getOnlinePlayers().size());
        this.timeTransitionTask = new LobbyTimeTransitionTask(instanceSupplier, TIME_TRANSITION_START_SECONDS);
        this.countdownSoundTask = new LobbyCountdownSoundTask();
    }

    /**
     * Checks if the start condition is met.
     * If the online player count reaches the minimum requirement, the countdown starts
     * and the waiting action bar display is stopped.
     */
    public void checkStartCondition() {
        int onlineCount = MinecraftServer.getConnectionManager().getOnlinePlayers().size();

        if (isPaused() && onlineCount >= this.minPlayers) {
            this.setPaused(false);
            this.waitingDisplay.stop(); // Stop the loop, but keep the final object
        } else {
            this.waitingDisplay.update(onlineCount);
        }
    }

    /**
     * Checks if the countdown should stop when players leave the server.
     * Re-activates the waiting display if the player count drops below the requirement.
     */
    public void checkStopCondition() {
        int onlineCount = MinecraftServer.getConnectionManager().getOnlinePlayers().size();

        if (onlineCount - 1 < this.minPlayers) {
            this.setPaused(true);
            this.setCurrentTicks(this.lobbyTime);
            this.forceStarted = false;
            this.setLevel();

            this.timeTransitionTask.reset();
            this.countdownSoundTask.reset();
            // Simply restart the existing display instead of allocating a new one
            this.waitingDisplay.start();
            this.waitingDisplay.update(onlineCount);
        }
    }

    @Override
    public void start() {
        super.start();
        setLevel();
    }

    @Override
    protected void onFinish() {
        this.waitingDisplay.stop(); // Stop the tick-aligned loop
        this.timeTransitionTask.stop();
    }

    @Override
    public void onUpdate() {
        setLevel();
        this.countdownSoundTask.onTick(getCurrentTicks());

        if (getCurrentTicks() == TIME_TRANSITION_START_SECONDS) {
            this.timeTransitionTask.start();
        }

        if (getCurrentTicks() == FORCE_START_TIME - 1) {
            EventDispatcher.call(new GameMapLoadEvent());
        }

        if (getCurrentTicks() == 0) {
            EventDispatcher.call(new GamePrepareEvent());
        }
    }

    /**
     * Checks if the player requirements are met.
     * Re-creates the display if the player count drops below the threshold.
     */
    public void checkPlayerRequirements() {
        int onlineCount = MinecraftServer.getConnectionManager().getOnlinePlayers().size();

        if (onlineCount - 1 < this.minPlayers) {
            this.setPaused(true);
            this.setForceStarted(false);
            this.setCurrentTicks(this.lobbyTime);
            this.setLevel(this.lobbyTime);

            this.timeTransitionTask.reset();
            this.countdownSoundTask.reset();
            // Simply restart the existing display instead of allocating a new one
            this.waitingDisplay.start();
            this.waitingDisplay.update(onlineCount);
        }
    }

    private void setLevel() {
        this.setLevel(getCurrentTicks());
    }

    /**
     * Sets the level and XP progress for all online players.
     *
     * @param amount the current countdown level
     */
    private void setLevel(int amount) {
        if (amount < 0) return;
        int time = isForceStarted() ? FORCE_START_TIME : this.lobbyTime;
        float currentExpCount = (float) this.getCurrentTicks() / time;
        for (Player onlinePlayer : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            onlinePlayer.setLevel(amount);
            onlinePlayer.setExp(currentExpCount);
        }
    }

    /**
     * Sets the level for a specific player (typically called on join).
     * Also immediately sends the waiting action bar if the lobby is paused.
     *
     * @param player the player to set the level for
     */
    public void setLevel(Player player) {
        if (getCurrentTicks() < 0) return;
        player.setLevel(getCurrentTicks());
        player.setExp(getCurrentTicks() / (float) this.lobbyTime);

        // Immediately send the action bar to the newly spawned player
        if (isPaused()) {
            this.waitingDisplay.send(player);
        }
    }

    /**
     * Returns if the phase was force started.
     *
     * @return true if the phase was force started, otherwise false
     */
    public boolean isForceStarted() {
        return forceStarted;
    }

    /**
     * Sets whether the phase is force started.
     *
     * @param forceStarted true to force start, otherwise false
     */
    public void setForceStarted(boolean forceStarted) {
        if (forceStarted) {
            this.setCurrentTicks(FORCE_START_TIME);
        }
        this.forceStarted = forceStarted;
    }
}