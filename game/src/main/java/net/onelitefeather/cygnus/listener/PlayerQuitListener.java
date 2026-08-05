package net.onelitefeather.cygnus.listener;

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
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static net.onelitefeather.cygnus.common.config.GameConfig.SLENDER_KEY;
import static net.onelitefeather.cygnus.common.config.GameConfig.SURVIVOR_KEY;

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
        Optional<Team> teamOpt = teamService.getTeam(TeamHelper.keyForTeamId(teamID));

        if (teamOpt.isEmpty()) return;
        Team team = teamOpt.get();

        team.removePlayer(player);

        if (SLENDER_KEY.equals(team.key())) {
            handleSlenderQuit(gamePhase);
            return;
        }

        if (SURVIVOR_KEY.equals(team.key())) {
            handleSurvivorQuit(team, gamePhase);
        }
    }

    /**
     * Handles the Slender player disconnecting: attempts a revival, otherwise ends the match.
     *
     * @param gamePhase the active game phase
     */
    private void handleSlenderQuit(GamePhase gamePhase) {
        var survivorSize = teamService.getTeam(SURVIVOR_KEY)
                .orElseThrow(() -> new IllegalStateException("Survivor team not found"))
                .getCurrentSize();
        boolean canRevive = currentReviveCount < this.maxReviveCount
                && gamePhase.getCurrentTicks() >= MINIMUM_SLENDER_RE_CHECK
                && survivorSize > this.minPlayers;

        if (!canRevive) {
            gamePhase.setFinishEvent(new GameFinishEvent(GameFinishEvent.Reason.SLENDER_LEFT));
            gamePhase.finish();
            return;
        }

        ++currentReviveCount;
        Team survivorTeam = teamService.getTeam(SURVIVOR_KEY)
                .orElseThrow(() -> new IllegalStateException("Survivor team not found"));
        Optional<Player> randomPlayerOpt = Players.getRandomPlayer(new ArrayList<>(survivorTeam.getPlayers()));
        if (randomPlayerOpt.isEmpty()) {
            gamePhase.setFinishEvent(new GameFinishEvent(GameFinishEvent.Reason.SLENDER_LEFT));
            gamePhase.finish();
            return;
        }
        Player randomPlayer = randomPlayerOpt.get();

        survivorTeam.removePlayer(randomPlayer);
        teamService.getTeam(SLENDER_KEY)
                .orElseThrow(() -> new IllegalStateException("Slender team not found"))
                .addPlayer(randomPlayer);

        EventDispatcher.call(new SlenderReviveEvent(randomPlayer));
    }

    /**
     * Handles a survivor disconnecting: ends the match if no survivors remain.
     *
     * @param survivorTeam the survivor team, after the disconnecting player was already removed
     * @param gamePhase    the active game phase
     */
    private void handleSurvivorQuit(Team survivorTeam, GamePhase gamePhase) {
        if (!survivorTeam.getPlayers().isEmpty()) return;
        Team slenderTeam = teamService.getTeam(SLENDER_KEY)
                .orElseThrow(() -> new IllegalStateException("Slender team not found"));
        if (slenderTeam.isEmpty()) return;
        Player slenderPlayer = slenderTeam.getPlayers().iterator().next();

        gamePhase.setFinishEvent(new GameFinishEvent(GameFinishEvent.Reason.SURVIVOR_LEFT, slenderPlayer));
        gamePhase.finish();
    }
}
