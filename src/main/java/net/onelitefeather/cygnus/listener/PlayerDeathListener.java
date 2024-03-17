package net.onelitefeather.cygnus.listener;

import de.icevizion.xerus.api.phase.LinearPhaseSeries;
import de.icevizion.xerus.api.phase.TimedPhase;
import de.icevizion.xerus.api.team.Team;
import de.icevizion.xerus.api.team.TeamService;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.phase.GamePhase;
import net.onelitefeather.cygnus.utils.Helper;
import net.onelitefeather.cygnus.utils.Messages;
import net.onelitefeather.cygnus.utils.Tags;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Function;

public class PlayerDeathListener implements Consumer<PlayerDeathEvent> {

    private final LinearPhaseSeries<TimedPhase> linearPhaseSeries;
    private final Team survivorTeam;
    private final Function<Void, Team> slenderGetter;

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
        if (survivorTeam.getPlayers().isEmpty() && linearPhaseSeries.getCurrentPhase() instanceof GamePhase) {
            event.setChatMessage(null);
            GamePhase gamePhase = (GamePhase) linearPhaseSeries.getCurrentPhase();
            var slenderPlayer = this.slenderGetter.apply(null).getPlayers().iterator().next();
            gamePhase.setFinishEvent(new GameFinishEvent(GameFinishEvent.Reason.ALL_SURVIVOR_DEAD, slenderPlayer));
            gamePhase.finish();
            return;
        }

    }
}
