package net.onelitefeather.cygnus.creek.consequence;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrackedEffectsIntegrationTest extends CygnusPlayerTestBase {

    @Test
    @DisplayName("Removing takes back every effect it handed out")
    void removesItsOwnEffects(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createConnection().connect(instance, new Pos(0, 40, 0));
        TrackedEffects effects = new TrackedEffects();
        effects.add(player, new Potion(PotionEffect.SLOWNESS, 0, 100));
        effects.add(player, new Potion(PotionEffect.BLINDNESS, 0, 100));

        effects.removeAll();

        assertFalse(player.hasEffect(PotionEffect.SLOWNESS));
        assertFalse(player.hasEffect(PotionEffect.BLINDNESS));
    }

    @Test
    @DisplayName("An effect something else put on the player since is left alone")
    void leavesForeignEffects(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createConnection().connect(instance, new Pos(0, 40, 0));
        TrackedEffects effects = new TrackedEffects();
        effects.add(player, new Potion(PotionEffect.SLOWNESS, 0, 100));
        player.addEffect(new Potion(PotionEffect.SLOWNESS, 2, 400));

        effects.removeAll();

        assertTrue(player.hasEffect(PotionEffect.SLOWNESS));
        assertEquals(2, player.getEffectLevel(PotionEffect.SLOWNESS));
    }

    @Test
    @DisplayName("Handing out the same effect again replaces the one it remembers")
    void remembersTheLatest(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createConnection().connect(instance, new Pos(0, 40, 0));
        TrackedEffects effects = new TrackedEffects();
        effects.add(player, new Potion(PotionEffect.SLOWNESS, 0, 100));
        effects.add(player, new Potion(PotionEffect.SLOWNESS, 6, 40));

        effects.removeAll();

        assertFalse(player.hasEffect(PotionEffect.SLOWNESS));
    }
}
