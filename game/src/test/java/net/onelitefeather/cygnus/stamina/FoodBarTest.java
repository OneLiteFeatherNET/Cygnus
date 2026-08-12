package net.onelitefeather.cygnus.stamina;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
