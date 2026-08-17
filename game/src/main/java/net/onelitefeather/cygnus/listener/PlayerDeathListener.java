package net.onelitefeather.cygnus.listener;

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

    public PlayerDeathListener(Supplier<Phase> phaseSupplier, TeamService teamService, JumpScareManager jumpscareManager) {
        this.phaseSupplier = phaseSupplier;
        this.survivorTeam = teamService.getTeam(GameConfig.SURVIVOR_KEY)
                .orElseThrow(() -> new IllegalStateException("Survivor team not found"));
        this.slenderTeam = teamService.getTeam(GameConfig.SLENDER_KEY)
                .orElseThrow(() -> new IllegalStateException("Slender team not found"));
        this.jumpscareManager = jumpscareManager;
    }

    @Override
    public void accept(PlayerDeathEvent event) {
        Player player = event.getPlayer();

        if (survivorTeam.getPlayers().contains(player)) {
            ((CygnusPlayer) player).setDeath(true);
            this.slenderTeam.getPlayers().stream().findFirst()
                    .ifPresent(slender -> ((CygnusPlayer) slender).incrementKills());

            if (player.getInstance() == null) return;
            Pos deathPos = player.getPosition();
            DeadPlayerMannequin mannequin = DeadPlayerMannequin.sleeping(player);
            mannequin.setInstance(player.getInstance(), deathPos.add(0, 0.15, 0));
            this.jumpscareManager.register(mannequin);
        }

        event.setChatMessage(Messages.getDeathComponent(player));
        survivorTeam.removePlayer(player);
        player.removeTag(Tags.TEAM_KEY);
        EventDispatcher.call(new SpectatorAddEvent(player));
        Phase currentPhase = this.phaseSupplier.get();
        //TODO: Should be tested
        if (!(currentPhase instanceof GamePhase gamePhase) || !survivorTeam.isEmpty()) return;
        event.setChatMessage(null);
        Player slenderPlayer = this.slenderTeam.getPlayers().iterator().next();
        gamePhase.setFinishEvent(new GameFinishEvent(GameFinishEvent.Reason.ALL_SURVIVOR_DEAD, slenderPlayer));
        gamePhase.finish();
    }
}
