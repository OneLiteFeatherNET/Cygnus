package net.onelitefeather.cygnus.phase;

import net.theevilreaper.aves.util.functional.VoidConsumer;
import net.theevilreaper.xerus.api.phase.TimedPhase;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.EventDispatcher;
import net.onelitefeather.cygnus.common.event.GamePreLaunchEvent;
import net.onelitefeather.cygnus.view.GameView;
import org.jetbrains.annotations.NotNull;

import java.time.temporal.ChronoUnit;
import java.util.HashSet;

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 **/
@SuppressWarnings("java:S1185")
public final class WaitingPhase extends TimedPhase {

    private final GameView gameView;
    private final VoidConsumer instanceSwitch;
    private final VoidConsumer teleportLogic;

    public WaitingPhase(@NotNull GameView gameView, @NotNull VoidConsumer instanceSwitch, @NotNull VoidConsumer teleportLogic) {
        super("Waiting", ChronoUnit.SECONDS, 1);
        this.setPaused(false);
        this.setCurrentTicks(3);
        this.setEndTicks(0);
        this.gameView = gameView;
        this.instanceSwitch = instanceSwitch;
        this.teleportLogic = teleportLogic;
    }

    @Override
    public void onStart() {
        super.onStart();
        EventDispatcher.call(new GamePreLaunchEvent());
    }

    @Override
    protected void onFinish() {
        this.instanceSwitch.apply();
        this.gameView.addPlayers(new HashSet<>(MinecraftServer.getConnectionManager().getOnlinePlayers()));
    }

    @Override
    public void onUpdate() {
        if (getCurrentTicks() == 1) {
            this.teleportLogic.apply();
        }
    }
}
