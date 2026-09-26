package net.onelitefeather.cygnus.creek.consequence;

import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import net.onelitefeather.cygnus.common.config.CreekConfig;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

/**
 * Punishes every catch, and sometimes gives the survivor away to the slender as well: always
 * from the {@code betrayalCatchCount}-th catch on, and before that with a chance of
 * {@code betrayalChance}.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class StagedCatchConsequence implements CatchConsequence {

    /** How often a survivor has been caught since they were last given away. */
    static final Tag<Integer> CATCHES = Tag.Transient("creekCatches");

    private final CatchConsequence effects;
    private final SlenderReveal reveal;
    private final Supplier<@Nullable Player> slender;
    private final int betrayalCatchCount;
    private final double betrayalChance;
    private final RandomGenerator random;
    private final Set<Player> counted = ConcurrentHashMap.newKeySet();

    /**
     * Sets up the consequence.
     *
     * @param effects the punishment for every catch
     * @param reveal  gives a survivor away to the slender
     * @param slender supplies the slender, or {@code null} while there is none
     * @param config  the settings for giving survivors away
     * @param random  the random source
     */
    public StagedCatchConsequence(CatchConsequence effects, SlenderReveal reveal, Supplier<@Nullable Player> slender,
                                  CreekConfig config, RandomGenerator random) {
        this.effects = effects;
        this.reveal = reveal;
        this.slender = slender;
        this.betrayalCatchCount = config.betrayalCatchCount();
        this.betrayalChance = config.betrayalChance();
        this.random = random;
    }

    @Override
    public void apply(Player survivor) {
        this.effects.apply(survivor);

        Integer before = survivor.getTag(CATCHES);
        int catches = before == null ? 1 : before + 1;
        Player current = this.slender.get();
        if (current != null && this.betrays(catches)) {
            this.reveal.reveal(survivor, current);
            survivor.removeTag(CATCHES);
            this.counted.remove(survivor);
            return;
        }
        survivor.setTag(CATCHES, catches);
        this.counted.add(survivor);
    }

    private boolean betrays(int catches) {
        return catches >= this.betrayalCatchCount || this.random.nextDouble() < this.betrayalChance;
    }

    @Override
    public void cleanUp() {
        this.effects.cleanUp();
        this.reveal.cleanUp();
        for (Player player : List.copyOf(this.counted)) {
            player.removeTag(CATCHES);
        }
        this.counted.clear();
    }
}
