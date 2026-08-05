package net.onelitefeather.cygnus.spectator;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.team.TeamHelper;
import net.theevilreaper.xerus.api.team.Team;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpectatorServiceTest extends CygnusPlayerTestBase {

    @Test
    void testJoinSetsGameModeTeamTagAndItems(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        Team spectatorTeam = Team.of(GameConfig.SPECTATOR_KEY, 5);
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);
        SpectatorService service = new SpectatorService(spectatorTeam, survivorTeam);

        service.join(player);

        assertEquals(GameMode.SPECTATOR, player.getGameMode());
        assertTrue(spectatorTeam.getPlayers().contains(player));
        assertEquals(TeamHelper.SPECTATOR_TEAM_ID, (byte) player.getTag(Tags.TEAM_ID));
        assertEquals(Material.COMPASS, player.getInventory().getItemStack(2).material());
        assertEquals(Material.OAK_DOOR, player.getInventory().getItemStack(5).material());

        env.destroyInstance(instance, true);
    }

    @Test
    void testJoinMakesPlayerInvisibleToOthers(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player target = env.createPlayer(instance);
        Player other = env.createPlayer(instance);

        for (int i = 0; i < 5; i++) env.tick();
        assertTrue(target.isViewer(other), "the target should be visible to the other player before joining spectator mode");

        Team spectatorTeam = Team.of(GameConfig.SPECTATOR_KEY, 5);
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);
        SpectatorService service = new SpectatorService(spectatorTeam, survivorTeam);

        service.join(target);

        assertFalse(target.isViewer(other), "A spectator must not be visible to other online players.");

        env.destroyInstance(instance, true);
    }

    @Test
    void testTeleportToMovesSpectatorToTargetPosition(@NotNull Env env) throws Exception {
        Instance instance = env.createFlatInstance();
        Player spectator = env.createPlayer(instance);
        Player target = env.createPlayer(instance);
        target.teleport(new Pos(15, 41, 15)).get();

        Team spectatorTeam = Team.of(GameConfig.SPECTATOR_KEY, 5);
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);
        SpectatorService service = new SpectatorService(spectatorTeam, survivorTeam);

        service.teleportTo(spectator, target).get();

        assertEquals(target.getPosition(), spectator.getPosition());

        env.destroyInstance(instance, true);
    }

    @Test
    void testIsSpectatorReflectsTeamTag(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        Team spectatorTeam = Team.of(GameConfig.SPECTATOR_KEY, 5);
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);
        SpectatorService service = new SpectatorService(spectatorTeam, survivorTeam);

        assertFalse(service.isSpectator(player));
        service.join(player);
        assertTrue(service.isSpectator(player));

        env.destroyInstance(instance, true);
    }

    @Test
    void testOpenOverviewOpensSpectatorInventory(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        Team spectatorTeam = Team.of(GameConfig.SPECTATOR_KEY, 5);
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);
        SpectatorService service = new SpectatorService(spectatorTeam, survivorTeam);

        service.openOverview(player);

        assertNotNull(player.getOpenInventory());

        env.destroyInstance(instance, true);
    }
}
