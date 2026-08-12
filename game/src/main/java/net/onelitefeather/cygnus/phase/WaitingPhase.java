package net.onelitefeather.cygnus.phase;

import net.theevilreaper.aves.util.functional.VoidConsumer;
import net.theevilreaper.xerus.api.phase.TimedPhase;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.common.event.GamePreLaunchEvent;
import net.onelitefeather.cygnus.hud.PageCountHudComponent;
import net.onelitefeather.cygnus.hud.PageTimerHudComponent;

import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 **/
@SuppressWarnings("java:S1185")
public final class WaitingPhase extends TimedPhase {

    private final PageTimerHudComponent pageTimerHudComponent;
    private final PageCountHudComponent pageCountHudComponent;
    private final VoidConsumer instanceSwitch;
    private final VoidConsumer teleportLogic;

    /**
     * Creates a new instance from the {@link WaitingPhase}.
     *
     * @param pageTimerHudComponent the HUD component to add players to on finish
     * @param pageCountHudComponent the HUD component to add players to on finish, shares pageTimerHudComponent's visibility lifecycle
     * @param instanceSwitch        the logic to run on start to switch to the game instance
     * @param teleportLogic         the logic to run to teleport players to their spawns
     */
    public WaitingPhase(
            PageTimerHudComponent pageTimerHudComponent,
            PageCountHudComponent pageCountHudComponent,
            VoidConsumer instanceSwitch,
            VoidConsumer teleportLogic
    ) {
        super("Waiting", ChronoUnit.SECONDS, 1);
        this.setPaused(false);
        this.setCurrentTicks(3);
        this.setEndTicks(0);
        this.pageTimerHudComponent = pageTimerHudComponent;
        this.pageCountHudComponent = pageCountHudComponent;
        this.instanceSwitch = instanceSwitch;
        this.teleportLogic = teleportLogic;
    }

    @Override
    public void onStart() {
        super.onStart();
        EventDispatcher.call(new GamePreLaunchEvent());
        this.instanceSwitch.apply();
    }

    @Override
    protected void onFinish() {
        Set<Player> onlinePlayers = new HashSet<>(MinecraftServer.getConnectionManager().getOnlinePlayers());
        this.pageTimerHudComponent.addPlayers(onlinePlayers);
        this.pageCountHudComponent.addPlayers(onlinePlayers);
    }

    @Override
    public void onUpdate() {
        if (getCurrentTicks() == 1) {
            this.teleportLogic.apply();
        }
    }
}
