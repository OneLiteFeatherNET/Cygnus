package net.onelitefeather.cygnus.creek.state;

import net.minestom.server.coordinate.Pos;
import net.onelitefeather.cygnus.common.config.CreekConfig;
import net.onelitefeather.cygnus.creek.body.CreekBody;
import net.onelitefeather.cygnus.creek.world.CreekSight;
import net.onelitefeather.cygnus.creek.world.RouteProvider;
import net.onelitefeather.cygnus.creek.world.SpotFinder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/**
 * Builds contexts for the state tests: the design's settings, ground everywhere, fixed chance.
 */
public final class Contexts {

    static final CreekConfig CONFIG = CreekConfig.DEFAULT;
    static final CreekSight SIGHT = new CreekSight(CONFIG.sightRange(), CONFIG.sightViewAngle());
    static final SpotFinder SPOTS = new SpotFinder(SIGHT, Optional::of);

    static CreekContext context(long now, CreekBody body, RouteProvider route, List<UUID> caught,
                                  SurvivorView... survivors) {
        return new CreekContext(now, List.of(survivors), body, route, SPOTS, caught::add, CONFIG, new Random(7));
    }

    /** A route that offers the first allowed point at least a block away. */
    public static RouteProvider route(Pos... points) {
        return (current, allowed, _) -> Arrays.stream(points)
                .filter(point -> point.distance(current) >= 1.0D)
                .filter(allowed)
                .findFirst();
    }

    private Contexts() {
    }
}
