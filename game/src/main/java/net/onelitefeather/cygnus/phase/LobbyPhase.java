package net.onelitefeather.cygnus.phase;

import de.icevizion.xerus.api.phase.TickDirection;
import de.icevizion.xerus.api.phase.TimedPhase;
import de.icevizion.xerus.api.team.Team;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.timer.Task;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.common.map.MapProvider;
import net.onelitefeather.cygnus.common.util.Helper;
import net.onelitefeather.cygnus.stamina.StaminaService;
import org.jetbrains.annotations.NotNull;

import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import static net.onelitefeather.cygnus.common.config.GameConfig.LOBBY_PHASE_TIME;
import static net.onelitefeather.cygnus.common.config.GameConfig.FORCE_START_TIME;

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 **/
public final class LobbyPhase extends TimedPhase {

    private boolean forceStarted;
    private final Function<Integer, @NotNull Team> teamGetter;
    private final StaminaService staminaService;
    private final Consumer<Void> worldUpdater;
    private final MapProvider mapProvider;
    private final Task waitingTask;
    private Component displayComponent;

    public LobbyPhase(
            @NotNull Function<Integer, @NotNull Team> teamGetter,
            @NotNull StaminaService staminaService,
            @NotNull Consumer<Void> worldUpdater,
            @NotNull MapProvider mapProvider
    ) {
        super("Lobby", ChronoUnit.SECONDS, 1);
        this.worldUpdater = worldUpdater;
        this.mapProvider = mapProvider;
        this.setPaused(true);
        this.setCurrentTicks(LOBBY_PHASE_TIME);
        this.setTickDirection(TickDirection.DOWN);
        this.teamGetter = teamGetter;
        this.staminaService = staminaService;
        this.setEndTicks(-5);
        this.updateComponent();
        this.waitingTask = MinecraftServer.getSchedulerManager().buildTask(() -> {
            if (isPaused() || isFinished() || MinecraftServer.getConnectionManager().getOnlinePlayers().isEmpty()) return;
            MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(
                    player -> player.sendActionBar(this.displayComponent)
            );
        }).repeat(30, ChronoUnit.SECONDS).schedule();
    }

    private void updateComponent() {
        this.displayComponent = Messages.withMini("<gray>Need <red>" + (GameConfig.MIN_PLAYERS - MinecraftServer.getConnectionManager().getOnlinePlayers().size() + " <gray>players to start"));
    }

    public void checkStartCondition() {
        this.updateComponent();
        if (isPaused() && MinecraftServer.getConnectionManager().getOnlinePlayers().size() >= GameConfig.MIN_PLAYERS) {
            this.setPaused(false);
        }
    }

    public void checkStopCondition() {
        if (waitingTask.isAlive() && MinecraftServer.getConnectionManager().getOnlinePlayers().size() - 1<= GameConfig.MIN_PLAYERS) {
            this.setPaused(true);
            this.updateComponent();
            this.setCurrentTicks(LOBBY_PHASE_TIME);
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
    }

    @Override
    protected void onFinish() {
        this.waitingTask.cancel();
    }

    @Override
    public void onUpdate() {
        setLevel();
        this.worldUpdater.accept(null);

        if (getCurrentTicks() == FORCE_START_TIME - 1) {
            this.mapProvider.loadGameMap();
        }

        if (getCurrentTicks() == 0) {
            var survivorTeam = teamGetter.apply(1);
            var player = Helper.prepareTeamAllocation(teamGetter.apply(0), survivorTeam);
            System.out.println("Slender is " + player.getUsername());
            this.staminaService.setSlenderBar(player);
            this.staminaService.createStaminaBars(survivorTeam);
        }
    }

    public void checkPlayerRequirements() {
        if (MinecraftServer.getConnectionManager().getOnlinePlayers().size() - 1 < GameConfig.MIN_PLAYERS) {
            this.setPaused(true);
            this.setForceStarted(false);
            this.setCurrentTicks(LOBBY_PHASE_TIME);
            this.setLevel(LOBBY_PHASE_TIME);
        }
    }

    private void setLevel() {
        this.setLevel(getCurrentTicks());
    }

    private void setLevel(int amount) {
        if (amount < 0) return;
        float currentExpCount = (float) this.getCurrentTicks() / (isForceStarted() ? FORCE_START_TIME : LOBBY_PHASE_TIME);
        for (Player onlinePlayer : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            onlinePlayer.setLevel(amount);
            onlinePlayer.setExp(currentExpCount);
        }
    }


    public boolean isForceStarted() {
        return forceStarted;
    }
}
