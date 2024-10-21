package net.onelitefeather.cygnus.listener;

import de.icevizion.xerus.api.phase.Phase;
import de.icevizion.xerus.api.team.Team;
import de.icevizion.xerus.api.team.TeamService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.util.Helper;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.phase.GamePhase;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public final class PlayerDeathListener implements Consumer<PlayerDeathEvent> {

    private final Component kickMessage = Component.text("Thanks for playing it. <3", NamedTextColor.RED);
    private final Supplier<Phase> phaseSupplier;
    private final Team survivorTeam;
    private final IntFunction<Team> slenderGetter;

    public PlayerDeathListener(@NotNull Supplier<Phase> phaseSupplier, @NotNull TeamService<Team> teamService) {
        this.phaseSupplier = phaseSupplier;
        this.survivorTeam = teamService.getTeams().get(Helper.SURVIVOR_ID);
        this.slenderGetter = ignore -> teamService.getTeams().getFirst();
    }

    @Override
    public void accept(@NotNull PlayerDeathEvent event) {
        Player player = event.getPlayer();
        event.setChatMessage(Messages.getDeathComponent(player));
        survivorTeam.removePlayer(player);
        player.removeTag(Tags.TEAM_ID);
        player.kick(kickMessage);
        Phase currentPhase = this.phaseSupplier.get();
        //TODO: Should be tested
        if ((!(currentPhase instanceof GamePhase gamePhase && !survivorTeam.getPlayers().isEmpty()))) return;
        event.setChatMessage(null);
        var slenderPlayer = this.slenderGetter.apply(0).getPlayers().iterator().next();
        gamePhase.setFinishEvent(new GameFinishEvent(GameFinishEvent.Reason.ALL_SURVIVOR_DEAD, slenderPlayer));
        gamePhase.finish();
    }
}
