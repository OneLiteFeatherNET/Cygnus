package net.onelitefeather.cygnus.stamina;

import net.minestom.server.entity.Player;
import net.minestom.server.potion.PotionEffect;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies which effects the slender's bar puts on him.
 *
 * <p>He used to get night vision whenever he was hidden, so hiding also meant seeing better than
 * everyone else. It is gone; hiding now only lifts the blindness.</p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.9.3
 */
class SlenderBarEffectsTest extends CygnusPlayerTestBase implements SlenderBarHelper {

    @Test
    @DisplayName("Blindness goes on while he hunts")
    void blindnessIsApplied(Env env) {
        Player slender = env.createPlayer(env.createFlatInstance());

        applyBlindness(slender);

        assertTrue(hasEffect(slender, PotionEffect.BLINDNESS));
    }

    @Test
    @DisplayName("Hiding lifts the blindness and grants nothing in its place")
    void clearingLeavesNoNightVision(Env env) {
        Player slender = env.createPlayer(env.createFlatInstance());
        applyBlindness(slender);

        clearBlindness(slender);

        assertFalse(hasEffect(slender, PotionEffect.BLINDNESS));
        assertFalse(hasEffect(slender, PotionEffect.NIGHT_VISION),
                "the slender must not see better than the survivors any more");
    }

    /**
     * Reports whether the player currently carries the given effect.
     *
     * @param player the player to check
     * @param effect the effect to look for
     * @return true if it is active
     */
    private static boolean hasEffect(Player player, PotionEffect effect) {
        return player.getActiveEffects().stream()
                .anyMatch(timed -> timed.potion().effect() == effect);
    }
}
