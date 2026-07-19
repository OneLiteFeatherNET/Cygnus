package net.onelitefeather.cygnus.listener;

import net.onelitefeather.cygnus.component.TeamNameComponent;
import net.theevilreaper.aves.util.Broadcaster;
import net.theevilreaper.aves.util.Players;
import net.theevilreaper.xerus.api.phase.Phase;
import net.theevilreaper.xerus.api.team.Team;
import net.theevilreaper.xerus.api.team.TeamService;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.event.SlenderReviveEvent;
import net.onelitefeather.cygnus.phase.GamePhase;
import net.onelitefeather.cygnus.phase.LobbyPhase;
import net.onelitefeather.cygnus.stamina.StaminaService;
import net.onelitefeather.cygnus.team.TeamHelper;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static net.onelitefeather.cygnus.common.config.GameConfig.SLENDER_TEAM_NAME;

/**
 * Listener that handles player disconnect events.
 * <p>
 * Depending on the active game phase (Lobby or Game), this listener pauses the
 * countdown or triggers match termination and Slender player revivals if the
 * required player thresholds are no longer met.
 *
 * @author theEvilReaper
 * @version 1.2.0
 * @since 1.0.0
 **/
public final class PlayerQuitListener implements Consumer<PlayerDisconnectEvent> {

    /**
     * Minimum remaining game ticks (seconds) required to allow a Slender player revival.
     */
    private static final int MINIMUM_SLENDER_RE_CHECK = 120;

    private final Supplier<Phase> phaseSupplier;
    private final TeamService teamService;
    private final StaminaService staminaService;
    private final int maxReviveCount;
    private final int minPlayers;
    private int currentReviveCount = 0;

    /**
     * Constructs a new PlayerQuitListener.
     *
     * @param phaseSupplier  supplier to retrieve the current active phase
     * @param teamService    service to manage teams
     * @param staminaService service to manage player stamina
     * @param minPlayers     minimum players required for the game
     */
    public PlayerQuitListener(
            Supplier<Phase> phaseSupplier,
            TeamService teamService,
            StaminaService staminaService,
            int minPlayers
    ) {
        this.phaseSupplier = phaseSupplier;
        this.teamService = teamService;
        this.staminaService = staminaService;
        this.minPlayers = minPlayers;
        this.maxReviveCount = this.minPlayers - 1;
    }

    @Override
    public void accept(PlayerDisconnectEvent event) {
        Player player = event.getPlayer();
        this.staminaService.removePlayer(player);
        switch (phaseSupplier.get()) {
            case LobbyPhase lobbyPhase -> handleLobbyQuit(player, lobbyPhase);
            case GamePhase gamePhase -> handleInGameQuit(player, gamePhase);
            default -> {
                // Nothing to do here currently
            }
        }
    }

    /**
     * Handles disconnects during the lobby phase.
     *
     * @param player     the player who disconnected
     * @param lobbyPhase the active lobby phase
     */
    private void handleLobbyQuit(Player player, LobbyPhase lobbyPhase) {
        lobbyPhase.checkPlayerRequirements();
        Broadcaster.broadcast(Messages.getLeaveMessage(player));
    }

    /**
     * Handles disconnects during the active game phase.
     *
     * @param player    the player who disconnected
     * @param gamePhase the active game phase
     */
    private void handleInGameQuit(Player player, GamePhase gamePhase) {
        if (!player.hasTag(Tags.TEAM_ID)) return;
        byte teamID = player.getTag(Tags.TEAM_ID);
        Team team = teamService.getTeams().get(teamID);

        if (team == null) return;

        team.removePlayer(player);
        String teamName = team.get(TeamNameComponent.class).teamName();

        // If the Slender player disconnected, check if we can revive a replacement
        if (SLENDER_TEAM_NAME.equals(teamName)) {
            var survivorSize = teamService.getTeams().get(TeamHelper.SURVIVOR_TEAM_ID).getCurrentSize();
            boolean canRevive = currentReviveCount < this.maxReviveCount 
                    && gamePhase.getCurrentTicks() >= MINIMUM_SLENDER_RE_CHECK 
                    && survivorSize > this.minPlayers;

            if (!canRevive) {
                // Terminate game since we cannot replace the Slender player
                gamePhase.setFinishEvent(new GameFinishEvent(GameFinishEvent.Reason.SLENDER_LEFT));
                gamePhase.finish();
                return;
            }

            // Perform Slender revival logic
            ++currentReviveCount;
            var survivorTeam = teamService.getTeams().get(TeamHelper.SURVIVOR_TEAM_ID);
            var randomPlayer = Players.getRandomPlayer(new ArrayList<>(survivorTeam.getPlayers())).get();

            survivorTeam.removePlayer(randomPlayer);
            teamService.getTeams().get(TeamHelper.SLENDER_TEAM_ID).addPlayer(randomPlayer);

            EventDispatcher.call(new SlenderReviveEvent(randomPlayer));
            return;
        }

        // If a survivor disconnected, check if there are any survivors left
        if (!team.getPlayers().isEmpty()) return;
        Team slenderTeam = teamService.getTeams().get(TeamHelper.SLENDER_TEAM_ID);
        if (slenderTeam.isEmpty()) return;
        Player slenderPlayer = slenderTeam.getPlayers().iterator().next();
        
        // Terminate game since all survivors left
        gamePhase.setFinishEvent(new GameFinishEvent(GameFinishEvent.Reason.SURVIVOR_LEFT, slenderPlayer));
        gamePhase.finish();
    }
}
