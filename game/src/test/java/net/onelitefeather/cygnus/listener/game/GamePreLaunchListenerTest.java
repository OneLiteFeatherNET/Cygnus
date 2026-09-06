package net.onelitefeather.cygnus.listener.game;

import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.common.event.GamePreLaunchEvent;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests that the attributes a round applies are limited to the players taking part in it.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.12.1
 */
class GamePreLaunchListenerTest extends CygnusPlayerTestBase {

    @Test
    void testSpectatorKeepsTheVanillaAttributes(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance);
        Player spectator = env.createConnection().connect(instance);
        survivor.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);
        spectator.setTag(Tags.TEAM_KEY, GameConfig.SPECTATOR_KEY);

        new GamePreLaunchListener(pageCount -> {}).accept(new GamePreLaunchEvent());

        assertEquals(0.0, survivor.getAttribute(Attribute.JUMP_STRENGTH).getValue(), 0.0001,
                "a survivor takes part in the round and gets its attributes");
        assertEquals(0.42, spectator.getAttribute(Attribute.JUMP_STRENGTH).getValue(), 0.0001,
                "a spectator is out of the round and must keep the vanilla jump strength");
        assertEquals(0.1, spectator.getAttribute(Attribute.MOVEMENT_SPEED).getValue(), 0.0001,
                "a spectator must not be slowed down to the survivor speed");
        assertEquals(20.0, spectator.getAttribute(Attribute.MAX_HEALTH).getValue(), 0.0001,
                "a spectator must not receive the health scaling of the round");

        env.destroyInstance(instance, true);
    }
}
