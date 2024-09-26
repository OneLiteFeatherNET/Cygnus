package net.onelitefeather.cygnus.listener;

import de.icevizion.xerus.api.phase.LinearPhaseSeries;
import de.icevizion.xerus.api.phase.Phase;
import de.icevizion.xerus.api.phase.TimedPhase;
import de.icevizion.xerus.api.team.Team;
import de.icevizion.xerus.api.team.TeamService;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.util.Helper;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.phase.GamePhase;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.IntFunction;

public class PlayerDeathListener implements Consumer<PlayerDeathEvent> {

    private final LinearPhaseSeries<TimedPhase> linearPhaseSeries;
    private final Team survivorTeam;
    private final IntFunction<Team> slenderGetter;

    public PlayerDeathListener(@NotNull LinearPhaseSeries<TimedPhase> linearPhaseSeries, @NotNull TeamService<Team> teamService) {
        this.linearPhaseSeries = linearPhaseSeries;
        this.survivorTeam = teamService.getTeams().get(Helper.SURVIVOR_ID);
        this.slenderGetter = ignore -> teamService.getTeams().get(Helper.SLENDER_ID);
    }

    @Override
    public void accept(PlayerDeathEvent event) {
        event.setChatMessage(Messages.getDeathComponent(event.getPlayer()));
        survivorTeam.removePlayer(event.getPlayer());
        event.getPlayer().removeTag(Tags.TEAM_ID);
        event.getPlayer().kick(Messages.withMini("<red>Thanks for playing it. <3"));
        Phase currentPhase = linearPhaseSeries.getCurrentPhase();
        //TODO: Should be tested
        if ((!(currentPhase instanceof GamePhase gamePhase)) || !survivorTeam.getPlayers().isEmpty()) return;
        event.setChatMessage(null);
        var slenderPlayer = this.slenderGetter.apply(0).getPlayers().iterator().next();
        gamePhase.setFinishEvent(new GameFinishEvent(GameFinishEvent.Reason.ALL_SURVIVOR_DEAD, slenderPlayer));
        gamePhase.finish();
    }
}
