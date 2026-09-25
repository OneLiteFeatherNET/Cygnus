package net.onelitefeather.cygnus.creek.state;

import net.minestom.server.coordinate.Pos;
import net.onelitefeather.cygnus.common.config.CreekConfig;
import net.onelitefeather.cygnus.creek.body.CreekBody;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The creek is invisible for a while. Afterwards it reappears somewhere nobody is looking.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class VanishState implements CreekState {

    private final long until;

    /**
     * Creates a vanish that ends at a fixed time.
     *
     * @param until when the creek may come back, in milliseconds
     */
    public VanishState(long until) {
        this.until = until;
    }

    /**
     * Starts a vanish. The higher the dread of the survivors, the shorter it is.
     *
     * @param ctx the current step
     * @return the state
     */
    public static VanishState after(CreekContext ctx) {
        return new VanishState(ctx.now() + cooldownMillis(ctx.config(), ctx.highestDread()));
    }

    /**
     * Starts a vanish that never ends. Used when the creek should sit out the rest of the round.
     *
     * @return the state
     */
    public static VanishState forever() {
        return new VanishState(Long.MAX_VALUE);
    }

    /**
     * Returns whether this vanish never ends.
     *
     * @return {@code true} for {@link #forever()}
     */
    public boolean isForever() {
        return this.until == Long.MAX_VALUE;
    }

    /**
     * Calculates how long the creek stays away: {@code vanishMaxSeconds} at no dread,
     * {@code vanishMinSeconds} at full dread.
     *
     * @param config the settings
     * @param dread  the highest dread among the survivors
     * @return the cooldown in milliseconds
     */
    static long cooldownMillis(CreekConfig config, double dread) {
        double span = config.vanishMaxSeconds() - config.vanishMinSeconds();
        double seconds = config.vanishMaxSeconds() - span * Math.clamp(dread, 0.0D, 1.0D);
        return Math.round(seconds * 1000.0D);
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
        // neighbours of where the creek vanished, which is often right next to a survivor.
        List<Pos> spots = ctx.route().points().stream()
                .map(point -> ctx.spots().hiddenSpotAt(point, observers, distance))
                .flatMap(Optional::stream)
                .toList();
        if (spots.isEmpty()) return this;

        ctx.body().teleport(spots.get(ctx.random().nextInt(spots.size())));
        return new WanderState(ctx.now());
    }
}
