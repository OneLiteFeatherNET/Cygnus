package net.onelitefeather.cygnus.clientguard;

import net.onelitefeather.cygnus.common.config.ShaderMod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The probe's pure logic: which keys go on which sign line, and which answered lines count as resolved. It
 * needs no server, so every rule is checked here on its own.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.16.0
 */
class ShaderProbePlanTest {

    private static RandomGenerator seeded() {
        return RandomGeneratorFactory.of("L64X128MixRandom").create(42L);
    }

    private static List<ShaderMod> irisAndSodium() {
        return List.of(ShaderMod.IRIS, ShaderMod.SODIUM);
    }

    @Test
    @DisplayName("A line that differs from its fallback marks its mod as detected")
    void resolvedLineDetectsItsMod() {
        List<ShaderProbePlan.ProbeLine> line = ShaderProbePlan.plan(List.of(ShaderMod.IRIS), seeded()).getFirst();

        Set<ShaderMod> detected = ShaderProbePlan.resolved(line, List.of("Apply", line.get(1).fallback()));

        assertEquals(Set.of(ShaderMod.IRIS), detected, "a translated line must detect the mod that owns its key");
    }

    @Test
    @DisplayName("A line that still shows its fallback does not detect its mod")
    void fallbackLineDoesNotDetect() {
        List<ShaderProbePlan.ProbeLine> line = ShaderProbePlan.plan(List.of(ShaderMod.IRIS), seeded()).getFirst();

        Set<ShaderMod> detected = ShaderProbePlan.resolved(line, fallbacksOf(line));

        assertTrue(detected.isEmpty(), "an unresolved key must not detect anything, but was " + detected);
    }

    @Test
    @DisplayName("A blank line does not detect its mod, since the answer is ambiguous")
    void blankLineDoesNotDetect() {
        List<ShaderProbePlan.ProbeLine> line = ShaderProbePlan.plan(List.of(ShaderMod.IRIS), seeded()).getFirst();

        Set<ShaderMod> detected = ShaderProbePlan.resolved(line, List.of("", " ", "", ""));

        assertFalse(detected.contains(ShaderMod.IRIS), "a blank answer must not count as a resolved key");
    }

    @Test
    @DisplayName("A partly resolved batch detects only the mods whose lines resolved")
    void partialBatchDetectsOnlyResolvedMods() {
        List<ShaderProbePlan.ProbeLine> batch = ShaderProbePlan.plan(irisAndSodium(), seeded()).getFirst();
        List<String> answer = fallbacksOf(batch);
        answer.set(2, "Sodium options");

        Set<ShaderMod> detected = ShaderProbePlan.resolved(batch, answer);

        assertEquals(Set.of(ShaderMod.SODIUM), detected, "only the resolved sodium line may detect sodium");
    }

    @Test
    @DisplayName("A short answer detects only the lines it reaches")
    void shortAnswerDetectsOnlyReachedLines() {
        List<ShaderProbePlan.ProbeLine> batch = ShaderProbePlan.plan(irisAndSodium(), seeded()).getFirst();

        Set<ShaderMod> detected = ShaderProbePlan.resolved(batch, List.of("Apply"));

        assertEquals(Set.of(ShaderMod.IRIS), detected, "the unreached sodium lines must not detect sodium");
    }

    @Test
    @DisplayName("Keys are packed four to a sign, in the order of the mods")
    void keysArePackedFourToASign() {
        List<List<ShaderProbePlan.ProbeLine>> batches = ShaderProbePlan.plan(
                List.of(ShaderMod.IRIS, ShaderMod.OPTIFINE, ShaderMod.CANVAS, ShaderMod.SODIUM), seeded());

        assertEquals(2, batches.size(), "six keys need two signs");
        assertEquals(4, batches.getFirst().size(), "the first sign carries four lines");
        assertEquals("options.iris.apply", batches.getFirst().getFirst().key(), "the keys keep the order of the mods");
        assertEquals(ShaderMod.SODIUM, batches.get(1).getFirst().mod(), "the second sign starts with the next mod");
    }

    @Test
    @DisplayName("Every line of a plan carries its own fallback")
    void everyLineHasItsOwnFallback() {
        List<ShaderProbePlan.ProbeLine> lines = ShaderProbePlan.plan(
                List.of(ShaderMod.IRIS, ShaderMod.SODIUM), seeded()).stream().flatMap(List::stream).toList();

        Set<String> fallbacks = new HashSet<>();
        lines.forEach(line -> fallbacks.add(line.fallback()));

        assertEquals(lines.size(), fallbacks.size(), "two lines sharing a fallback could not tell their keys apart");
    }

    @Test
    @DisplayName("The same seed gives the same fallbacks, so the plan is repeatable")
    void sameSeedGivesSameFallbacks() {
        assertEquals(ShaderProbePlan.plan(irisAndSodium(), seeded()), ShaderProbePlan.plan(irisAndSodium(), seeded()),
                "a seeded plan must not change between runs");
    }

    @Test
    @DisplayName("No mods give no batches, so nothing is sent")
    void noModsGiveNoBatches() {
        assertTrue(ShaderProbePlan.plan(List.of(), seeded()).isEmpty(), "without mods there is nothing to probe");
    }

    private static List<String> fallbacksOf(List<ShaderProbePlan.ProbeLine> batch) {
        return new ArrayList<>(batch.stream().map(ShaderProbePlan.ProbeLine::fallback).toList());
    }
}
