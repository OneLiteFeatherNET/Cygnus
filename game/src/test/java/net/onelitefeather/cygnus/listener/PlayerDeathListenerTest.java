package net.onelitefeather.cygnus.listener;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.jumpscare.JumpScareManager;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import net.onelitefeather.cygnus.player.event.SpectatorAddEvent;
 import net.theevilreaper.xerus.api.team.Team;
import net.theevilreaper.xerus.api.team.TeamService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        PlayerDeathListener listener = new PlayerDeathListener(() -> null, teamService, new JumpScareManager(), () -> {});

        env.listen(SpectatorAddEvent.class)
                .followup(event -> assertEquals(player, event.getPlayer()));

        listener.accept(new PlayerDeathEvent(player, null, null));

        env.destroyInstance(instance, true);
    }

    @Test
    void testDyingSurvivorIsMarkedAsDied(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        CygnusPlayer player = (CygnusPlayer) env.createPlayer(instance);

        TeamService teamService = TeamService.of();
        Team slenderTeam = Team.of(GameConfig.SLENDER_KEY, 1);
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);
        teamService.add(slenderTeam);
        teamService.add(survivorTeam);

        survivorTeam.addPlayer(player);
        player.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);

        PlayerDeathListener listener = new PlayerDeathListener(() -> null, teamService, new JumpScareManager(), () -> {});

        listener.accept(new PlayerDeathEvent(player, null, null));

        assertTrue(player.hasDied());

        env.destroyInstance(instance, true);
    }

    @Test
    void testDyingSurvivorIncrementsSlenderKills(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        CygnusPlayer survivor = (CygnusPlayer) env.createPlayer(instance);
        CygnusPlayer slender = (CygnusPlayer) env.createPlayer(instance);

        TeamService teamService = TeamService.of();
        Team slenderTeam = Team.of(GameConfig.SLENDER_KEY, 1);
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);
        teamService.add(slenderTeam);
        teamService.add(survivorTeam);

        survivorTeam.addPlayer(survivor);
        survivor.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);
        slenderTeam.addPlayer(slender);
        slender.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);

        PlayerDeathListener listener = new PlayerDeathListener(() -> null, teamService, new JumpScareManager(), () -> {});

        listener.accept(new PlayerDeathEvent(survivor, null, null));

        assertEquals(1, slender.getKills());

        env.destroyInstance(instance, true);
    }

    @Test
    void testDyingPlayerRespawnsWhereTheyFell(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Pos deathPos = new Pos(17.5, 64.0, -23.5, 90f, 10f);
        Player player = env.createPlayer(instance, deathPos);

        TeamService teamService = TeamService.of();
        Team slenderTeam = Team.of(GameConfig.SLENDER_KEY, 1);
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);
        teamService.add(slenderTeam);
        teamService.add(survivorTeam);
        survivorTeam.addPlayer(player);
        player.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);

        // Stand in for the map spawn: whatever the round set as the respawn point before they
        // died. Without the listener overwriting it, the spectator would be sent back here and
        // lose the spot they died in.
        Pos mapSpawn = new Pos(0, 100, 0);
        player.setRespawnPoint(mapSpawn);

        PlayerDeathListener listener = new PlayerDeathListener(() -> null, teamService, new JumpScareManager(), () -> {});
        listener.accept(new PlayerDeathEvent(player, null, null));

        assertNotEquals(mapSpawn.blockY(), player.getRespawnPoint().blockY(),
                "the map spawn must not survive the death");
        assertEquals(deathPos.blockX(), player.getRespawnPoint().blockX());
        assertEquals(deathPos.blockY(), player.getRespawnPoint().blockY());
        assertEquals(deathPos.blockZ(), player.getRespawnPoint().blockZ());

        env.destroyInstance(instance, true);
    }
}
