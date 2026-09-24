package net.onelitefeather.cygnus.creek;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.common.config.CreekConfig;
import net.onelitefeather.cygnus.creek.body.CreekBody;
import net.onelitefeather.cygnus.creek.consequence.CatchConsequence;
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
import java.util.UUID;
import java.util.random.RandomGenerator;

/**
 * Connects the states to the server. Each step it turns the players into survivor snapshots and
 * runs the current state.
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
    private final CreekConfig config;
    private final RandomGenerator random;
    private CreekState state;
    private List<SurvivorView> lastViews = List.of();
    private boolean entered;

    Creek(CreekBody body, CreekSight sight, DreadSource dread, RouteProvider route, SpotFinder spots,
            CatchConsequence consequence, CreekConfig config, RandomGenerator random, CreekState initial) {
        this.body = body;
        this.sight = sight;
        this.dread = dread;
        this.route = route;
        this.spots = spots;
        this.consequence = consequence;
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
        Map<UUID, Player> players = new HashMap<>();
        for (Player survivor : survivors) {
            players.put(survivor.getUuid(), survivor);
        }
        this.lastViews = this.views(survivors);
        CreekContext ctx = new CreekContext(now, this.lastViews, this.body, this.route, this.spots,
                id -> {
                    Player caught = players.get(id);
                    if (caught != null) this.consequence.apply(caught);
                },
                this.config, this.random);

        if (!this.entered) {
            this.state.enter(ctx);
            this.entered = true;
        }
        if (!this.config.activeWithLastSurvivor() && ctx.survivors().size() <= 1) {
            if (!(this.state instanceof VanishState vanish && vanish.isForever())) {
                this.switchTo(VanishState.forever(), ctx);
            }
            return;
        }

        CreekState next = this.state.tick(ctx);
        if (next != this.state) this.switchTo(next, ctx);
    }

    /**
     * Creates a snapshot of every survivor for the states.
     * <p>
     * A survivor only counts as seeing the creek if it is visible to them. During a stalk, the
     * other survivors cannot see it, so they must not scare it off.
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
     * Returns the survivor snapshots from the last step.
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
