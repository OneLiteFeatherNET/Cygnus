package net.onelitefeather.cygnus.creek.state;

import net.minestom.server.coordinate.Pos;
import net.onelitefeather.cygnus.common.config.CreekConfig;
import net.onelitefeather.cygnus.creek.body.CreekBody;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.random.RandomGenerator;

/**
 * The creek shadows one survivor from a distance, and only that survivor can see it.
 * <p>
 * Look at it and it holds your gaze for a moment, then it is gone and turns up somewhere else,
 * just out of view. The longer the stalk lasts, the closer it creeps: it starts at
 * {@code stalkMinDistance} to {@code stalkMaxDistance} away and ends at {@link #END_MIN_DISTANCE}
 * to {@link #END_MAX_DISTANCE}. The stalk is over when time runs out or the survivor is gone.
 * </p>
 * <p>
 * It turns into a hunt once the survivor is scared enough, but only after it has run for
 * {@code huntMinStalkSeconds} and the survivor's last hunt is {@code huntCooldownSeconds} ago.
 * A scared survivor still gets stalked first, and a caught one gets a breather.
 * </p>
 * <p>
 * While the survivor does not look at it, it makes itself heard now and then, and more often the
 * longer the stalk lasts: every {@link #FIRST_GAP_MIN_MILLIS} to {@link #FIRST_GAP_MAX_MILLIS} at
 * first, every {@link #LAST_GAP_MIN_MILLIS} to {@link #LAST_GAP_MAX_MILLIS} by the end. It is also
 * heard right after moving to a new spot unseen. Moving away after being looked at stays silent.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class StalkState implements CreekState {

    /** How close the creek gets by the end of a stalk, in blocks. */
    static final double END_MIN_DISTANCE = 8.0D;

    /** How far away it may still be at the end of a stalk, in blocks. */
    static final double END_MAX_DISTANCE = 15.0D;

    /** The shortest gap between two sounds at the start of a stalk, in milliseconds. */
    static final long FIRST_GAP_MIN_MILLIS = 6000L;

    /** The longest gap between two sounds at the start of a stalk, in milliseconds. */
    static final long FIRST_GAP_MAX_MILLIS = 10_000L;

    /** The shortest gap between two sounds at the end of a stalk, in milliseconds. */
    static final long LAST_GAP_MIN_MILLIS = 3000L;

    /** The longest gap between two sounds at the end of a stalk, in milliseconds. */
    static final long LAST_GAP_MAX_MILLIS = 5000L;

    private final UUID target;
    private final long endsAt;
    private long startedAt;
    private long seenSince = -1L;
    private long nextSoundAt = -1L;

    /**
     * Sets up the stalk.
     *
     * @param target the survivor being stalked
     * @param endsAt when the stalk is over, in milliseconds
     */
    public StalkState(UUID target, long endsAt) {
        this.target = target;
        this.endsAt = endsAt;
    }

    /**
     * Starts a stalk that lasts somewhere between {@code stalkMinSeconds} and
     * {@code stalkMaxSeconds}.
     *
     * @param target the survivor to stalk
     * @param now    the current time in milliseconds
     * @param config the settings
     * @param random the random source
     * @return the state
     */
    public static StalkState starting(UUID target, long now, CreekConfig config, RandomGenerator random) {
        long seconds = random.nextLong(config.stalk().minSeconds(), config.stalk().maxSeconds() + 1L);
        return new StalkState(target, now + seconds * 1000L);
    }

    /**
     * Tells who is being stalked.
     *
     * @return the survivor's id
     */
    public UUID target() {
        return this.target;
    }

    /**
     * Tells when the stalk is over.
     *
     * @return the time in milliseconds
     */
    public long endsAt() {
        return this.endsAt;
    }

    @Override
    public void enter(CreekContext ctx) {
        this.startedAt = ctx.now();
        this.scheduleSound(ctx);
        CreekBody body = ctx.body();
        body.stop();
        body.setAggressive(false);
        body.setFrozen(false);
        body.showTo(Set.of(this.target));
    }

    @Override
    public CreekState tick(CreekContext ctx) {
        Optional<SurvivorView> found = ctx.survivor(this.target);
        if (found.isEmpty() || ctx.now() >= this.endsAt) return DoneState.INSTANCE;

        SurvivorView view = found.get();
        CreekConfig config = ctx.config();
        if (this.mayHunt(ctx, view)) return HuntState.starting(this.target, ctx);

        CreekBody body = ctx.body();
        body.lookAt(view.eyes());

        if (view.seesCreek()) {
            if (this.seenSince < 0) this.seenSince = ctx.now();
            if (ctx.now() - this.seenSince >= config.stalk().revealMillis() && this.relocate(ctx, view)) {
                this.seenSince = -1L;
                // Gone without a sound. The next one only comes after a full gap.
                this.scheduleSound(ctx);
            }
            return this;
        }
        this.seenSince = -1L;

        double distance = body.position().distance(view.position());
        boolean moved = (distance < this.minDistance(ctx) || distance > this.maxDistance(ctx))
                && this.relocate(ctx, view);
        if (this.nextSoundAt < 0) this.scheduleSound(ctx);
        if (moved || ctx.now() >= this.nextSoundAt) {
            ctx.actions().stalkSound(this.target, this.progress(ctx.now()));
            this.scheduleSound(ctx);
        }
        return this;
    }

    /**
     * Tells whether the stalk may turn into a hunt in this step.
     */
    private boolean mayHunt(CreekContext ctx, SurvivorView view) {
        CreekConfig config = ctx.config();
        if (view.dread() < config.hunt().threshold()) return false;
        if (ctx.now() - this.startedAt < config.hunt().minStalkSeconds() * 1000L) return false;
        return ctx.hunts().ready(this.target, ctx.now());
    }

    /**
     * Works out when the creek is heard next, from now.
     */
    private void scheduleSound(CreekContext ctx) {
        this.nextSoundAt = ctx.now() + soundGapMillis(this.progress(ctx.now()), ctx.random().nextDouble());
    }

    /**
     * How long until the creek is heard again. The gap shrinks the further the stalk is.
     *
     * @param progress how far into the stalk it is, from 0 to 1
     * @param roll     a random number from 0 to 1 that picks the gap within its range
     * @return the gap in milliseconds
     */
    static long soundGapMillis(double progress, double roll) {
        double min = FIRST_GAP_MIN_MILLIS + (LAST_GAP_MIN_MILLIS - FIRST_GAP_MIN_MILLIS) * progress;
        double max = FIRST_GAP_MAX_MILLIS + (LAST_GAP_MAX_MILLIS - FIRST_GAP_MAX_MILLIS) * progress;
        return Math.round(min + (max - min) * roll);
    }

    /**
     * Moves the creek to a new spot just outside the survivor's view.
     *
     * @return {@code false} if there was no free spot in this step
     */
    private boolean relocate(CreekContext ctx, SurvivorView view) {
        CreekConfig config = ctx.config();
        double min = this.minDistance(ctx);
        // The band's own minimum keeps the creek off the survivor. The fixed personal space would
        // find no spot at all once the band comes closer than that.
        Optional<Pos> spot = ctx.spots().beside(view.position(), min, this.maxDistance(ctx),
                config.stalk().minAngle(), config.stalk().maxAngle(), List.of(view.eyes()), min, ctx.random());
        spot.ifPresent(ctx.body()::teleport);
        return spot.isPresent();
    }

    /**
     * How far into the stalk it is: 0 at the start, 1 at the end.
     */
    private double progress(long now) {
        long length = this.endsAt - this.startedAt;
        if (length <= 0) return 1.0D;
        return Math.clamp((double) (now - this.startedAt) / length, 0.0D, 1.0D);
    }

    private double minDistance(CreekContext ctx) {
        double start = ctx.config().stalk().minDistance();
        return start + (END_MIN_DISTANCE - start) * this.progress(ctx.now());
    }

    private double maxDistance(CreekContext ctx) {
        double start = ctx.config().stalk().maxDistance();
        return start + (END_MAX_DISTANCE - start) * this.progress(ctx.now());
    }
}
