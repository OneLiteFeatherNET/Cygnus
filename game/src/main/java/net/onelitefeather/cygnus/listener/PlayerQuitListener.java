package net.onelitefeather.cygnus.listener;

import net.kyori.adventure.key.Key;
import net.theevilreaper.aves.util.Broadcaster;
import net.theevilreaper.aves.util.functional.VoidConsumer;
import net.theevilreaper.xerus.api.phase.Phase;
import net.theevilreaper.xerus.api.team.Team;
import net.theevilreaper.xerus.api.team.TeamService;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.listener.game.SlenderTakeover;
import net.onelitefeather.cygnus.phase.GamePhase;
import net.onelitefeather.cygnus.phase.LobbyPhase;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import net.onelitefeather.cygnus.spectator.LateJoinSlots;
import net.onelitefeather.cygnus.stamina.StaminaService;

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
 * required player thresholds are no longer met. A late joiner's place in the
 * running round is released on every disconnect, also for a player who was
 * turned away before the spawn.
 *
 * @author theEvilReaper
 * @version 1.2.0
 * @since 1.0.0
 **/
public final class PlayerQuitListener implements Consumer<PlayerDisconnectEvent> {

    private final Supplier<Phase> phaseSupplier;
    private final TeamService teamService;
    private final StaminaService staminaService;
    private final VoidConsumer inventoryUpdater;
    private final SlenderTakeover takeover;
    private final LateJoinSlots lateJoinSlots;

    /**
     * Constructs a new PlayerQuitListener.
     *
     * @param phaseSupplier  supplier to retrieve the current active phase
     * @param teamService    service to manage teams
     * @param staminaService   service to manage player stamina
     * @param inventoryUpdater refreshes the spectators' inventories
     * @param takeover         hands the slender's role to a survivor once he left
     * @param lateJoinSlots    the late-join places, the place of the leaving player is released
     * @since 2.21.0
     */
    public PlayerQuitListener(
            Supplier<Phase> phaseSupplier,
            TeamService teamService,
            StaminaService staminaService,
            VoidConsumer inventoryUpdater,
            SlenderTakeover takeover,
            LateJoinSlots lateJoinSlots
    ) {
        this.phaseSupplier = phaseSupplier;
        this.teamService = teamService;
        this.staminaService = staminaService;
        this.inventoryUpdater = inventoryUpdater;
        this.takeover = takeover;
        this.lateJoinSlots = lateJoinSlots;
    }

    @Override
    public void accept(PlayerDisconnectEvent event) {
        Player player = event.getPlayer();
        this.staminaService.removePlayer(player);
        // Before the early return below: a refused joiner must not keep a place either.
        this.lateJoinSlots.release(player.getUuid());
        // Turned away before the spawn: never announced as joined, never counted as a participant.
        if (player instanceof CygnusPlayer cygnusPlayer && cygnusPlayer.isRejectedBeforeJoin()) {
            return;
        }
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
        if (!player.hasTag(Tags.TEAM_KEY)) return;
        Key teamKey = player.getTag(Tags.TEAM_KEY);
        Optional<Team> teamOpt = teamService.getTeam(teamKey);

        if (teamOpt.isEmpty()) return;
        Team team = teamOpt.get();

        team.removePlayer(player);

        if (SLENDER_KEY.equals(team.key())) {
            handleSlenderQuit(player, gamePhase);
            return;
        }

        if (SURVIVOR_KEY.equals(team.key())) {
            handleSurvivorQuit(player, team, gamePhase);
        }
    }

    /**
     * Handles the Slender player disconnecting: announces the leave, then a survivor takes over after
     * a countdown, otherwise the match ends.
     *
     * @param player    the slender who disconnected
     * @param gamePhase the active game phase
     */
    private void handleSlenderQuit(Player player, GamePhase gamePhase) {
        // Before the takeover, which may end the round right away.
        Broadcaster.broadcast(Messages.getSlenderLeaveMessage(player));
        this.takeover.begin(gamePhase);
    }

    /**
     * Handles a survivor disconnecting: announces the leave and ends the match if no survivors remain.
     *
     * @param player       the survivor who disconnected
     * @param survivorTeam the survivor team, after the disconnecting player was already removed
     * @param gamePhase    the active game phase
     */
    private void handleSurvivorQuit(Player player, Team survivorTeam, GamePhase gamePhase) {
        // Before the finish below, so the leave reads ahead of the win message it causes.
        Broadcaster.broadcast(Messages.getLeaveMessage(player));
        this.inventoryUpdater.apply();
        if (!survivorTeam.getPlayers().isEmpty()) return;
        Team slenderTeam = teamService.getTeam(SLENDER_KEY)
                .orElseThrow(() -> new IllegalStateException("Slender team not found"));
        if (slenderTeam.isEmpty()) return;
        Player slenderPlayer = slenderTeam.getPlayers().iterator().next();

        gamePhase.setFinishEvent(new GameFinishEvent(GameFinishEvent.Reason.SURVIVOR_LEFT, slenderPlayer));
        gamePhase.finish();
    }
}
