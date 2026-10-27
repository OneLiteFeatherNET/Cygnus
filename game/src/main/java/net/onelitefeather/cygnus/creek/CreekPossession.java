package net.onelitefeather.cygnus.creek;

import net.minestom.server.entity.Entity;
import net.onelitefeather.cygnus.creek.state.CreekState;
import net.onelitefeather.cygnus.creek.state.DoneState;
import net.onelitefeather.cygnus.creek.state.VanishState;
import net.onelitefeather.cygnus.creek.world.CreekSight;
import net.onelitefeather.cygnus.possession.PossessionTarget;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * The patrolling creek as the slender possesses it.
 * <p>
 * Only the patrolling creek can be possessed. The variants keep the normal sight. They haunt one
 * survivor each and are none of the slender's business.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.21.0
 */
public final class CreekPossession implements PossessionTarget {

    private final Supplier<@Nullable Creek> creek;
    private final int sightRange;
    private final int viewAngle;

    /**
     * Sets up the possession.
     *
     * @param creek      supplies the patrolling creek, or {@code null} while there is none
     * @param sightRange how far away the creek is noticed without a possession, in blocks
     * @param viewAngle  how far off to the side it is noticed, in degrees from the view centre
     */
    CreekPossession(Supplier<@Nullable Creek> creek, int sightRange, int viewAngle) {
        this.creek = creek;
        this.sightRange = sightRange;
        this.viewAngle = viewAngle;
    }

    @Override
    public @Nullable Entity possessable() {
        Creek current = this.creek.get();
        if (current == null) return null;
        CreekState state = current.state();
        if (state instanceof VanishState || state instanceof DoneState) return null;
        return current.body().entity();
    }

    @Override
    public void possess(UUID slender, double sightFactor) {
        Creek current = this.creek.get();
        if (current == null) return;
        int range = (int) Math.round(this.sightRange * sightFactor);
        current.body().observe(slender);
        current.boost(new CreekSight(range, this.viewAngle));
    }

    @Override
    public void release() {
        Creek current = this.creek.get();
        if (current == null) return;
        current.body().observe(null);
        current.boost(null);
    }

    @Override
    public int sightRange() {
        return this.sightRange;
    }
}
