package net.onelitefeather.cygnus.creek.consequence;

import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.creek.state.CreekMood;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

/**
 * Gives a survivor away to the slender now and then: with a chance of {@link #STARE_CHANCE} at the
 * end of a patrol stare, and when a stalked survivor looked at the creek with the chance the
 * creek's mood sets, see {@link CreekMood.Traits#spotMarkChance()}.
 * <p>
 * After a mark the survivor is safe from these two for {@link #COOLDOWN_MILLIS}. A catch ignores
 * this breather and does not start it either. The reveal is shared with the catch, which also ends
 * it at the end of the round.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.21.0
 */
public final class RevealMarks implements CreekMarks {

    /** The chance that the end of a patrol stare marks the survivor. */
    static final double STARE_CHANCE = 0.5D;

    /** How long a marked survivor is safe from the next mark, in milliseconds. */
    static final long COOLDOWN_MILLIS = 20_000L;

    private final SlenderReveal reveal;
    private final Supplier<@Nullable Player> slender;
    private final RandomGenerator random;
    private final Supplier<CreekMood> mood;
    private final Map<UUID, Long> safeUntil = new ConcurrentHashMap<>();

    /**
     * Sets up the marks.
     *
     * @param reveal  gives a survivor away to the slender
     * @param slender supplies the slender, or {@code null} while there is none
     * @param random  the random source
     * @param mood    the creek's mood right now
     */
    public RevealMarks(SlenderReveal reveal, Supplier<@Nullable Player> slender,
                       RandomGenerator random, Supplier<CreekMood> mood) {
        this.reveal = reveal;
        this.slender = slender;
        this.random = random;
        this.mood = mood;
    }

    @Override
    public void stared(Player survivor, long now) {
        this.mark(survivor, STARE_CHANCE, now);
    }

    @Override
    public void spotted(Player survivor, long now) {
        this.mark(survivor, this.mood.get().traits().spotMarkChance(), now);
    }

    private void mark(Player survivor, double chance, long now) {
        Long safe = this.safeUntil.get(survivor.getUuid());
        if (safe != null && now < safe) return;
        if (this.random.nextDouble() >= chance) return;
        Player current = this.slender.get();
        if (current == null) return;
        this.safeUntil.put(survivor.getUuid(), now + COOLDOWN_MILLIS);
        this.reveal.reveal(survivor, current);
    }

    @Override
    public void cleanUp() {
        this.safeUntil.clear();
    }
}
