package net.onelitefeather.cygnus.listener;

import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.jumpscare.JumpScareManager;
import net.onelitefeather.cygnus.player.event.SpectatorAddEvent;
import net.onelitefeather.cygnus.team.TeamHelper;
import net.theevilreaper.xerus.api.team.Team;
import net.theevilreaper.xerus.api.team.TeamService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerDeathListenerTest extends CygnusPlayerTestBase {

    @Test
    void testDyingSurvivorFiresSpectatorAddEventForThemselves(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        TeamService teamService = TeamService.of();
        Team slenderTeam = Team.of(GameConfig.SLENDER_KEY, 1);
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);
        teamService.add(slenderTeam);
        teamService.add(survivorTeam);

        survivorTeam.addPlayer(player);
        player.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);

        PlayerDeathListener listener = new PlayerDeathListener(() -> null, teamService, new JumpScareManager());

        env.listen(SpectatorAddEvent.class)
                .followup(event -> assertEquals(player, event.getPlayer()));

        listener.accept(new PlayerDeathEvent(player, null, null));

        env.destroyInstance(instance, true);
    }
}
