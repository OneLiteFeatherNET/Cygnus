package net.onelitefeather.cygnus.listener;

import de.icevizion.aves.util.Broadcaster;
import de.icevizion.aves.util.Players;
import de.icevizion.xerus.api.phase.LinearPhaseSeries;
import de.icevizion.xerus.api.phase.TimedPhase;
import de.icevizion.xerus.api.team.Team;
import de.icevizion.xerus.api.team.TeamService;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.event.SlenderReviveEvent;
import net.onelitefeather.cygnus.phase.GamePhase;
import net.onelitefeather.cygnus.phase.LobbyPhase;
import net.onelitefeather.cygnus.utils.Helper;
import net.onelitefeather.cygnus.utils.Messages;
import net.onelitefeather.cygnus.utils.Tags;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.function.Consumer;

import static net.onelitefeather.cygnus.config.GameConfig.*;

public final class PlayerQuitListener implements Consumer<PlayerDisconnectEvent> {

    private final LinearPhaseSeries<TimedPhase> phaseLinearPhaseSeries;
    private final TeamService<Team> teamService;
    private final Runnable stopSlenderBar;
    private int currentReviveCount = 1;

    public PlayerQuitListener(@NotNull LinearPhaseSeries<TimedPhase> phaseLinearPhaseSeries, @NotNull TeamService<Team> teamService, @NotNull Runnable stopSlenderBar) {
        this.phaseLinearPhaseSeries = phaseLinearPhaseSeries;
        this.teamService = teamService;
        this.stopSlenderBar = stopSlenderBar;
    }

    @Override
    public void accept(@NotNull PlayerDisconnectEvent event) {
        if (phaseLinearPhaseSeries.getCurrentPhase() instanceof LobbyPhase lobbyPhase) {
            lobbyPhase.checkStopCondition();
            handleLobbyQuit(event.getPlayer(), lobbyPhase);
            return;
        }

        if (phaseLinearPhaseSeries.getCurrentPhase() instanceof GamePhase gamePhase) {
            handleInGameQuit(event.getPlayer(), gamePhase);
        }
    }

    private void handleLobbyQuit(@NotNull Player player, @NotNull LobbyPhase lobbyPhase) {
        lobbyPhase.checkPlayerRequirements();
        Broadcaster.broadcast(Messages.getLeaveMessage(player));
    }

    private void handleInGameQuit(@NotNull Player player, @NotNull GamePhase gamePhase) {
        if (!player.hasTag(Tags.TEAM_ID)) return;
        byte teamID = player.getTag(Tags.TEAM_ID);
        Team team = teamService.getTeams().get(teamID);

        if (team == null) return;

        team.removePlayer(player);
        if (SLENDER_TEAM_NAME.equals(team.getName())) {
            stopSlenderBar.run();
            var survivorSize = teamService.getTeams().get(Helper.SURVIVOR_ID).getCurrentSize();
            boolean canRevive = currentReviveCount < MAX_REVIVE_COUNT_SLENDER && gamePhase.getCurrentTicks() <= MINIMUM_SLENDER_RE_CHECK && survivorSize > MIN_PLAYERS;

            if (!canRevive) {
                gamePhase.setFinishEvent(new GameFinishEvent(GameFinishEvent.Reason.SLENDER_LEFT));
                gamePhase.finish();
                return;
            }
            ++currentReviveCount;
            var survivorTeam = teamService.getTeams().get(Helper.SURVIVOR_ID);
            var randomPlayer = Players.getRandomPlayer(new ArrayList<>(survivorTeam.getPlayers())).get();

            survivorTeam.removePlayer(randomPlayer);

            EventDispatcher.call(new SlenderReviveEvent(randomPlayer));
            return;
        }

        if (!team.getPlayers().isEmpty()) return;
        var slenderPlayer = teamService.getTeams().get(Helper.SLENDER_ID).getPlayers().iterator().next();
        gamePhase.setFinishEvent(new GameFinishEvent(GameFinishEvent.Reason.SURVIVOR_LEFT, slenderPlayer));
        gamePhase.finish();
    }
}
