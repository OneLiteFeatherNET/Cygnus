package net.onelitefeather.cygnus.listener.game;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.component.TeamNameComponent;
import net.onelitefeather.cygnus.event.SlenderReviveEvent;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import net.onelitefeather.cygnus.stamina.SlenderBarHelper;
import net.onelitefeather.cygnus.stamina.StaminaService;
import net.onelitefeather.cygnus.team.TeamHelper;
import net.onelitefeather.cygnus.utils.ScoreboardDisplay;
import net.onelitefeather.cygnus.visibility.VisibilityRules;
import net.theevilreaper.xerus.api.ColorData;
import net.theevilreaper.xerus.api.component.team.ColorComponent;
import net.theevilreaper.xerus.api.team.Team;
import net.theevilreaper.xerus.api.team.TeamService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test verifying that {@link SlenderReviveListener} works correctly.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.6.7
 */
class SlenderReviveIntegrationTest extends CygnusPlayerTestBase {

    @Test
    void testEventDispatchedThroughGlobalEventHandler(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        CygnusPlayer player = (CygnusPlayer) env.createPlayer(instance);
        StaminaService staminaService = new StaminaService();
        TeamService teamService = TeamService.of();

        AtomicReference<GameMap> mapSupplierRef = new AtomicReference<>(null);
        SlenderReviveListener listener = new SlenderReviveListener(mapSupplierRef::get, staminaService, teamService);

        env.process().eventHandler().addListener(SlenderReviveEvent.class, listener);

        Pos targetSpawn = new Pos(25, 70, 25);
        GameMap gameMap = new GameMap("Map", Pos.ZERO, targetSpawn, Set.of(), Set.of(new Pos(2, 64, 2)), List.of(), null);
        mapSupplierRef.set(gameMap);

        env.process().eventHandler().call(new SlenderReviveEvent(player));

        assertEquals(GameConfig.SLENDER_KEY, player.getTag(Tags.TEAM_KEY));
        assertEquals(targetSpawn, player.getPosition());
        assertNotNull(staminaService.getSlenderBar());

        env.destroyInstance(instance, true);
    }

    @Test
    void testRevivedSlenderIsHiddenFromEveryoneElse(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        CygnusPlayer player = (CygnusPlayer) env.createPlayer(instance);
        CygnusPlayer survivor = (CygnusPlayer) env.createPlayer(instance);
        survivor.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);
        TeamService teamService = TeamService.of();

        for (int i = 0; i < 5; i++) env.tick();
        assertTrue(player.isViewer(survivor), "precondition: both players see each other before the revive");

        StaminaService staminaService = new StaminaService();
        SlenderReviveListener listener = new SlenderReviveListener(() -> null, staminaService, teamService);

        listener.accept(new SlenderReviveEvent(player));

        assertEquals(SlenderBarHelper.HIDDEN, player.getTag(Tags.HIDDEN));
        assertTrue(VisibilityRules.isHidden(player), "the revived slender has to start hidden");
        assertFalse(player.isViewer(survivor), "the revived slender must not stay visible for the survivors");

        staminaService.cleanUp();
        env.destroyInstance(instance, true);
    }

    @Test
    void testRevivedSlenderUpdatesScoreboard(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        CygnusPlayer player = (CygnusPlayer) env.createPlayer(instance);

        TeamService teamService = TeamService.of();
        Team slenderTeam = Team.of(GameConfig.SLENDER_KEY, 1);
        slenderTeam.add(ColorComponent.class, new ColorComponent(ColorData.BLACK));
        slenderTeam.add(TeamNameComponent.class, new TeamNameComponent(GameConfig.SLENDER_TEAM_NAME));

        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);
        survivorTeam.add(ColorComponent.class, new ColorComponent(ColorData.GREEN));
        survivorTeam.add(TeamNameComponent.class, new TeamNameComponent(GameConfig.SURVIVOR_TEAM_NAME));

        teamService.add(slenderTeam);
        teamService.add(survivorTeam);

        ScoreboardDisplay scoreboardDisplay = new ScoreboardDisplay(teamService.getTeams());
        scoreboardDisplay.addPlayer(player, GameConfig.SURVIVOR_KEY);

        net.minestom.server.scoreboard.Team sbSurvivor = env.process().team().getTeam(GameConfig.SURVIVOR_TEAM_NAME);
        net.minestom.server.scoreboard.Team sbSlender = env.process().team().getTeam(GameConfig.SLENDER_TEAM_NAME);
        assertTrue(sbSurvivor.getMembers().contains(player.getUsername()));
        assertFalse(sbSlender.getMembers().contains(player.getUsername()));

        StaminaService staminaService = new StaminaService();
        SlenderReviveListener listener = new SlenderReviveListener(() -> null, staminaService, teamService, scoreboardDisplay);
        slenderTeam.addPlayer(player);
        listener.accept(new SlenderReviveEvent(player));

        assertFalse(sbSurvivor.getMembers().contains(player.getUsername()), "Revived slender must be removed from survivor scoreboard team");
        assertTrue(sbSlender.getMembers().contains(player.getUsername()), "Revived slender must be added to slender scoreboard team");

        staminaService.cleanUp();
        scoreboardDisplay.clear();
        env.destroyInstance(instance, true);
    }
}
