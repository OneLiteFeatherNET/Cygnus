package net.onelitefeather.cygnus.creek;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.creek.body.CreekBody;
import net.onelitefeather.cygnus.creek.state.CreekActions;
import net.onelitefeather.cygnus.creek.state.CreekContext;
import net.onelitefeather.cygnus.creek.state.CreekState;
import net.onelitefeather.cygnus.creek.state.SurvivorView;
import net.onelitefeather.cygnus.creek.state.VanishState;
import net.onelitefeather.cygnus.creek.world.RouteProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * One creek in the round: its body, what it is doing right now, and the step that moves it on.
 * <p>
 * The patrolling creek is one of these, and so is every variant. Each step it fills in who can see
 * it in the shared snapshot of the survivors and lets its current state decide what happens next.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
final class Creek {

    private final CreekBody body;
    private final RouteProvider route;
    private final CreekRound round;
    private CreekState state;
    private List<SurvivorView> lastViews = List.of();
    private boolean entered;

    /**
     * Sets up a creek.
     *
     * @param body    its body in the world
     * @param route   its own walker along the routes
     * @param round   what it shares with every other creek of the round
     * @param initial the state it starts in
     */
    Creek(CreekBody body, RouteProvider route, CreekRound round, CreekState initial) {
        this.body = body;
        this.route = route;
        this.round = round;
        this.state = initial;
    }

    /**
     * Runs one step.
     *
     * @param survivors the survivors of this step
     * @param now       the current time in milliseconds
     */
    void tick(SurvivorSnapshot survivors, long now) {
        this.tick(survivors, Set.of(), now);
    }

    /**
     * Runs one step, leaving some survivors out of it. The patrolling creek uses this for everyone
     * who is haunted by a variant: they do not see it, and it does not pick them out.
     * <p>
     * The snapshots in {@link #lastViews()} still cover every survivor.
     * </p>
     *
     * @param survivors the survivors of this step
     * @param ignored   the survivors this creek leaves alone in this step
     * @param now       the current time in milliseconds
     */
    void tick(SurvivorSnapshot survivors, Set<UUID> ignored, long now) {
        CreekContext ctx = this.context(survivors, ignored, now);
        if (!this.entered) {
            this.state.enter(ctx);
            this.entered = true;
        }
        CreekState next = this.state.tick(ctx);
        if (next != this.state) this.switchTo(next, ctx);
    }

    /**
     * Sends the creek away for the rest of the round. Calling it again changes nothing.
     *
     * @param survivors the survivors of this step
     * @param now       the current time in milliseconds
     */
    void vanishForGood(SurvivorSnapshot survivors, long now) {
        CreekContext ctx = this.context(survivors, Set.of(), now);
        if (this.state instanceof VanishState vanish && vanish.isForever()) return;
        // The state it had so far is never entered now, and never needs to be.
        this.entered = true;
        this.switchTo(VanishState.forever(), ctx);
    }

    /**
     * Fills in who sees this creek and builds the context for the states.
     */
    private CreekContext context(SurvivorSnapshot survivors, Set<UUID> ignored, long now) {
        this.lastViews = this.views(survivors);
        List<SurvivorView> noticed = ignored.isEmpty() ? this.lastViews
                : this.lastViews.stream().filter(view -> !ignored.contains(view.id())).toList();
        return new CreekContext(now, noticed, this.body, this.route, this.round.spots(),
                new Actions(this.round, survivors, ignored),
                this.round.config(), this.round.random());
    }

    /**
     * Tells for every survivor whether they see this creek.
     * <p>
     * A survivor only counts as seeing the creek if it is shown to them at all. A variant is hidden
     * from everyone but its target, so the others must not be able to scare it off.
     * </p>
     *
     * @param survivors the survivors of this step
     * @return one view per survivor
     */
    List<SurvivorView> views(SurvivorSnapshot survivors) {
        List<Player> players = survivors.players();
        List<SurvivorView> base = survivors.views();
        List<SurvivorView> views = new ArrayList<>(players.size());
        for (int index = 0; index < players.size(); index++) {
            Player survivor = players.get(index);
            boolean sees = this.body.isVisibleTo(survivor.getUuid())
                    && this.round.sight().sees(survivor, this.body.entity());
            views.add(base.get(index).withSeesCreek(sees));
        }
        return views;
    }

    /**
     * The survivor snapshots from the last step.
     *
     * @return the views, empty before the first step
     */
    List<SurvivorView> lastViews() {
        return this.lastViews;
    }

    CreekState state() {
        return this.state;
    }

    CreekBody body() {
        return this.body;
    }

    RouteProvider route() {
        return this.route;
    }

    void remove() {
        this.body.remove();
    }

    private void switchTo(CreekState next, CreekContext ctx) {
        this.state = next;
        next.enter(ctx);
    }

    /**
     * Applies the consequences to the survivors of one step. Anyone who is no longer among them is
     * left alone, and so is everyone this creek ignores in that step.
     *
     * @param round     what the creek shares with the others of the round
     * @param survivors the survivors of the step
     * @param ignored   the survivors the creek leaves alone in the step
     */
    private record Actions(CreekRound round, SurvivorSnapshot survivors, Set<UUID> ignored) implements CreekActions {

        @Override
        public void caught(UUID survivor) {
            Player player = this.survivors.player(survivor);
            if (player != null) this.round.consequence().apply(player);
        }

        @Override
        public void selected(UUID survivor) {
            Player player = this.survivors.player(survivor);
            if (player != null) this.round.patrol().selected(player, this.survivors.players());
        }

        @Override
        public void vanished(Pos where) {
            // Whoever this creek leaves alone does not see it, so they do not see it vanish either.
            List<Player> noticing = this.survivors.players().stream()
                    .filter(player -> !this.ignored.contains(player.getUuid()))
                    .toList();
            this.round.patrol().vanished(where, noticing);
        }
    }
}
