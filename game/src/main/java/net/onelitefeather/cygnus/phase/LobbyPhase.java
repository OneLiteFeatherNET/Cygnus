package net.onelitefeather.cygnus.phase;

import de.icevizion.aves.util.functional.VoidConsumer;
import de.icevizion.xerus.api.phase.TickDirection;
import de.icevizion.xerus.api.phase.TimedPhase;
import net.infumia.agones4j.Agones;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.network.ConnectionManager;
import net.minestom.server.timer.Task;
import org.jetbrains.annotations.NotNull;

import java.time.temporal.ChronoUnit;

import static net.onelitefeather.cygnus.common.config.GameConfig.FORCE_START_TIME;

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 **/
public final class LobbyPhase extends TimedPhase {

    private static final ConnectionManager CONNECTION_MANAGER = MinecraftServer.getConnectionManager();
    private boolean forceStarted;
    private final Task waitingTask;
    private final VoidConsumer gameMapLoading;
    private final VoidConsumer staminaInstantiation;
    private final VoidConsumer worldUpdater;
    private final int lobbyTime;
    private final int minPlayers;
    private Component displayComponent;
    private final Agones agones;

    public LobbyPhase(
            @NotNull VoidConsumer gameMapLoading,
            @NotNull VoidConsumer staminaInstantiation,
            @NotNull VoidConsumer worldUpdater,
            int lobbyTime,
            int minPlayers,
            Agones agones
    ) {
        super("Lobby", ChronoUnit.SECONDS, 1);
        this.gameMapLoading = gameMapLoading;
        this.staminaInstantiation = staminaInstantiation;
        this.worldUpdater = worldUpdater;
        this.lobbyTime = lobbyTime;
        this.minPlayers = minPlayers;
        this.agones = agones;
        this.setPaused(true);
        this.setCurrentTicks(lobbyTime);
        this.setTickDirection(TickDirection.DOWN);
        this.setEndTicks(-5);
        this.updateComponent();
        this.waitingTask = MinecraftServer.getSchedulerManager().buildTask(() -> {
            if (isPaused() || isFinished() || MinecraftServer.getConnectionManager().getOnlinePlayers().isEmpty())
                return;
            MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(
                    player -> player.sendActionBar(this.displayComponent)
            );
        }).repeat(30, ChronoUnit.SECONDS).schedule();
    }

    private void updateComponent() {
        int playerDifference = Math.max(0, this.minPlayers - CONNECTION_MANAGER.getOnlinePlayers().size());
        this.displayComponent = Component.text("Need", NamedTextColor.GRAY)
                .append(Component.space())
                .append(Component.text(playerDifference, NamedTextColor.RED))
                .append(Component.space())
                .append(Component.text("players to start", NamedTextColor.GRAY));
    }

    public void checkStartCondition() {
        this.updateComponent();
        if (isPaused() && CONNECTION_MANAGER.getOnlinePlayers().size() >= this.minPlayers) {
            this.setPaused(false);
        }
    }

    public void checkStopCondition() {
        if (waitingTask.isAlive() && MinecraftServer.getConnectionManager().getOnlinePlayers().size() - 1 <= this.minPlayers) {
            this.setPaused(true);
            this.updateComponent();
            this.setCurrentTicks(this.lobbyTime);
            this.forceStarted = false;
            setLevel();
        }
    }

    public void setForceStarted(boolean forceStarted) {
        if (forceStarted) {
            this.setCurrentTicks(FORCE_START_TIME);
        }
        this.forceStarted = forceStarted;
    }

    @Override
    public void start() {
        super.start();
        setLevel();
        this.agones.ready();
    }

    @Override
    protected void onFinish() {
        this.waitingTask.cancel();
    }

    @Override
    public void onUpdate() {
        setLevel();
        this.worldUpdater.apply();

        if (getCurrentTicks() == FORCE_START_TIME - 1) {
            this.gameMapLoading.apply();
        }

        if (getCurrentTicks() == 0) {
            this.staminaInstantiation.apply();
        }
    }

    /**
     * Checks if the player requirements are met.
     * If the player requirements are not met the phase will be paused.
     */
    public void checkPlayerRequirements() {
        if (MinecraftServer.getConnectionManager().getOnlinePlayers().size() - 1 < this.minPlayers) {
            this.setPaused(true);
            this.setForceStarted(false);
            this.setCurrentTicks(this.lobbyTime);
            this.setLevel(this.lobbyTime);
        }
    }

    private void setLevel() {
        this.setLevel(getCurrentTicks());
    }

    /**
     * Sets the level for all online players.
     * The level is calculated by the current ticks.
     * The experience is calculated by the current ticks divided by the lobby phase time.
     *
     * @param amount the amount of the level
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
     * Sets the level for the given player.
     *
     * @param player the player to set the level
     */
    public void setLevel(@NotNull Player player) {
        player.setLevel(getCurrentTicks());
        player.setExp(getCurrentTicks() / (float) this.lobbyTime);
    }

    /**
     * Returns if the phase was force started.
     *
     * @return True if the phase was force started otherwise false
     */
    public boolean isForceStarted() {
        return forceStarted;
    }
}
