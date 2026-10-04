package net.onelitefeather.cygnus.creek.state;

import net.minestom.server.coordinate.Pos;
import net.onelitefeather.cygnus.common.config.CreekConfig;
import net.onelitefeather.cygnus.creek.body.CreekBody;
import net.onelitefeather.cygnus.creek.world.RouteStep;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The creek on its rounds: it walks its routes, and every survivor can see it.
 * <p>
 * Come within {@link #SELECT_RADIUS} of it with nothing in between and it picks out whoever is
 * closest, stares at them for a second and then freezes them or flings them away. Duck behind a
 * wall during that second and nothing happens. After that it simply walks on and leaves
 * everyone alone for {@link #SELECT_COOLDOWN_MILLIS}. Spot it from farther away and it stops and
 * looks back at you for a moment. At some waypoints it rests for a while, either because the route
 * says so or just by chance; whichever takes longer wins.
 * </p>
 * <p>
 * At the end of a route no other route links to, it may vanish instead of resting there, which
 * blinds anyone close by. It stays away for as long as it would have rested and then turns up
 * somewhere else on the map, so it does not keep pacing one stretch of routes all round.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class PatrolState implements CreekState {

    /** How close a survivor has to come before the creek picks them out, in blocks. */
    static final double SELECT_RADIUS = 4.0D;

    /** How long it stares at the chosen survivor before anything happens, in milliseconds. */
    static final long STARE_MILLIS = 1000L;

    /** How long it leaves everyone alone after picking someone out, in milliseconds. */
    static final long SELECT_COOLDOWN_MILLIS = 10_000L;

    /** How likely it vanishes on reaching a dead end instead of resting there. */
    static final double DEAD_END_VANISH_CHANCE = 0.5D;

    /** How close counts as having reached a waypoint, in blocks. */
    static final double ARRIVED = 2.0D;

    /** How far it has to get to count as moving at all, in blocks. */
    static final double PROGRESS = 0.5D;

    private @Nullable RouteStep goal;
    private boolean watched;
    private long pausedUntil;
    private long restingUntil;
    private Pos progressAt = Pos.ZERO;
    private long progressSince;
    private Set<UUID> shownTo = Set.of();
    private @Nullable UUID staring;
    private long stareUntil;
    private long selectAllowedAt;

    /**
     * Tells who the creek is staring at right now.
     *
     * @return the survivor's id, or empty while it stares at nobody
     */
    public Optional<UUID> staring() {
        return Optional.ofNullable(this.staring);
    }

    /**
     * Tells when the creek may pick someone out again.
     *
     * @return the time in milliseconds
     */
    public long selectAllowedAt() {
        return this.selectAllowedAt;
    }

    @Override
    public void enter(CreekContext ctx) {
        CreekBody body = ctx.body();
        body.setAggressive(false);
        body.setFrozen(false);
        this.showToSurvivors(ctx);
        this.markProgress(ctx.now(), body.position());
    }

    @Override
    public CreekState tick(CreekContext ctx) {
        CreekConfig config = ctx.config();
        CreekBody body = ctx.body();
        Pos here = body.position();
        this.showToSurvivors(ctx);

        if (this.stare(ctx)) return this;
        if (ctx.now() >= this.selectAllowedAt && this.select(ctx, here)) return this;

        Optional<SurvivorView> watcher = ctx.survivors().stream().filter(SurvivorView::seesCreek).findFirst();
        if (watcher.isEmpty()) {
            this.watched = false;
        } else if (this.pause(ctx, watcher.get())) {
            return this;
        }

        if (ctx.now() < this.restingUntil) {
            body.stop();
            return this;
        }
        RouteStep reached = this.goal;
        if (reached != null && here.distance(reached.target()) < ARRIVED) {
            this.goal = null;
            if (reached.deadEnd() && ctx.random().nextDouble() < DEAD_END_VANISH_CHANCE) {
                ctx.actions().vanished(here);
                // Gone for as long as it would have rested here, then back somewhere else.
                return new VanishState(ctx.now() + reached.pauseMillis());
            }
            long rest = this.restMillis(ctx, reached);
            if (rest > 0) {
                this.restingUntil = ctx.now() + rest;
                body.stop();
                return this;
            }
        }

        if (this.needsNewGoal(ctx, here)) {
            this.goal = ctx.route().next(here, _ -> true, ctx.random()).orElse(null);
            this.markProgress(ctx.now(), here);
        }
        RouteStep next = this.goal;
        if (next == null) {
            body.stop();
            return this;
        }
        body.moveTo(next.target(), config.wanderSpeed());
        return this;
    }

    /**
     * Keeps staring at the chosen survivor. Once the second is up, the consequence hits them.
     *
     * @return {@code true} while the stare lasts
     */
    private boolean stare(CreekContext ctx) {
        UUID target = this.staring;
        if (target == null) return false;
        Optional<SurvivorView> view = ctx.survivor(target);
        if (view.isEmpty() || !view.get().inSight()) {
            // They left or ducked behind a wall before the stare was over: nothing happens, and no
            // cooldown starts. Anyone still in sight may be picked out right away.
            this.staring = null;
            return false;
        }
        CreekBody body = ctx.body();
        if (ctx.now() < this.stareUntil) {
            body.stop();
            body.lookAt(view.get().eyes());
            return true;
        }
        this.staring = null;
        this.selectAllowedAt = ctx.now() + SELECT_COOLDOWN_MILLIS;
        // They are most likely still looking at it. Walk on rather than stop again to look back.
        this.watched = true;
        this.markProgress(ctx.now(), body.position());
        ctx.actions().selected(target);
        return false;
    }

    /**
     * Picks out the closest survivor within {@link #SELECT_RADIUS} with nothing in between and
     * starts staring at them.
     *
     * @return {@code true} if someone was picked out
     */
    private boolean select(CreekContext ctx, Pos here) {
        SurvivorView nearest = null;
        double best = SELECT_RADIUS;
        for (SurvivorView view : ctx.survivors()) {
            if (!view.inSight()) continue;
            double distance = view.position().distance(here);
            if (distance <= best) {
                best = distance;
                nearest = view;
            }
        }
        if (nearest == null) return false;
        this.staring = nearest.id();
        this.stareUntil = ctx.now() + STARE_MILLIS;
        ctx.body().stop();
        ctx.body().lookAt(nearest.eyes());
        this.markProgress(ctx.now(), here);
        return true;
    }

    /**
     * Stops and looks back for a moment when someone spots the creek from afar.
     *
     * @return {@code true} while it is looking back
     */
    private boolean pause(CreekContext ctx, SurvivorView watcher) {
        if (!this.watched) {
            this.watched = true;
            this.pausedUntil = ctx.now() + ctx.config().wanderPauseMillis();
        }
        if (ctx.now() >= this.pausedUntil) return false;

        ctx.body().stop();
        ctx.body().lookAt(watcher.eyes());
        // Keep heading for the same point. Dropping it made the route hand out the next one, so the
        // creek skipped points or turned around. Standing still on purpose is not being stuck.
        this.markProgress(ctx.now(), ctx.body().position());
        return true;
    }

    /**
     * How long to rest at the point just reached: the route's pause, or a random stop if that
     * is longer.
     */
    private long restMillis(CreekContext ctx, RouteStep reached) {
        CreekConfig config = ctx.config();
        int rest = reached.pauseMillis();
        if (config.randomStopChance() > 0.0D && ctx.random().nextDouble() < config.randomStopChance()) {
            int spread = config.randomStopMaxMillis() - config.randomStopMinMillis();
            rest = Math.max(rest, config.randomStopMinMillis() + ctx.random().nextInt(spread + 1));
        }
        return rest;
    }

    private boolean needsNewGoal(CreekContext ctx, Pos here) {
        RouteStep current = this.goal;
        if (current == null || here.distance(current.target()) < ARRIVED) return true;
        if (here.distance(this.progressAt) >= PROGRESS) {
            this.markProgress(ctx.now(), here);
            return false;
        }
        return ctx.now() - this.progressSince >= ctx.config().stuckMillis();
    }

    /**
     * Shows the creek to exactly the current survivors. Anyone who stops being one (dead, or now
     * the slender) loses sight of it right away.
     */
    private void showToSurvivors(CreekContext ctx) {
        Set<UUID> survivors = ctx.survivorIds();
        if (survivors.equals(this.shownTo)) return;
        this.shownTo = survivors;
        ctx.body().showTo(survivors);
    }

    private void markProgress(long now, Pos here) {
        this.progressAt = here;
        this.progressSince = now;
    }
}
