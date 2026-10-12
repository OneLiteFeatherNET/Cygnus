package net.onelitefeather.cygnus.clientguard;

import net.onelitefeather.cygnus.common.config.ShaderMod;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;

/**
 * The pure part of the shader probe: which translation key goes on which sign line, and which answered lines
 * show that a key resolved. It knows nothing about Minestom, so the rules are tested without a server.
 * <p>
 * A probe line is a translatable text with a key and a fallback. A client that has the key shows the translated
 * text on the sign, a client that has not shows the fallback. The fallback is a random token per line, so a
 * translated answer can never be mistaken for an unresolved one. A line that is blank counts as unresolved: a
 * blank answer says nothing about the mods.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.16.0
 */
public final class ShaderProbePlan {

    /** A sign has four lines, so a batch carries at most four keys. */
    public static final int LINES_PER_SIGN = 4;

    private static final String TOKEN_PREFIX = "cygnus-probe-";

    private ShaderProbePlan() {
    }

    /**
     * One line of a probe sign.
     *
     * @param mod      the mod that owns the key
     * @param key      the translation key to ask for
     * @param fallback the token the line shows when the key does not resolve
     * @author TheMeinerLP
     * @version 1.0.0
     * @since 2.16.0
     */
    public record ProbeLine(ShaderMod mod, String key, String fallback) {
    }

    /**
     * Lays out the keys of the given mods in batches of four, one batch per sign. The keys keep the order of the
     * mods, and every line gets its own fallback token drawn from the given random source.
     *
     * @param mods   the mods to probe, in the order they should be asked
     * @param random the source of the fallback tokens. A seeded source makes the plan repeatable.
     * @return the batches, empty if no mod has a key
     */
    public static List<List<ProbeLine>> plan(Collection<ShaderMod> mods, RandomGenerator random) {
        Set<String> tokens = new HashSet<>();
        List<ProbeLine> lines = new ArrayList<>();
        for (ShaderMod mod : new LinkedHashSet<>(mods)) {
            for (String key : mod.translationKeys()) {
                lines.add(new ProbeLine(mod, key, nextToken(random, tokens)));
            }
        }
        List<List<ProbeLine>> batches = new ArrayList<>();
        for (int start = 0; start < lines.size(); start += LINES_PER_SIGN) {
            batches.add(List.copyOf(lines.subList(start, Math.min(start + LINES_PER_SIGN, lines.size()))));
        }
        return List.copyOf(batches);
    }

    /**
     * Finds the mods whose keys resolved in one answered sign. A line resolves when it is not blank and differs
     * from its fallback. A line the answer does not reach is not resolved.
     *
     * @param batch  the lines the sign was sent with
     * @param answer the four lines the client answered with, in order
     * @return the mods with at least one resolved key, never {@code null}
     */
    public static Set<ShaderMod> resolved(List<ProbeLine> batch, List<String> answer) {
        Set<ShaderMod> detected = EnumSet.noneOf(ShaderMod.class);
        for (int index = 0; index < batch.size() && index < answer.size(); index++) {
            ProbeLine line = batch.get(index);
            if (isResolved(line, answer.get(index))) {
                detected.add(line.mod());
            }
        }
        return Collections.unmodifiableSet(detected);
    }

    private static boolean isResolved(ProbeLine line, @Nullable String answered) {
        return answered != null && !answered.isBlank() && !answered.equals(line.fallback());
    }

    private static String nextToken(RandomGenerator random, Set<String> used) {
        String token;
        do {
            token = TOKEN_PREFIX + Long.toHexString(random.nextLong());
        } while (!used.add(token));
        return token;
    }
}
