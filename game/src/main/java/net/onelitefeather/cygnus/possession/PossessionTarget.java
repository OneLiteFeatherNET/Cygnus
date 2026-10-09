package net.onelitefeather.cygnus.possession;

import net.minestom.server.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The creek as the slender sees it when he wants to look through its eyes.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.16.0
 */
public interface PossessionTarget {

    /**
     * Returns the entity of the main creek if it can be possessed right now.
     *
     * @return the entity, or {@code null} while the creek is away, done or not in the round
     */
    @Nullable Entity possessable();

    /**
     * Lets the slender's client see the creek and makes the creek noticed from farther away.
     *
     * @param slender     the slender who possesses the creek
     * @param sightFactor how much farther survivors notice the creek meanwhile
     */
    void possess(UUID slender, double sightFactor);

    /**
     * Takes back everything {@link #possess(UUID, double)} changed. Does nothing without a creek.
     */
    void release();

    /**
     * Returns the creek's normal sight range, the distance from which survivors notice it.
     *
     * @return the range in blocks
     */
    int sightRange();
}
