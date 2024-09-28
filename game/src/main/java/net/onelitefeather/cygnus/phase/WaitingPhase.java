package net.onelitefeather.cygnus.phase;

import de.icevizion.xerus.api.phase.TimedPhase;
import de.icevizion.xerus.api.team.Team;
import net.onelitefeather.cygnus.common.map.MapProvider;
import net.onelitefeather.cygnus.view.GameView;
import org.jetbrains.annotations.NotNull;

import java.time.temporal.ChronoUnit;
import java.util.function.IntFunction;

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 **/
public final class WaitingPhase extends TimedPhase {

    private final GameView gameView;
    private final MapProvider mapProvider;
    private final IntFunction<Team> teamGetter;

    public WaitingPhase(@NotNull GameView gameView, @NotNull MapProvider mapProvider, @NotNull IntFunction<Team> teamGetter) {
        super("Waiting", ChronoUnit.SECONDS, 1);
        this.setPaused(false);
        this.setCurrentTicks(3);
        this.setEndTicks(0);
        this.gameView = gameView;
        this.mapProvider = mapProvider;
        this.teamGetter = teamGetter;
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    protected void onFinish() {
        this.mapProvider.switchToGameMap();
        this.gameView.updateViewers(true);
    }

    @Override
    public void onUpdate() {
        if (getCurrentTicks() == 1) {
            var gameMap = this.mapProvider.getGameMap();
            var gameInstance = this.mapProvider.getGameInstance();
            this.teamGetter.apply(0).getPlayers().iterator().next().setInstance(gameInstance, gameMap.getSlenderSpawn());
            if (gameMap.getSurvivorSpawns().size() == 1) {
                var pos = gameMap.getSurvivorSpawns().iterator().next();
                this.teamGetter.apply(1).getPlayers().forEach(player -> player.setInstance(gameInstance, pos));
            }
        }
    }
}
