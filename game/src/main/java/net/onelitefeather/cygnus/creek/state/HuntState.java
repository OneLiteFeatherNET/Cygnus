package net.onelitefeather.cygnus.creek.state;

import net.minestom.server.coordinate.Pos;
import net.onelitefeather.cygnus.common.config.CreekConfig;
import net.onelitefeather.cygnus.creek.body.CreekBody;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The creek chases one survivor, but only moves while that survivor is not looking at it.
 * <p>
 * When the survivor looks, the creek freezes. When it gets close enough, the survivor is caught.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class HuntState implements CreekState {

    /** Minimum distance to the target after a shortcut, in blocks. */
    static final double NEAR_MIN = 6.0D;

    /** Maximum distance to the target after a shortcut, in blocks. */
    static final double NEAR_MAX = 10.0D;

    /** How much closer the creek has to get to count as progress, in blocks. */
    static final double PROGRESS = 0.5D;

    private final UUID target;
    private final long endsAt;
    private double bestDistance = Double.MAX_VALUE;
    private long progressSince;

    /**
     * Creates the state.
     *
     * @param target the hunted survivor
     * @param endsAt when the hunt ends, in milliseconds
     */
    public HuntState(UUID target, long endsAt) {
        this.target = target;
        this.endsAt = endsAt;
    }

    /**
     * Starts a hunt that ends after {@code huntMaxSeconds}.
     *
     * @param target the survivor to hunt
     * @param ctx    the current step
     * @return the state
     */
    static HuntState starting(UUID target, CreekContext ctx) {
        return new HuntState(target, ctx.now() + ctx.config().huntMaxSeconds() * 1000L);
    }

    /**
     * Returns the hunted survivor.
     *
     * @return the survivor's id
     */
    public UUID target() {
        return this.target;
    }

    @Override
    public void enter(CreekContext ctx) {
        CreekBody body = ctx.body();
        body.showTo(Set.of(this.target));
        body.setAggressive(true);
        body.setFrozen(false);
        this.progressSince = ctx.now();
    }

    @Override
    public CreekState tick(CreekContext ctx) {
        Optional<SurvivorView> found = ctx.survivor(this.target);
        if (found.isEmpty() || ctx.now() >= this.endsAt) return VanishState.after(ctx);

        SurvivorView view = found.get();
        CreekConfig config = ctx.config();
        CreekBody body = ctx.body();

        if (view.seesCreek()) {
            body.setFrozen(true);
            body.stop();
            this.progressSince = ctx.now();
            return this;
        }
        body.setFrozen(false);

        double distance = body.position().distance(view.position());
        if (distance <= config.catchDistance()) {
            ctx.onCatch().accept(this.target);
            return VanishState.after(ctx);
        }

        if (distance < this.bestDistance - PROGRESS) {
            this.bestDistance = distance;
            this.progressSince = ctx.now();
        } else if (ctx.now() - this.progressSince >= config.stuckMillis()) {
            this.shortcut(ctx, view);
            return this;
        }

        body.lookAt(view.eyes());
        body.moveTo(view.position(), config.huntSpeed());
        return this;
    }

    /**
     * Teleports the creek near the target, out of its view, when walking there does not work.
     */
    private void shortcut(CreekContext ctx, SurvivorView view) {
        ctx.spots()
                .beside(view.position(), NEAR_MIN, NEAR_MAX, ctx.config().stalkMinAngle(), 180.0D,
                        List.of(view.eyes()), NEAR_MIN, ctx.random())
                .ifPresent(ctx.body()::teleport);
        this.bestDistance = Double.MAX_VALUE;
        this.progressSince = ctx.now();
    }
}
