package net.onelitefeather.cygnus.creek;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.common.config.CreekConfig;
import net.onelitefeather.cygnus.creek.body.CreekBody;
import net.onelitefeather.cygnus.creek.consequence.CatchConsequence;
import net.onelitefeather.cygnus.creek.consequence.SelectionConsequence;
import net.onelitefeather.cygnus.creek.dread.DreadSource;
import net.onelitefeather.cygnus.creek.state.CreekContext;
import net.onelitefeather.cygnus.creek.state.CreekState;
import net.onelitefeather.cygnus.creek.state.SurvivorView;
import net.onelitefeather.cygnus.creek.state.VanishState;
import net.onelitefeather.cygnus.creek.world.CreekSight;
import net.onelitefeather.cygnus.creek.world.RouteProvider;
import net.onelitefeather.cygnus.creek.world.SpotFinder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.random.RandomGenerator;

/**
 * One creek in the round: its body, what it is doing right now, and the step that moves it on.
 * <p>
 * The patrolling creek is one of these, and so is every variant. Each step it takes a snapshot of
 * the survivors and lets its current state decide what happens next.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
final class Creek {

    private final CreekBody body;
    private final CreekSight sight;
    private final DreadSource dread;
    private final RouteProvider route;
    private final SpotFinder spots;
    private final CatchConsequence consequence;
    private final SelectionConsequence selection;
    private final CreekConfig config;
    private final RandomGenerator random;
    private CreekState state;
    private List<SurvivorView> lastViews = List.of();
    private boolean entered;

    Creek(CreekBody body, CreekSight sight, DreadSource dread, RouteProvider route, SpotFinder spots,
            CatchConsequence consequence, SelectionConsequence selection, CreekConfig config, RandomGenerator random,
            CreekState initial) {
        this.body = body;
        this.sight = sight;
        this.dread = dread;
        this.route = route;
        this.spots = spots;
        this.consequence = consequence;
        this.selection = selection;
        this.config = config;
        this.random = random;
        this.state = initial;
    }

    /**
     * Runs one step.
     *
     * @param survivors the survivors of the round
     * @param now       the current time in milliseconds
     */
    void tick(Collection<Player> survivors, long now) {
        this.tick(survivors, Set.of(), now);
    }

    /**
     * Runs one step, leaving some survivors out of it. The patrolling creek uses this for everyone
     * who is haunted by a variant: they do not see it, and it does not pick them out.
     * <p>
     * The snapshots in {@link #lastViews()} still cover every survivor.
     * </p>
     *
     * @param survivors the survivors of the round
     * @param ignored   the survivors this creek leaves alone in this step
     * @param now       the current time in milliseconds
     */
    void tick(Collection<Player> survivors, Set<UUID> ignored, long now) {
        Map<UUID, Player> players = new HashMap<>();
        for (Player survivor : survivors) {
            players.put(survivor.getUuid(), survivor);
        }
        this.lastViews = this.views(survivors);
        List<SurvivorView> noticed = ignored.isEmpty() ? this.lastViews
                : this.lastViews.stream().filter(view -> !ignored.contains(view.id())).toList();
        CreekContext ctx = new CreekContext(now, noticed, this.body, this.route, this.spots,
                id -> {
                    Player caught = players.get(id);
                    if (caught != null) this.consequence.apply(caught);
                },
                id -> {
                    Player selected = players.get(id);
                    if (selected != null) this.selection.apply(selected, survivors);
                },
                this.config, this.random);

        if (!this.entered) {
            this.state.enter(ctx);
            this.entered = true;
        }
        // Count every survivor here: leaving the haunted ones out is not being down to the last one.
        if (!this.config.activeWithLastSurvivor() && this.lastViews.size() <= 1) {
            if (!(this.state instanceof VanishState vanish && vanish.isForever())) {
                this.switchTo(VanishState.forever(), ctx);
            }
            return;
        }

        CreekState next = this.state.tick(ctx);
        if (next != this.state) this.switchTo(next, ctx);
    }

    /**
     * Takes a snapshot of every survivor for the states.
     * <p>
     * A survivor only counts as seeing the creek if it is shown to them at all. A variant is hidden
     * from everyone but its target, so the others must not be able to scare it off.
     * </p>
     *
     * @param survivors the survivors of the round
     * @return one view per survivor
     */
    List<SurvivorView> views(Collection<Player> survivors) {
        List<SurvivorView> views = new ArrayList<>(survivors.size());
        for (Player survivor : survivors) {
            Pos position = survivor.getPosition();
            List<Pos> others = new ArrayList<>();
            for (Player other : survivors) {
                if (other != survivor) others.add(other.getPosition());
            }
            boolean sees = this.body.isVisibleTo(survivor.getUuid()) && this.sight.sees(survivor, this.body.entity());
            views.add(new SurvivorView(survivor.getUuid(), position,
                    this.dread.dreadOf(survivor.getUuid(), position, others), sees));
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

    void remove() {
        this.body.remove();
    }

    private void switchTo(CreekState next, CreekContext ctx) {
        this.state = next;
        next.enter(ctx);
    }
}
