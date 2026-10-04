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
import java.util.random.RandomGenerator;

/**
 * Everything a state needs for one step.
 * <p>
 * A fresh context is built for every step. It holds no server objects, so the states can be tested
 * without a running server.
 * </p>
 *
 * @param now       the current time in milliseconds
 * @param survivors the survivors of the round
 * @param body      the creek's body
 * @param route     decides where the creek walks next
 * @param spots     finds places where nobody can see the creek
 * @param actions   what the states can do to a survivor
 * @param config    the settings
 * @param random    the random source
 * @param hunts     when each survivor's last hunt ended, shared by every creek of the round
 *
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
        CreekActions actions,
        CreekConfig config,
        RandomGenerator random,
        HuntCooldowns hunts
) {

    /**
     * Looks up a survivor by id.
     *
     * @param id the id to look for
     * @return the survivor, or empty if they are no longer in the round
     */
    public Optional<SurvivorView> survivor(UUID id) {
        for (SurvivorView view : this.survivors) {
            if (view.id().equals(id)) return Optional.of(view);
        }
        return Optional.empty();
    }

    /**
     * The ids of every survivor.
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
     * Where every survivor's eyes are.
     *
     * @return the eye positions
     */
    public List<Pos> observerEyes() {
        return this.survivors.stream().map(SurvivorView::eyes).toList();
    }

    /**
     * The highest dread among the survivors.
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
     * @param distance the distance to keep, in blocks
     * @return {@code true} if every survivor is at least that far away
     */
    public boolean farFromAll(Pos point, double distance) {
        for (SurvivorView view : this.survivors) {
            if (view.position().distance(point) < distance) return false;
        }
        return true;
    }
}
