package net.onelitefeather.cygnus.creek.state;

import net.minestom.server.coordinate.Pos;
import net.onelitefeather.cygnus.common.config.CreekConfig;
import net.onelitefeather.cygnus.creek.body.CreekBody;
import net.onelitefeather.cygnus.creek.world.RouteStep;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The creek walks around the map. Every survivor can see it.
 * <p>
 * When someone looks at it, it stops for a moment and looks back. When a survivor's dread is
 * high enough, it switches to stalking them.
 * After reaching a waypoint it may rest there: for the pause the route sets, or for a random stop,
 * whichever is longer.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class WanderState implements CreekState {

    /** Distance at which a waypoint counts as reached, in blocks. */
    static final double ARRIVED = 2.0D;

    /** How far the creek has to move to count as progress, in blocks. */
    static final double PROGRESS = 0.5D;

    private final long stalkAllowedAt;
    private @Nullable Pos goal;
    private boolean watched;
    private long pausedUntil;
    private int goalPauseMillis;
    private long restingUntil;
    private Pos progressAt = Pos.ZERO;
    private long progressSince;
    private Set<UUID> shownTo = Set.of();

    /**
     * Creates the state.
     *
     * @param stalkAllowedAt the earliest time the creek may start stalking, in milliseconds
     */
    public WanderState(long stalkAllowedAt) {
        this.stalkAllowedAt = stalkAllowedAt;
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

        if (!ctx.farFromAll(here, config.personalSpace())) return VanishState.after(ctx);

        if (ctx.now() >= this.stalkAllowedAt) {
            Optional<CreekState> stalk = pickTarget(ctx).flatMap(target -> startStalking(ctx, target));
            if (stalk.isPresent()) return stalk.get();
        }

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
        if (this.hasArrived(here)) {
            long rest = this.restMillis(ctx);
            this.goal = null;
            if (rest > 0) {
                this.restingUntil = ctx.now() + rest;
                body.stop();
                return this;
            }
        }

        if (this.needsNewGoal(ctx, here)) {
            Optional<RouteStep> step = ctx.route()
                    .next(here, point -> ctx.farFromAll(point, config.personalSpace()), ctx.random());
            this.goal = step.map(RouteStep::target).orElse(null);
            this.goalPauseMillis = step.map(RouteStep::pauseMillis).orElse(0);
            this.markProgress(ctx.now(), here);
        }
        if (this.goal == null) {
            body.stop();
            return this;
        }
        body.moveTo(this.goal, config.wanderSpeed());
        return this;
    }

    /**
     * Stops and looks back for a moment when someone spots the creek.
     *
     * @return {@code true} while the pause lasts
     */
    private boolean pause(CreekContext ctx, SurvivorView watcher) {
        if (!this.watched) {
            this.watched = true;
            this.pausedUntil = ctx.now() + ctx.config().wanderPauseMillis();
        }
        if (ctx.now() >= this.pausedUntil) return false;

        ctx.body().stop();
        ctx.body().lookAt(watcher.eyes());
        this.goal = null;
        return true;
    }

    private boolean hasArrived(Pos here) {
        return this.goal != null && here.distance(this.goal) < ARRIVED;
    }

    /**
     * Works out how long to rest at the point just reached: its pause, or a random stop if that
     * is longer.
     */
    private long restMillis(CreekContext ctx) {
        CreekConfig config = ctx.config();
        int rest = this.goalPauseMillis;
        if (config.randomStopChance() > 0.0D && ctx.random().nextDouble() < config.randomStopChance()) {
            int spread = config.randomStopMaxMillis() - config.randomStopMinMillis();
            rest = Math.max(rest, config.randomStopMinMillis() + ctx.random().nextInt(spread + 1));
        }
        return rest;
    }

    private boolean needsNewGoal(CreekContext ctx, Pos here) {
        if (this.goal == null || here.distance(this.goal) < ARRIVED) return true;
        if (!ctx.farFromAll(this.goal, ctx.config().personalSpace())) return true;
        if (here.distance(this.progressAt) >= PROGRESS) {
            this.markProgress(ctx.now(), here);
            return false;
        }
        return ctx.now() - this.progressSince >= ctx.config().stuckMillis();
    }

    /**
     * Makes the creek visible to exactly the current survivors. A player who is no longer a
     * survivor (dead, or now the slender) stops seeing it right away.
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

    /**
     * Picks the survivor to stalk: the one with the highest dread above the threshold. On a tie,
     * the one farthest from the others.
     */
    static Optional<SurvivorView> pickTarget(CreekContext ctx) {
        double threshold = ctx.config().stalkThreshold();
        return ctx.survivors().stream()
                .filter(view -> view.dread() >= threshold)
                .max(Comparator.comparingDouble(SurvivorView::dread)
                        .thenComparingDouble(view -> nearestOther(ctx, view)));
    }

    private static double nearestOther(CreekContext ctx, SurvivorView view) {
        double nearest = Double.MAX_VALUE;
        for (SurvivorView other : ctx.survivors()) {
            if (other.id().equals(view.id())) continue;
            nearest = Math.min(nearest, other.position().distance(view.position()));
        }
        return nearest;
    }

    private static Optional<CreekState> startStalking(CreekContext ctx, SurvivorView target) {
        CreekConfig config = ctx.config();
        return ctx.spots()
                .beside(target.position(), config.stalkMinDistance(), config.stalkMaxDistance(),
                        config.stalkMinAngle(), config.stalkMaxAngle(), List.of(target.eyes()),
                        config.personalSpace(), ctx.random())
                .map(spot -> {
                    ctx.body().teleport(spot);
                    return StalkState.starting(target.id(), ctx);
                });
    }
}
