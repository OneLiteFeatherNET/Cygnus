package net.onelitefeather.cygnus.creek.state;

import net.minestom.server.coordinate.Pos;
import net.onelitefeather.cygnus.creek.body.CreekBody;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The creek is gone for a while: at the start of a round, or for good once it sits out the rest
 * of it. When it comes back, it turns up somewhere nobody is looking.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class VanishState implements CreekState {

    private final long until;

    /**
     * Sets up a vanish that ends at a fixed time.
     *
     * @param until when the creek may come back, in milliseconds
     */
    public VanishState(long until) {
        this.until = until;
    }

    /**
     * A vanish that never ends, for when the creek sits out the rest of the round.
     *
     * @return the state
     */
    public static VanishState forever() {
        return new VanishState(Long.MAX_VALUE);
    }

    /**
     * Tells when the creek may come back.
     *
     * @return the time in milliseconds, {@link Long#MAX_VALUE} for {@link #forever()}
     */
    public long until() {
        return this.until;
    }

    /**
     * Tells whether this vanish never ends.
     *
     * @return {@code true} for {@link #forever()}
     */
    public boolean isForever() {
        return this.until == Long.MAX_VALUE;
    }

    @Override
    public void enter(CreekContext ctx) {
        CreekBody body = ctx.body();
        body.stop();
        body.setAggressive(false);
        body.setFrozen(false);
        body.showTo(Set.of());
    }

    @Override
    public CreekState tick(CreekContext ctx) {
        if (ctx.now() < this.until) return this;

        List<Pos> observers = ctx.observerEyes();
        double distance = ctx.config().respawnMinDistance();
        // Any point of the route will do. Asking the route for its next point would only offer the
        // neighbours of where the creek disappeared, and that is often right next to a survivor.
        List<Pos> spots = ctx.route().points().stream()
                .map(point -> ctx.spots().hiddenSpotAt(point, observers, distance))
                .flatMap(Optional::stream)
                .toList();
        if (spots.isEmpty()) return this;

        ctx.body().teleport(spots.get(ctx.random().nextInt(spots.size())));
        return new PatrolState();
    }
}
