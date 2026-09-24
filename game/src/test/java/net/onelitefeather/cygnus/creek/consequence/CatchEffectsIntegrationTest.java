package net.onelitefeather.cygnus.creek.consequence;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.potion.PotionEffect;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import net.onelitefeather.cygnus.stamina.FoodBar;
import net.onelitefeather.cygnus.stamina.StaminaFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatchEffectsIntegrationTest extends CygnusPlayerTestBase {

    @Test
    @DisplayName("A catch drains the stamina and slows the survivor")
    void drainsAndSlows(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0));
        FoodBar bar = (FoodBar) StaminaFactory.createFoodStamina((CygnusPlayer) survivor);
        CatchEffects effects = new CatchEffects(_ -> true, _ -> bar, 4);

        effects.apply(survivor);

        assertEquals(0.0f, bar.remainingShare(), 1.0E-6f);
        assertTrue(survivor.hasEffect(PotionEffect.SLOWNESS));
    }

    @Test
    @DisplayName("Without a corpse to borrow, the scare still happens")
    void scaresWithoutACorpse(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0));
        CatchEffects effects = new CatchEffects(_ -> false, _ -> null, 4);

        effects.apply(survivor);

        assertTrue(survivor.hasEffect(PotionEffect.DARKNESS));
    }

    @Test
    @DisplayName("Cleaning up lifts the slowness")
    void cleanUpLiftsTheSlowness(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0));
        CatchEffects effects = new CatchEffects(_ -> true, _ -> null, 4);
        effects.apply(survivor);

        effects.cleanUp();

        assertFalse(survivor.hasEffect(PotionEffect.SLOWNESS));
    }
}
