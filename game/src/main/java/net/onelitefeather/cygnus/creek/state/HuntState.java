package net.onelitefeather.cygnus.creek.state;

import net.onelitefeather.cygnus.common.config.CreekConfig;
import net.onelitefeather.cygnus.creek.body.CreekBody;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The creek chases one survivor, but only moves while they are not looking at it.
 * <p>
 * Look at it and it freezes. Let it get close enough, with nothing in between, and you are caught. The hunt is over after a
 * catch, when time runs out or when the survivor is gone. However it ends, the survivor's breather
 * in {@link HuntCooldowns} starts.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class HuntState implements CreekState {

    /** How close to the survivor the creek lands after a shortcut, at the least, in blocks. */
    static final double NEAR_MIN = 6.0D;

    /** How close to the survivor the creek lands after a shortcut, at the most, in blocks. */
    static final double NEAR_MAX = 10.0D;

    /** How much closer the creek has to get to count as getting anywhere, in blocks. */
    static final double PROGRESS = 0.5D;

    private final UUID target;
    private final long endsAt;
    private double bestDistance = Double.MAX_VALUE;
    private long progressSince;

    /**
     * Sets up the hunt.
     *
     * @param target the survivor being hunted
     * @param endsAt when the hunt is over, in milliseconds
     */
    public HuntState(UUID target, long endsAt) {
        this.target = target;
        this.endsAt = endsAt;
    }

    /**
     * Starts a hunt that lasts {@code huntMaxSeconds}.
     *
     * @param target the survivor to hunt
     * @param ctx    the current step
     * @return the state
     */
    static HuntState starting(UUID target, CreekContext ctx) {
        return new HuntState(target, ctx.now() + ctx.config().hunt().maxSeconds() * 1000L);
    }

    /**
     * Tells who is being hunted.
     *
     * @return the survivor's id
     */
    public UUID target() {
        return this.target;
    }

    /**
     * Tells when the hunt is over.
     *
     * @return the time in milliseconds
     */
    public long endsAt() {
        return this.endsAt;
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
        if (found.isEmpty() || ctx.now() >= this.endsAt) return this.over(ctx);

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
        if (distance <= config.hunt().catchDistance() && view.inSight()) {
            ctx.actions().caught(this.target);
            return this.over(ctx);
        }

        if (distance < this.bestDistance - PROGRESS) {
            this.bestDistance = distance;
            this.progressSince = ctx.now();
        } else if (ctx.now() - this.progressSince >= config.stuckMillis()) {
            this.shortcut(ctx, view);
            return this;
        }

        body.lookAt(view.eyes());
        body.moveTo(view.position(), config.hunt().speed());
        return this;
    }

    /**
     * Ends the hunt and starts the survivor's breather.
     */
    private CreekState over(CreekContext ctx) {
        ctx.hunts().ended(this.target, ctx.now());
        return DoneState.INSTANCE;
    }

    /**
     * When walking there gets the creek nowhere, it jumps close to the survivor instead, out of their view.
     */
    private void shortcut(CreekContext ctx, SurvivorView view) {
        ctx.spots()
                .beside(view.position(), NEAR_MIN, NEAR_MAX, ctx.config().stalk().minAngle(), 180.0D,
                        List.of(view.eyes()), NEAR_MIN, ctx.random())
                .ifPresent(ctx.body()::teleport);
        this.bestDistance = Double.MAX_VALUE;
        this.progressSince = ctx.now();
    }
}
