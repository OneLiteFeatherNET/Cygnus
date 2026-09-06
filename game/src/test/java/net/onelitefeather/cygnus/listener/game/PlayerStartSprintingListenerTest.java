package net.onelitefeather.cygnus.listener.game;

import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.movement.PlayerStartSprintingEvent;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import net.onelitefeather.cygnus.stamina.FoodBar;
import net.onelitefeather.cygnus.stamina.StaminaFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that the stamina drain on sprinting is limited to the players who are actually running a round.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.10.2
 */
class PlayerStartSprintingListenerTest extends CygnusPlayerTestBase {

    @Test
    void testSurvivorStartsConsumingStamina(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        CygnusPlayer player = (CygnusPlayer) env.createPlayer(instance);
        player.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);

        FoodBar bar = (FoodBar) StaminaFactory.createFoodStamina(player);
        PlayerStartSprintingListener listener = new PlayerStartSprintingListener(_ -> bar);

        PlayerStartSprintingEvent event = new PlayerStartSprintingEvent(player);
        listener.accept(event);

        assertFalse(event.isCancelled());
        bar.consume();
        assertTrue(bar.remainingShare() < 1.0f, "a sprinting survivor drains stamina");

        env.destroyInstance(instance, true);
    }

    @Test
    void testSpectatorDoesNotTouchTheStaminaBar(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        CygnusPlayer player = (CygnusPlayer) env.createPlayer(instance);
        player.setTag(Tags.TEAM_KEY, GameConfig.SPECTATOR_KEY);

        FoodBar bar = (FoodBar) StaminaFactory.createFoodStamina(player);
        PlayerStartSprintingListener listener = new PlayerStartSprintingListener(_ -> bar);

        listener.accept(new PlayerStartSprintingEvent(player));

        bar.consume();
        assertEquals(1.0f, bar.remainingShare(), "a spectator must not drain the survivor stamina bar");

        env.destroyInstance(instance, true);
    }

    @Test
    void testSpectatorWithoutAStaminaBarIsIgnored(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        CygnusPlayer player = (CygnusPlayer) env.createPlayer(instance);
        player.setTag(Tags.TEAM_KEY, GameConfig.SPECTATOR_KEY);

        // A spectator has their bar removed when they leave the round, so the lookup answers null.
        PlayerStartSprintingListener listener = new PlayerStartSprintingListener(_ -> null);

        listener.accept(new PlayerStartSprintingEvent(player));

        env.destroyInstance(instance, true);
    }

    @Test
    void testPlayerWithoutTeamTagIsIgnored(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        PlayerStartSprintingListener listener = new PlayerStartSprintingListener(_ -> null);

        listener.accept(new PlayerStartSprintingEvent(player));

        env.destroyInstance(instance, true);
    }
}
