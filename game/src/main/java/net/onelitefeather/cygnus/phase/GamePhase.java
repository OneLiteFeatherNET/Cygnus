package net.onelitefeather.cygnus.phase;

import agones.dev.sdk.Sdk;
import de.icevizion.aves.util.functional.VoidConsumer;
import de.icevizion.xerus.api.phase.TickDirection;
import de.icevizion.xerus.api.phase.TimedPhase;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.player.PlayerTickEvent;
import net.onelitefeather.agones.AgonesAPI;
import net.onelitefeather.cygnus.common.CygnusFlag;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.listener.player.CygnusPlayerTickListener;
import net.onelitefeather.cygnus.view.GameView;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 **/
public final class GamePhase extends TimedPhase {

    private static final Logger LOGGER = LoggerFactory.getLogger(GamePhase.class);
    private final GameView gameView;
    private final Runnable startRunnable;
    private final VoidConsumer updateWorldTime;
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
            int gameTime
    ) {
        super("GamePhase", ChronoUnit.SECONDS, 1);
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
        addListener(PlayerTickEvent.class, new CygnusPlayerTickListener());
        if (CygnusFlag.AGONES_SUPPORT.isPresent()) {
            AgonesAPI.instance().allocateFuture()
                    .exceptionallyCompose(this::allocateFuture)
                    .exceptionallyCompose(this::allocateFuture)
                    .exceptionallyCompose(this::allocateFuture)
                    .thenRun(() -> LOGGER.info("GameServer allocated"));
        }
        this.startRunnable.run();
    }

    private CompletableFuture<Sdk.Empty> allocateFuture(Throwable throwable) {
        LOGGER.error("Failed to allocate the GameServer", throwable);
        return AgonesAPI.instance().allocateFuture();
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
