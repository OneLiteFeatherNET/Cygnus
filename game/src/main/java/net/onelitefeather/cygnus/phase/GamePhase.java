package net.onelitefeather.cygnus.phase;

import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.onelitefeather.cygnus.event.GameStartEvent;
import net.onelitefeather.cygnus.jumpscare.JumpScareManager;
import net.onelitefeather.cygnus.view.event.ViewUpdateEvent;
import net.theevilreaper.xerus.api.phase.TickDirection;
import net.theevilreaper.xerus.api.phase.TimedPhase;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.player.PlayerTickEvent;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.listener.player.CygnusPlayerTickListener;
import net.onelitefeather.cygnus.hud.PageCountHudComponent;
import net.onelitefeather.cygnus.hud.PageTimerHudComponent;
import org.jetbrains.annotations.Nullable;

import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 **/
public final class GamePhase extends TimedPhase {

    private final PageTimerHudComponent pageTimerHudComponent;
    private final PageCountHudComponent pageCountHudComponent;
    private final JumpScareManager jumpscareManager;
    private @Nullable GameFinishEvent finishEvent;

    /**
     * Creates a new instance from the {@link GamePhase}.
     *
     * @param pageTimerHudComponent the HUD component to update
     * @param pageCountHudComponent the HUD component to update, shares pageTimerHudComponent's visibility lifecycle
     * @param endRunnable      the runnable to execute on end
     * @param gameTime         the game time
     * @param jumpscareManager the jumpscare manager instance
     */
    public GamePhase(
            PageTimerHudComponent pageTimerHudComponent,
            PageCountHudComponent pageCountHudComponent,
            Runnable endRunnable,
            int gameTime,
            JumpScareManager jumpscareManager
    ) {
        super("GamePhase", ChronoUnit.SECONDS, 1);
        this.setCurrentTicks(gameTime);
        this.setTickDirection(TickDirection.DOWN);
        this.setEndTicks(0);
        this.pageTimerHudComponent = pageTimerHudComponent;
        this.pageCountHudComponent = pageCountHudComponent;
        this.jumpscareManager = jumpscareManager;
        this.setFinishedCallback(endRunnable);
    }

    /**
     * Set's the reason why a game has ended.
     *
     * @param finishEvent the reason to set
     */
    public void setFinishEvent(GameFinishEvent finishEvent) {
        if (this.finishEvent != null) return;
        this.finishEvent = finishEvent;
    }

    @Override
    public void onStart() {
        super.onStart();
        addListener(PlayerTickEvent.class, new CygnusPlayerTickListener(this.jumpscareManager));
        EventDispatcher.call(new GameStartEvent());
    }

    @Override
    protected void onFinish() {
        finishEvent = finishEvent == null ? new GameFinishEvent(GameFinishEvent.Reason.TIME_OVER) : finishEvent;
        MinecraftServer.getGlobalEventHandler().call(finishEvent);
        Set<Player> onlinePlayers = new HashSet<>(MinecraftServer.getConnectionManager().getOnlinePlayers());
        this.pageTimerHudComponent.removePlayers(onlinePlayers);
        this.pageCountHudComponent.removePlayers(onlinePlayers);
    }

    /**
     * Contains the logic which should be exacted on each tick.
     */
    @Override
    public void onUpdate() {
        EventDispatcher.call(new ViewUpdateEvent(getCurrentTicks()));
    }
}

