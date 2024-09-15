package net.onelitefeather.cygnus.phase;

import de.icevizion.xerus.api.phase.TickDirection;
import de.icevizion.xerus.api.phase.TimedPhase;
import net.minestom.server.MinecraftServer;
import net.onelitefeather.cygnus.cloudnet.CloudGameAPI;
import net.onelitefeather.cygnus.config.GameConfig;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.view.GameView;
import org.jetbrains.annotations.NotNull;

import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 **/
public final class GamePhase extends TimedPhase {

    private final GameView gameView;
    private final Runnable startRunnable;
    private final Consumer<Void> updateWorldTime;
    private GameFinishEvent finishEvent;

    public GamePhase(@NotNull GameView gameView, @NotNull Runnable startRunnable, @NotNull Runnable endRunnable, @NotNull Consumer<Void> updateWorldTime) {
        super("GamePhase", ChronoUnit.SECONDS, 1);
        this.setCurrentTicks(GameConfig.MAX_GAME_TIME);
        this.setTickDirection(TickDirection.DOWN);
        this.setEndTicks(0);
        this.gameView = gameView;
        this.startRunnable = startRunnable;
        this.updateWorldTime = updateWorldTime;
        this.setFinishedCallback(endRunnable);
    }

    /**
     * Set's the reason why a game has ended.
     * @param finishEvent the reason to set
     */
    public void setFinishEvent(@NotNull GameFinishEvent finishEvent) {
        if (this.finishEvent != null) return;
        this.finishEvent = finishEvent;
    }

    @Override
    public void start() {
        super.start();
        CloudGameAPI.cloudGameAPI().switchInGame();
        this.startRunnable.run();
    }

    @Override
    protected void onFinish() {
        finishEvent = finishEvent == null ? new GameFinishEvent(GameFinishEvent.Reason.TIME_OVER) : finishEvent;
        MinecraftServer.getGlobalEventHandler().call(finishEvent);
    }

    /**
     * Contains the logic which should be exacted on each tick.
     */
    @Override
    public void onUpdate() {
        this.updateWorldTime.accept(null);
        this.gameView.updateView();
    }
}
