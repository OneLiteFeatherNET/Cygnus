package net.onelitefeather.cygnus.listener;

import de.icevizion.aves.util.Broadcaster;
import de.icevizion.xerus.api.phase.LinearPhaseSeries;
import de.icevizion.xerus.api.phase.TimedPhase;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.onelitefeather.cygnus.map.MapProvider;
import net.onelitefeather.cygnus.phase.LobbyPhase;
import net.onelitefeather.cygnus.config.GameConfig;
import net.onelitefeather.cygnus.utils.Messages;
import net.onelitefeather.cygnus.utils.Tags;
import org.jetbrains.annotations.NotNull;

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 **/
public final class PlayerSpawnListener implements Consumer<PlayerSpawnEvent> {

    private final MapProvider mapProvider;
    private final LinearPhaseSeries<TimedPhase> linearPhaseSeries;

    public PlayerSpawnListener(@NotNull MapProvider mapProvider, LinearPhaseSeries<TimedPhase> linearPhaseSeries) {
        this.mapProvider = mapProvider;
        this.linearPhaseSeries = linearPhaseSeries;
    }

    @Override
    public void accept(@NotNull PlayerSpawnEvent event) {
        var player = event.getPlayer();
        player.setDisplayName(Component.text(player.getUsername()));

        if (linearPhaseSeries.getCurrentPhase() instanceof LobbyPhase lobbyPhase ) {
            Broadcaster.broadcast(Messages.getJoinMessage(player));
            player.setDisplayName(Component.text(player.getUsername()));
            player.teleport(mapProvider.getActiveMap().getSpawn());
            player.setLevel(lobbyPhase.getCurrentTicks());
            player.setExp(lobbyPhase.getCurrentTicks() / (float) GameConfig.LOBBY_PHASE_TIME);
            lobbyPhase.checkStartCondition();
            return;
        }

        var tag = player.getTag(Tags.TEAM_ID);

        if (tag == null) {
            player.teleport(mapProvider.getGameMap().getSpawn());
        }
    }
}
