package net.onelitefeather.cygnus.stamina;

import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test verifying the {@link FoodBar}'s drain/regenerate lifecycle.
 */
class FoodBarIntegrationTest extends CygnusPlayerTestBase {

    @Test
    void testStartIsReadyAndFull(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        CygnusPlayer player = (CygnusPlayer) env.createPlayer(instance);

        FoodBar foodBar = (FoodBar) StaminaFactory.createFoodStamina(player);
        foodBar.start();

        assertEquals(1.0f, player.getExp(), "food should start completely full");
        assertTrue(foodBar.canConsume(), "a fresh, ready bar should allow starting to sprint");

        foodBar.stop();
        env.destroyInstance(instance, true);
    }

    @Test
    void testDrainingBlocksSprintWhenDepleted(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        CygnusPlayer player = (CygnusPlayer) env.createPlayer(instance);

        FoodBar foodBar = (FoodBar) StaminaFactory.createFoodStamina(player);
        foodBar.start();
        foodBar.startConsume();

        foodBar.consume();
        assertEquals(0.9f, player.getExp(), 0.0001f, "draining once should take 2 of 20 food");
        assertFalse(player.hasBlockedSprinting(), "sprinting shouldn't be blocked while food remains");

        // 20 food / 2 per tick = 10 ticks to fully deplete
        for (int i = 0; i < 9; i++) {
            foodBar.consume();
        }

        assertTrue(player.hasBlockedSprinting(), "sprinting should be blocked once food is fully depleted");
        assertEquals(0.0f, player.getExp(), "depleted food should show an empty bar");

        foodBar.stop();
        env.destroyInstance(instance, true);
    }

    @Test
    void testRegenerationUnblocksSprint(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        CygnusPlayer player = (CygnusPlayer) env.createPlayer(instance);

        FoodBar foodBar = (FoodBar) StaminaFactory.createFoodStamina(player);
        foodBar.start();
        foodBar.startConsume();

        // fully deplete to enter REGENERATING with sprinting blocked
        for (int i = 0; i < 10; i++) {
            foodBar.consume();
        }
        assertTrue(player.hasBlockedSprinting());

        // 20 food, +1 per tick, needs 20 ticks to fully regenerate
        for (int i = 0; i < 20; i++) {
            foodBar.consume();
        }

        assertFalse(player.hasBlockedSprinting(), "sprinting should be unblocked once food is fully restored");
        assertEquals(1.0f, player.getExp(), "fully regenerated food should show a full bar");
        assertTrue(foodBar.canConsume(), "a fully regenerated, READY bar should allow sprinting again");

        foodBar.stop();
        env.destroyInstance(instance, true);
    }

    @Test
    void testCannotConsumeRightAfterDepletion(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        CygnusPlayer player = (CygnusPlayer) env.createPlayer(instance);

        FoodBar foodBar = (FoodBar) StaminaFactory.createFoodStamina(player);
        foodBar.start();
        foodBar.startConsume();

        // fully deplete to enter REGENERATING at its lowest point
        for (int i = 0; i < 10; i++) {
            foodBar.consume();
        }

        assertFalse(foodBar.canConsume(),
                "should not be able to sprint again immediately after running out of food");

        foodBar.stop();
        env.destroyInstance(instance, true);
    }
}
