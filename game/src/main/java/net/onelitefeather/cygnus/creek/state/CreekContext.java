package net.onelitefeather.cygnus.creek.state;

import net.minestom.server.coordinate.Pos;
import net.onelitefeather.cygnus.common.config.CreekConfig;
import net.onelitefeather.cygnus.creek.body.CreekBody;
import net.onelitefeather.cygnus.creek.world.RouteProvider;
import net.onelitefeather.cygnus.creek.world.SpotFinder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;

/**
 * Everything a state needs for one step.
 * <p>
 * A new context is built for every step. It holds no server objects, so the states can be
 * tested without a running server.
 * </p>
 *
 * @param now       the current time in milliseconds
 * @param survivors the survivors of the round
 * @param body      the creek's body
 * @param route     picks where the creek walks to
 * @param spots     finds places where nobody sees the creek
 * @param onCatch   called with the id of a caught survivor
 * @param config    the settings
 * @param random    the random source
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public record CreekContext(
        long now,
        List<SurvivorView> survivors,
        CreekBody body,
        RouteProvider route,
        SpotFinder spots,
        Consumer<UUID> onCatch,
        CreekConfig config,
        RandomGenerator random
) {

    /**
     * Finds a survivor by id.
     *
     * @param id the id to look for
     * @return the survivor, or empty if they left the round
     */
    public Optional<SurvivorView> survivor(UUID id) {
        for (SurvivorView view : this.survivors) {
            if (view.id().equals(id)) return Optional.of(view);
        }
        return Optional.empty();
    }

    /**
     * Returns the ids of every survivor.
     *
     * @return the ids
     */
    public Set<UUID> survivorIds() {
        Set<UUID> ids = new HashSet<>();
        for (SurvivorView view : this.survivors) {
            ids.add(view.id());
        }
        return Set.copyOf(ids);
    }

    /**
     * Returns the eyes of every survivor.
     *
     * @return the eye positions
     */
    public List<Pos> observerEyes() {
        return this.survivors.stream().map(SurvivorView::eyes).toList();
    }

    /**
     * Returns the highest dread among the survivors.
     *
     * @return the dread, {@code 0} without survivors
     */
    public double highestDread() {
        return this.survivors.stream().mapToDouble(SurvivorView::dread).max().orElse(0.0D);
    }

    /**
     * Checks that no survivor is closer to a point than the given distance.
     *
     * @param point    the point to check
     * @param distance the minimum distance, in blocks
     * @return {@code true} if every survivor is at least that far away
     */
    public boolean farFromAll(Pos point, double distance) {
        for (SurvivorView view : this.survivors) {
            if (view.position().distance(point) < distance) return false;
        }
        return true;
    }
}
