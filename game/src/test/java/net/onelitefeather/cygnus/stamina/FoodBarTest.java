package net.onelitefeather.cygnus.stamina;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.config.StaminaConfig;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the stamina share other systems read off the survivor's bar.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
class FoodBarTest extends CygnusPlayerTestBase {

    @Test
    @DisplayName("A fresh bar reports a full share")
    void freshBarIsFull(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createConnection().connect(instance, new Pos(0, 40, 0));
        FoodBar bar = (FoodBar) StaminaFactory.createFoodStamina((CygnusPlayer) player);

        assertEquals(1.0f, bar.remainingShare(), 1.0E-6f);
    }

    @Test
    @DisplayName("Draining empties the bar and leaves it to regenerate")
    void drainEmptiesTheBar(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createConnection().connect(instance, new Pos(0, 40, 0));
        FoodBar bar = (FoodBar) StaminaFactory.createFoodStamina((CygnusPlayer) player);

        bar.drain();

        assertEquals(0.0f, bar.remainingShare(), 1.0E-6f);
        assertFalse(bar.canConsume(), "an empty bar must not allow sprinting again straight away");

        bar.consume();

        assertEquals(1.25f / 20, bar.remainingShare(), 1.0E-6f);
    }

    @Test
    @DisplayName("After running dry the sprint comes back at the resume share, not only when full")
    void exhaustionUnblocksAtTheResumeShare(Env env) {
        Instance instance = env.createFlatInstance();
        CygnusPlayer player = (CygnusPlayer) env.createConnection().connect(instance, new Pos(0, 40, 0));
        FoodBar bar = (FoodBar) StaminaFactory.createFoodStamina(player);
        bar.drain();

        // 4 x 1.25 = 5 of 20, still below the default 30 %
        for (int i = 0; i < 4; i++) {
            bar.consume();
        }
        assertTrue(player.hasBlockedSprinting());
        assertFalse(bar.canConsume());

        bar.consume(); // 6.25 of 20

        assertFalse(player.hasBlockedSprinting());
        assertTrue(bar.canConsume());
    }

    @Test
    @DisplayName("Stopping early only allows a new sprint from the resume share on")
    void voluntaryStopUsesTheResumeShare(Env env) {
        Instance instance = env.createFlatInstance();
        CygnusPlayer player = (CygnusPlayer) env.createConnection().connect(instance, new Pos(0, 40, 0));
        FoodBar bar = (FoodBar) StaminaFactory.createFoodStamina(player);
        bar.startConsume();
        for (int i = 0; i < 8; i++) {
            bar.consume(); // 20 - 8 x 2 = 4 of 20
        }

        bar.switchToRegenerating();
        assertFalse(bar.canConsume(), "4 of 20 is below the resume share");

        bar.consume();
        bar.consume(); // 6.5 of 20

        assertTrue(bar.canConsume());
    }

    @Test
    @DisplayName("The configured values replace the defaults")
    void usesTheGivenConfig(Env env) {
        Instance instance = env.createFlatInstance();
        CygnusPlayer player = (CygnusPlayer) env.createConnection().connect(instance, new Pos(0, 40, 0));
        FoodBar bar = (FoodBar) StaminaFactory.createFoodStamina(player, new StaminaConfig(0.5D, 2.0D, 5));
        bar.drain();

        for (int i = 0; i < 4; i++) {
            bar.consume(); // 8 of 20
        }
        assertFalse(bar.canConsume());

        bar.consume(); // 10 of 20

        assertTrue(bar.canConsume());
    }
}
