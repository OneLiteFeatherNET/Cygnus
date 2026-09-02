package net.onelitefeather.cygnus.listener.game;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.event.SlenderReviveEvent;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import net.onelitefeather.cygnus.stamina.SlenderBarHelper;
import net.onelitefeather.cygnus.stamina.StaminaService;
import net.onelitefeather.cygnus.team.TeamHelper;
import net.onelitefeather.cygnus.visibility.VisibilityRules;
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
}
