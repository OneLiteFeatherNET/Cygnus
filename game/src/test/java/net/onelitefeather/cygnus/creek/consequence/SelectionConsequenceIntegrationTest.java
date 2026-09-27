package net.onelitefeather.cygnus.creek.consequence;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.potion.PotionEffect;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelectionConsequenceIntegrationTest extends CygnusPlayerTestBase {

    @Test
    @DisplayName("A stun slows the selected survivor and does not count as a catch")
    void stunSlowsTheSelected(Env env) {
        Instance instance = env.createFlatInstance();
        Player selected = env.createConnection().connect(instance, new Pos(0, 40, 0));
        SelectionConsequence selection = new SelectionConsequence(List::of, Optional::of, new Random(1));

        selection.stun(selected, List.of(selected));

        assertTrue(selected.hasEffect(PotionEffect.SLOWNESS));
        assertFalse(selected.hasEffect(PotionEffect.BLINDNESS));
        assertFalse(selected.hasTag(StagedCatchConsequence.CATCHES));
    }

    @Test
    @DisplayName("A stun blinds other survivors nearby, nobody else")
    void stunBlindsOtherSurvivorsNearby(Env env) {
        Instance instance = env.createFlatInstance();
        Player selected = env.createConnection().connect(instance, new Pos(0, 40, 0));
        Player near = env.createConnection().connect(instance, new Pos(5, 40, 0));
        Player far = env.createConnection().connect(instance, new Pos(20, 40, 0));
        Player slender = env.createConnection().connect(instance, new Pos(3, 40, 0));
        SelectionConsequence selection = new SelectionConsequence(List::of, Optional::of, new Random(1));

        selection.stun(selected, List.of(selected, near, far));

        assertTrue(near.hasEffect(PotionEffect.BLINDNESS));
        assertFalse(far.hasEffect(PotionEffect.BLINDNESS));
        assertFalse(slender.hasEffect(PotionEffect.BLINDNESS), "only survivors go blind");
        assertFalse(selected.hasEffect(PotionEffect.BLINDNESS));
    }

    @Test
    @DisplayName("Vanishing blinds the survivors nearby, nobody else")
    void vanishBlindsSurvivorsNearby(Env env) {
        Instance instance = env.createFlatInstance();
        Player near = env.createConnection().connect(instance, new Pos(5, 40, 0));
        Player far = env.createConnection().connect(instance, new Pos(20, 40, 0));
        Player slender = env.createConnection().connect(instance, new Pos(3, 40, 0));
        SelectionConsequence selection = new SelectionConsequence(List::of, Optional::of, new Random(1));

        selection.vanishAt(new Pos(0, 40, 0), List.of(near, far));

        assertTrue(near.hasEffect(PotionEffect.BLINDNESS));
        assertFalse(far.hasEffect(PotionEffect.BLINDNESS));
        assertFalse(slender.hasEffect(PotionEffect.BLINDNESS), "only survivors go blind");
    }

    @Test
    @DisplayName("A teleport lands on a route point 20 to 40 blocks away")
    void teleportLandsInRange(Env env) {
        Instance instance = env.createFlatInstance();
        Player selected = env.createConnection().connect(instance, new Pos(0.5, 40, 0.5));
        List<Pos> points = List.of(new Pos(10.5, 40, 0.5), new Pos(30.5, 40, 0.5), new Pos(60.5, 40, 0.5));
        SelectionConsequence selection = new SelectionConsequence(() -> points, Optional::of, new Random(1));

        assertTrue(selection.teleportAway(selected));
        env.tick();

        assertTrue(selected.getPosition().sameBlock(new Pos(30.5, 40, 0.5)), "stands at " + selected.getPosition());
    }

    @Test
    @DisplayName("Without a route point in range there is no teleport, the stun is applied instead")
    void fallsBackToTheStunWithoutAFittingPoint(Env env) {
        Instance instance = env.createFlatInstance();
        Player selected = env.createConnection().connect(instance, new Pos(0.5, 40, 0.5));
        // The only point is too close. nextBoolean() is false for 0, so apply() tries the teleport first.
        SelectionConsequence selection = new SelectionConsequence(() -> List.of(new Pos(5.5, 40, 0.5)), Optional::of,
                () -> 0L);

        selection.apply(selected, List.of(selected));
        env.tick();

        assertTrue(selected.getPosition().sameBlock(new Pos(0.5, 40, 0.5)), "stands at " + selected.getPosition());
        assertTrue(selected.hasEffect(PotionEffect.SLOWNESS));
    }

    @Test
    @DisplayName("Cleaning up removes every slowness and blindness it applied")
    void cleanUpRemovesTheEffects(Env env) {
        Instance instance = env.createFlatInstance();
        Player selected = env.createConnection().connect(instance, new Pos(0, 40, 0));
        Player near = env.createConnection().connect(instance, new Pos(5, 40, 0));
        SelectionConsequence selection = new SelectionConsequence(List::of, Optional::of, new Random(1));
        selection.stun(selected, List.of(selected, near));

        selection.cleanUp();

        assertFalse(selected.hasEffect(PotionEffect.SLOWNESS));
        assertFalse(near.hasEffect(PotionEffect.BLINDNESS));
    }
}
