package net.onelitefeather.cygnus.phase;

import de.icevizion.aves.util.functional.VoidConsumer;
import de.icevizion.xerus.api.phase.TickDirection;
import de.icevizion.xerus.api.phase.TimedPhase;
import net.infumia.agones4j.Agones;
import net.minestom.server.MinecraftServer;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.view.GameView;
import org.jetbrains.annotations.NotNull;

import java.time.temporal.ChronoUnit;
import java.util.HashSet;

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 **/
public final class GamePhase extends TimedPhase {

    private final GameView gameView;
    private final Runnable startRunnable;
    private final VoidConsumer updateWorldTime;
    private final Agones agones;
    private GameFinishEvent finishEvent;

    /**
     * Creates a new instance from the {@link GamePhase}.
     *
     * @param gameView        the view to update
     * @param startRunnable   the runnable to execute on start
     * @param endRunnable     the runnable to execute on end
     * @param updateWorldTime the consumer to update the world time
     */
    public GamePhase(
            @NotNull GameView gameView,
            @NotNull Runnable startRunnable,
            @NotNull Runnable endRunnable,
            @NotNull VoidConsumer updateWorldTime,
            int gameTime, Agones agones
    ) {
        super("GamePhase", ChronoUnit.SECONDS, 1);
        this.agones = agones;
        this.setCurrentTicks(gameTime);
        this.setTickDirection(TickDirection.DOWN);
        this.setEndTicks(0);
        this.gameView = gameView;
        this.startRunnable = startRunnable;
        this.updateWorldTime = updateWorldTime;
        this.setFinishedCallback(endRunnable);
    }

    /**
     * Set's the reason why a game has ended.
     *
     * @param finishEvent the reason to set
     */
    public void setFinishEvent(@NotNull GameFinishEvent finishEvent) {
        if (this.finishEvent != null) return;
        this.finishEvent = finishEvent;
    }

    @Override
    public void onStart() {
        super.onStart();
        this.startRunnable.run();
        this.agones.setLabel("status", "ingame");
    }

    @Override
    protected void onFinish() {
        finishEvent = finishEvent == null ? new GameFinishEvent(GameFinishEvent.Reason.TIME_OVER) : finishEvent;
        MinecraftServer.getGlobalEventHandler().call(finishEvent);
        this.gameView.removePlayers(new HashSet<>(MinecraftServer.getConnectionManager().getOnlinePlayers()));
    }

    /**
     * Contains the logic which should be exacted on each tick.
     */
    @Override
    public void onUpdate() {
        this.updateWorldTime.apply();
        this.gameView.updateView();
    }
}
