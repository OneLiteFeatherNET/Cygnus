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
import net.onelitefeather.cygnus.stamina.StaminaService;
import net.onelitefeather.cygnus.team.TeamHelper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

        AtomicReference<GameMap> mapSupplierRef = new AtomicReference<>(null);
        SlenderReviveListener listener = new SlenderReviveListener(mapSupplierRef::get, staminaService);

        env.process().eventHandler().addListener(SlenderReviveEvent.class, listener);

        Pos targetSpawn = new Pos(25, 70, 25);
        GameMap gameMap = new GameMap("Map", Pos.ZERO, targetSpawn, Set.of(), Set.of(new Pos(2, 64, 2)), List.of());
        mapSupplierRef.set(gameMap);

        env.process().eventHandler().call(new SlenderReviveEvent(player));

        assertEquals(GameConfig.SLENDER_KEY, player.getTag(Tags.TEAM_KEY));
        assertEquals(targetSpawn, player.getPosition());
        assertNotNull(staminaService.getSlenderBar());

        env.destroyInstance(instance, true);
    }
}
