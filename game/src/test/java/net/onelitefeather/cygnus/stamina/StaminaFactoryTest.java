package net.onelitefeather.cygnus.stamina;

import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MicrotusExtension.class)
class StaminaFactoryTest {

    @Test
    void testFoodStaminaBarCreation(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        StaminaBar foodBar = StaminaFactory.createFoodStamina(player);

        assertNotNull(foodBar);
        assertInstanceOf(FoodBar.class, foodBar);
        assertEquals(player, foodBar.player);
        env.destroyInstance(instance, true);
    }

    @Test
    void testSlenderStaminaBarCreation(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        StaminaBar slenderBar = StaminaFactory.createSlenderStamina(player);

        assertNotNull(slenderBar);
        assertInstanceOf(SlenderBar.class, slenderBar);
        assertEquals(player, slenderBar.player);
        env.destroyInstance(instance, true);
    }
}
