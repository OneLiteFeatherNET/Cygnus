package net.onelitefeather.cygnus.phase.task;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;

import java.util.Collection;

/**
 * Handles the action bar display logic for players while the lobby is waiting
 * for the minimum required player count to start the match countdown.
 * <p>
 * This class is designed to be reusable. The tick-aligned scheduler can be started
 * and stopped dynamically without re-instantiating the object.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.3.0
 * @since 1.0.0
 **/
public final class LobbyWaitingTask {

    private static final Component PREFIX_NEED = Component.text("Need", NamedTextColor.GRAY);
    private static final Component SUFFIX_TO_START = Component.text("players to start", NamedTextColor.GRAY);
    private static final Component SPACE = Component.space();
    private static final int DISPLAY_REFRESH_INTERVAL_TICKS = 40; // Approx. 2 seconds

    private final int minPlayers;
    private final Component[] cachedComponents;
    private final Runnable tickRunnable;

    private Component displayComponent = Component.empty();
    private int tickCounter = 0;
    private boolean running = false;

    /**
     * Creates a new instance of the waiting display and starts the tick-aligned loop.
     *
     * @param minPlayers the minimum number of players required to start the game
     */
    public LobbyWaitingTask(int minPlayers) {
        this.minPlayers = minPlayers;
        this.cachedComponents = new Component[minPlayers + 1];

        // Pre-build all possible text components for player differences [0 ... minPlayers]
        for (int diff = 0; diff <= minPlayers; diff++) {
            this.cachedComponents[diff] = PREFIX_NEED
                    .append(SPACE)
                    .append(Component.text(diff, NamedTextColor.RED))
                    .append(SPACE)
                    .append(SUFFIX_TO_START);
        }

        // Define the self-scheduling tick task
        this.tickRunnable = new Runnable() {
            @Override
            public void run() {
                if (!running) {
                    return; // Gracefully exit the loop when stopped
                }

                // Send the action bar only at the specified tick interval
                if (tickCounter++ % DISPLAY_REFRESH_INTERVAL_TICKS == 0) {
                    sendToAll(MinecraftServer.getConnectionManager().getOnlinePlayers());
                }

                // Schedule this task again for the next server tick
                MinecraftServer.getSchedulerManager().scheduleNextTick(this);
            }
        };

        // Start the loop automatically on creation
        this.start();
    }

    /**
     * Starts the tick-aligned display loop if it is not already running.
     */
    public void start() {
        if (this.running) {
            return; // Prevent duplicate scheduler registrations
        }
        this.running = true;
        this.tickCounter = 0;
        MinecraftServer.getSchedulerManager().scheduleNextTick(this.tickRunnable);
    }

    /**
     * Stops the tick-aligned loop.
     * The loop will automatically terminate on the next scheduled tick.
     */
    public void stop() {
        this.running = false;
    }

    /**
     * Updates the player count difference and immediately refreshes the action bar
     * for all online players using pre-cached components.
     *
     * @param onlineCount the current number of online players
     */
    public void update(int onlineCount) {
        int playerDifference = Math.clamp(this.minPlayers - onlineCount, 0, this.minPlayers);
        this.displayComponent = this.cachedComponents[playerDifference];

        if (running) {
            sendToAll(MinecraftServer.getConnectionManager().getOnlinePlayers());
        }
    }

    /**
     * Sends the current action bar message immediately to a specific player
     * (e.g., when a player joins the server).
     *
     * @param player the player to send the message to
     */
    public void send(Player player) {
        if (running && displayComponent != Component.empty()) {
            player.sendActionBar(this.displayComponent);
        }
    }

    private void sendToAll(Collection<Player> players) {
        if (players.isEmpty() || displayComponent == Component.empty()) {
            return;
        }
        for (Player player : players) {
            player.sendActionBar(this.displayComponent);
        }
    }
}