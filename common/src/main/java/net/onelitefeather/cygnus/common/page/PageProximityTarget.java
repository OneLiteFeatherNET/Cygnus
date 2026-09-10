package net.onelitefeather.cygnus.common.page;

import net.minestom.server.coordinate.Pos;
import org.jetbrains.annotations.Contract;

import java.util.UUID;

/**
 * Represents a page target whose proximity can be signaled to players.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public interface PageProximityTarget {

    /**
     * Returns the unique identifier of this target.
     *
     * @return the unique identifier
     */
    UUID id();

    /**
     * Returns the position of the page.
     *
     * @return the page position
     */
    Pos position();

    /**
     * Returns the remaining time-to-live ratio, between 0.0 (expired) and 1.0 (fresh).
     *
     * @return the remaining TTL fraction
     */
    double remainingTtlRatio();

    /**
     * Creates a simple {@link PageProximityTarget} from raw values.
     *
     * @param id                the unique target id
     * @param position          the target position
     * @param remainingTtlRatio the remaining TTL ratio
     * @return a new proximity target instance
     */
    @Contract(value = "_, _, _ -> new", pure = true)
    static PageProximityTarget of(UUID id, Pos position, double remainingTtlRatio) {
        return new Simple(id, position, remainingTtlRatio);
    }

    /**
     * Simple record implementation of {@link PageProximityTarget}.
     */
    record Simple(UUID id, Pos position, double remainingTtlRatio) implements PageProximityTarget {}
}
