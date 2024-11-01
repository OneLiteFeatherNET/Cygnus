package net.onelitefeather.cygnus.listener;

import de.icevizion.aves.util.Broadcaster;
import de.icevizion.aves.util.Players;
import de.icevizion.xerus.api.phase.Phase;
import de.icevizion.xerus.api.team.Team;
import de.icevizion.xerus.api.team.TeamService;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.onelitefeather.agones.AgonesAPI;
import net.onelitefeather.cygnus.common.CygnusFlag;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.util.Helper;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.event.SlenderReviveEvent;
import net.onelitefeather.cygnus.phase.GamePhase;
import net.onelitefeather.cygnus.phase.LobbyPhase;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static net.onelitefeather.cygnus.common.config.GameConfig.SLENDER_TEAM_NAME;

public final class PlayerQuitListener implements Consumer<PlayerDisconnectEvent> {

    private static final int MINIMUM_SLENDER_RE_CHECK = 120;
    private final Supplier<Phase> phaseSupplier;
    private final TeamService<Team> teamService;
    private final Runnable stopSlenderBar;
    private final int maxReviveCount;
    private final int minPlayers;
    private int currentReviveCount = 1;


    public PlayerQuitListener(
            @NotNull Supplier<Phase> phaseSupplier,
            @NotNull TeamService<Team> teamService,
            @NotNull Runnable stopSlenderBar,
            int minPlayers
    ) {
        this.phaseSupplier = phaseSupplier;
        this.teamService = teamService;
        this.stopSlenderBar = stopSlenderBar;
        this.minPlayers = minPlayers;
        this.maxReviveCount = this.minPlayers - 1;
    }

    @Override
    public void accept(@NotNull PlayerDisconnectEvent event) {
        Phase phase = phaseSupplier.get();
        if (phase instanceof LobbyPhase lobbyPhase) {
            if (CygnusFlag.AGONES_SUPPORT.isPresent()) {
                AgonesAPI.instance().decreaseCurrentPlayerCount();
            }
            lobbyPhase.checkStopCondition();
            handleLobbyQuit(event.getPlayer(), lobbyPhase);
            return;
        }

        if (phase instanceof GamePhase gamePhase) {
            if (CygnusFlag.AGONES_SUPPORT.isPresent()) {
                AgonesAPI.instance().decreaseCurrentPlayerCount();
            }
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
        String teamName = PlainTextComponentSerializer.plainText().serialize(team.getName());
        if (SLENDER_TEAM_NAME.equals(teamName)) {
            stopSlenderBar.run();
            var survivorSize = teamService.getTeams().get(Helper.SURVIVOR_ID).getCurrentSize();
            boolean canRevive = currentReviveCount < this.maxReviveCount && gamePhase.getCurrentTicks() <= MINIMUM_SLENDER_RE_CHECK && survivorSize > this.minPlayers;

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
        Team slenderTeam = teamService.getTeams().getFirst();
        if (slenderTeam.isEmpty()) return;
        Player slenderPlayer = slenderTeam.getPlayers().iterator().next();
        gamePhase.setFinishEvent(new GameFinishEvent(GameFinishEvent.Reason.SURVIVOR_LEFT, slenderPlayer));
        gamePhase.finish();
    }
}
