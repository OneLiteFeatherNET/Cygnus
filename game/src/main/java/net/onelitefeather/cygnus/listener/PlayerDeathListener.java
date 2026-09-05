package net.onelitefeather.cygnus.listener;

import net.theevilreaper.aves.util.functional.VoidConsumer;
import net.theevilreaper.xerus.api.phase.Phase;
import net.theevilreaper.xerus.api.team.Team;
import net.theevilreaper.xerus.api.team.TeamService;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.entity.DeadPlayerMannequin;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.jumpscare.JumpScareManager;
import net.onelitefeather.cygnus.phase.GamePhase;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import net.onelitefeather.cygnus.player.event.SpectatorAddEvent;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class PlayerDeathListener implements Consumer<PlayerDeathEvent> {

    private final Supplier<Phase> phaseSupplier;
    private final Team survivorTeam;
    private final Team slenderTeam;
    private final JumpScareManager jumpscareManager;
    private final VoidConsumer inventoryUpdater;

    public PlayerDeathListener(Supplier<Phase> phaseSupplier, TeamService teamService, JumpScareManager jumpscareManager, VoidConsumer inventoryUpdater) {
        this.phaseSupplier = phaseSupplier;
        this.survivorTeam = teamService.getTeam(GameConfig.SURVIVOR_KEY)
                .orElseThrow(() -> new IllegalStateException("Survivor team not found"));
        this.slenderTeam = teamService.getTeam(GameConfig.SLENDER_KEY)
                .orElseThrow(() -> new IllegalStateException("Slender team not found"));
        this.jumpscareManager = jumpscareManager;
        this.inventoryUpdater = inventoryUpdater;
    }

    @Override
    public void accept(PlayerDeathEvent event) {
        Player player = event.getPlayer();

        // Read before anything moves them, and used for both the mannequin and the respawn: a
        // dead player is respawned at their respawn point, so without this a spectator would blink
        // back to the map spawn and lose the spot they died in - the one place they have any reason
        // to look at.
        Pos deathPos = player.getPosition();
        player.setRespawnPoint(deathPos);

        if (survivorTeam.getPlayers().contains(player)) {
            ((CygnusPlayer) player).setDeath(true);
            this.slenderTeam.getPlayers().stream().findFirst()
                    .ifPresent(slender -> ((CygnusPlayer) slender).incrementKills());

            if (player.getInstance() == null) return;
            DeadPlayerMannequin mannequin = DeadPlayerMannequin.sleeping(player);
            mannequin.setInstance(player.getInstance(), deathPos.add(0, 0.15, 0));
            this.jumpscareManager.register(mannequin);
        }

        event.setChatMessage(Messages.getDeathComponent(player));
        survivorTeam.removePlayer(player);
        player.removeTag(Tags.TEAM_KEY);
        EventDispatcher.call(new SpectatorAddEvent(player));
        this.inventoryUpdater.apply();
        Phase currentPhase = this.phaseSupplier.get();
        //TODO: Should be tested
        if (!(currentPhase instanceof GamePhase gamePhase) || !survivorTeam.isEmpty()) return;
        event.setChatMessage(null);
        Player slenderPlayer = this.slenderTeam.getPlayers().iterator().next();
        gamePhase.setFinishEvent(new GameFinishEvent(GameFinishEvent.Reason.ALL_SURVIVOR_DEAD, slenderPlayer));
        gamePhase.finish();
    }
}
