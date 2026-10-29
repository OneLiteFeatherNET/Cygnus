package net.onelitefeather.cygnus.map;

import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Picks a map at random, with every map equally likely.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.21.0
 */
public final class RandomMapSelector implements MapSelector {

    private final RandomGenerator random;

    /**
     * Creates a selector drawing from the default random generator of the JVM.
     */
    public RandomMapSelector() {
        this(RandomGenerator.getDefault());
    }

    /**
     * Creates a selector drawing from the given random generator.
     *
     * @param random the generator the map is drawn with
     */
    public RandomMapSelector(RandomGenerator random) {
        this.random = random;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MapCandidate select(List<MapCandidate> candidates) {
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("There is no map to select from");
        }
        return candidates.get(this.random.nextInt(candidates.size()));
    }
}
