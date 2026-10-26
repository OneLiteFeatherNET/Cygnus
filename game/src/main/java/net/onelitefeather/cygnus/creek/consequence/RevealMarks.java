package net.onelitefeather.cygnus.creek.consequence;

import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

/**
 * Gives a survivor away to the slender now and then: with a chance of {@link #STARE_CHANCE} at the
 * end of a patrol stare, and with a chance of {@link #SPOT_CHANCE} when a stalked survivor looked
 * at the creek.
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

    /** The chance that a stalked survivor who looked at the creek is marked. */
    static final double SPOT_CHANCE = 0.25D;

    /** How long a marked survivor is safe from the next mark, in milliseconds. */
    static final long COOLDOWN_MILLIS = 20_000L;

    private final SlenderReveal reveal;
    private final Supplier<@Nullable Player> slender;
    private final LongSupplier clock;
    private final RandomGenerator random;
    private final Map<UUID, Long> safeUntil = new ConcurrentHashMap<>();

    /**
     * Sets up the marks.
     *
     * @param reveal  gives a survivor away to the slender
     * @param slender supplies the slender, or {@code null} while there is none
     * @param clock   supplies the current time in milliseconds
     * @param random  the random source
     */
    public RevealMarks(SlenderReveal reveal, Supplier<@Nullable Player> slender, LongSupplier clock,
                       RandomGenerator random) {
        this.reveal = reveal;
        this.slender = slender;
        this.clock = clock;
        this.random = random;
    }

    @Override
    public void stared(Player survivor) {
        this.mark(survivor, STARE_CHANCE);
    }

    @Override
    public void spotted(Player survivor) {
        this.mark(survivor, SPOT_CHANCE);
    }

    private void mark(Player survivor, double chance) {
        long now = this.clock.getAsLong();
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
