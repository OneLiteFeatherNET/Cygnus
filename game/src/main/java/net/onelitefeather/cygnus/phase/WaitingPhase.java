package net.onelitefeather.cygnus.phase;

import net.theevilreaper.aves.util.functional.VoidConsumer;
import net.theevilreaper.xerus.api.phase.TimedPhase;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.EventDispatcher;
import net.onelitefeather.cygnus.attribute.AttributeHelper;
import net.onelitefeather.cygnus.camera.WakeUpTransition;
import net.onelitefeather.cygnus.common.event.GamePreLaunchEvent;
import net.onelitefeather.cygnus.map.event.GamePrepareEvent;
import net.minestom.server.instance.Instance;
import net.onelitefeather.cygnus.view.GameView;

import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.function.Supplier;

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
    private final Supplier<Instance> gameInstance;
    private final WakeUpTransition wakeUpTransition;

    public WaitingPhase(GameView gameView, VoidConsumer instanceSwitch, VoidConsumer teleportLogic,
                        Supplier<Instance> gameInstance, WakeUpTransition wakeUpTransition) {
        super("Waiting", ChronoUnit.SECONDS, 1);
        this.setPaused(false);
        this.setCurrentTicks(3);
        this.setEndTicks(0);
        this.gameView = gameView;
        this.instanceSwitch = instanceSwitch;
        this.teleportLogic = teleportLogic;
        this.gameInstance = gameInstance;
        this.wakeUpTransition = wakeUpTransition;
    }

    @Override
    public void onStart() {
        super.onStart();
        // The players arrive on the game map one by one, so whoever lands first could otherwise walk
        // off before the round starts. Looking around stays allowed.
        var players = MinecraftServer.getConnectionManager().getOnlinePlayers();
        players.forEach(AttributeHelper::freeze);
        // The active instance is still the lobby here, so it is read before the switch. The eyes close in the lobby
        // and stay closed until each player leaves it for the game.
        this.wakeUpTransition.close(players, this.gameInstance.get());
        this.instanceSwitch.apply();
    }

    @Override
    protected void onFinish() {
        MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(AttributeHelper::unfreeze);
        this.gameView.addPlayers(new HashSet<>(MinecraftServer.getConnectionManager().getOnlinePlayers()));
    }

    @Override
    public void onUpdate() {
        if (getCurrentTicks() == 1) {
            // The roles are handed out in the same tick the players leave the lobby, so nobody can
            // read the slender off the lobby (hidden player, name tag colour) before the round starts
            EventDispatcher.call(new GamePrepareEvent());
            // Right before the end, so the page counts are based on the players that actually start
            EventDispatcher.call(new GamePreLaunchEvent());
            this.teleportLogic.apply();
            // The players may still be on their way into the game instance (the switch completes later), so the
            // transition names the target and spawns the spider once each player has arrived there
            this.wakeUpTransition.open(MinecraftServer.getConnectionManager().getOnlinePlayers(), this.gameInstance.get());
        }
    }
}
