package net.onelitefeather.cygnus.stamina;

import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@Disabled("The Minestom framework doesn't support custom player implementations in tests")
@ExtendWith(MicrotusExtension.class)
class StaminaFactoryTest {

    private static Instance instance;
    private static Player player;

    @BeforeAll
    static void setup(@NotNull Env env) {
        env.process().connection().setPlayerProvider(CygnusPlayer::new);
        instance = env.createFlatInstance();
        player = env.createPlayer(instance);
    }

    @AfterAll
    static void teardown(@NotNull Env env) {
        env.destroyInstance(instance, true);
        instance = null;
        player = null;
    }

    @Test
    void testFoodStaminaBarCreation() {
        StaminaBar foodBar = StaminaFactory.createFoodStamina((CygnusPlayer) player);
        assertNotNull(foodBar);
        assertInstanceOf(FoodBar.class, foodBar);
        assertEquals(player, foodBar.player);
    }

    @Test
    void testSlenderStaminaBarCreation() {
        StaminaBar slenderBar = StaminaFactory.createSlenderStamina((CygnusPlayer) player);

        assertNotNull(slenderBar);
        assertInstanceOf(SlenderBar.class, slenderBar);
        assertEquals(player, slenderBar.player);
    }
}
