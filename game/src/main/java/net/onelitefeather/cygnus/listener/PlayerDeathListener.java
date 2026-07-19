package net.onelitefeather.cygnus.listener;

import net.theevilreaper.xerus.api.phase.Phase;
import net.theevilreaper.xerus.api.team.Team;
import net.theevilreaper.xerus.api.team.TeamService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.phase.GamePhase;
import net.onelitefeather.cygnus.team.TeamHelper;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class PlayerDeathListener implements Consumer<PlayerDeathEvent> {

    private final Component kickMessage = Component.text("Thanks for playing it. <3", NamedTextColor.RED);
    private final Supplier<Phase> phaseSupplier;
    private final Team survivorTeam;
    private final Team slenderTeam;

    public PlayerDeathListener(Supplier<Phase> phaseSupplier, TeamService teamService) {
        this.phaseSupplier = phaseSupplier;
        this.survivorTeam = teamService.getTeams().get(TeamHelper.SURVIVOR_TEAM_ID);
        this.slenderTeam = teamService.getTeams().get(TeamHelper.SLENDER_TEAM_ID);
    }

    @Override
    public void accept(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        event.setChatMessage(Messages.getDeathComponent(player));
        survivorTeam.removePlayer(player);
        player.removeTag(Tags.TEAM_ID);
        player.kick(kickMessage);
        Phase currentPhase = this.phaseSupplier.get();
        //TODO: Should be tested
        if (!(currentPhase instanceof GamePhase gamePhase) || !survivorTeam.isEmpty()) return;
        event.setChatMessage(null);
        Player slenderPlayer = this.slenderTeam.getPlayers().iterator().next();
        gamePhase.setFinishEvent(new GameFinishEvent(GameFinishEvent.Reason.ALL_SURVIVOR_DEAD, slenderPlayer));
        gamePhase.finish();
    }
}
