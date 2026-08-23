package net.onelitefeather.cygnus.utils;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.stamina.StaminaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies {@link StaminaHelper#remainingShare(StaminaService, Player)}, the stamina reading the
 * tunnel vision drives itself off.
 * <p>
 * The case that matters is the player without a bar. The effect is ticked from a scheduler, so it
 * asks about players who are connected but not playing a round; reading a full bar there is what
 * keeps the effect off for them instead of closing their view completely.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
class StaminaShareTest extends CygnusPlayerTestBase {

    @Test
    @DisplayName("A player without a bar reads as untouched rather than empty")
    void playerWithoutBarIsFull(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createConnection().connect(instance, new Pos(0, 40, 0));

        assertEquals(1.0D, StaminaHelper.remainingShare(new StaminaService(), player), 1.0E-6D);
    }

    @Test
    @DisplayName("A survivor with a fresh bar reads as untouched")
    void freshBarIsFull(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createConnection().connect(instance, new Pos(0, 40, 0));

        StaminaService staminaService = new StaminaService();
        staminaService.createStaminaBars(Set.of(player));

        assertEquals(1.0D, StaminaHelper.remainingShare(staminaService, player), 1.0E-6D);
    }
}
