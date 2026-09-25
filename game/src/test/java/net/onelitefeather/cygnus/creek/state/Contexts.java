package net.onelitefeather.cygnus.creek.state;

import net.minestom.server.coordinate.Pos;
import net.onelitefeather.cygnus.common.config.CreekConfig;
import net.onelitefeather.cygnus.creek.body.CreekBody;
import net.onelitefeather.cygnus.creek.world.CreekSight;
import net.onelitefeather.cygnus.creek.world.RouteProvider;
import net.onelitefeather.cygnus.creek.world.RouteStep;
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

    /** The defaults, without random stops, so wandering tests do not depend on chance. */
    static final CreekConfig CONFIG = randomStops(0.0D, 0, 0);
    static final CreekSight SIGHT = new CreekSight(CONFIG.sightRange(), CONFIG.sightViewAngle());
    static final SpotFinder SPOTS = new SpotFinder(SIGHT, Optional::of);

    static CreekContext context(long now, CreekBody body, RouteProvider route, List<UUID> caught,
                                  SurvivorView... survivors) {
        return new CreekContext(now, List.of(survivors), body, route, SPOTS, caught::add, CONFIG, new Random(7));
    }

    static CreekContext context(long now, CreekBody body, RouteProvider route, CreekConfig config,
                                SurvivorView... survivors) {
        return new CreekContext(now, List.of(survivors), body, route, SPOTS, _ -> {}, config, new Random(7));
    }

    /** The defaults with other random stops. */
    static CreekConfig randomStops(double chance, int minMillis, int maxMillis) {
        CreekConfig d = CreekConfig.DEFAULT;
        return new CreekConfig(
                d.enabled(), d.activeWithLastSurvivor(), d.sightRange(), d.sightViewAngle(),
                d.wanderPauseMillis(), d.wanderSpeed(), d.huntSpeed(), d.stalkThreshold(), d.huntThreshold(),
                d.stalkMinDistance(), d.stalkMaxDistance(), d.stalkMinAngle(), d.stalkMaxAngle(),
                d.stalkRevealMillis(), d.stalkMinSeconds(), d.stalkMaxSeconds(), d.huntMaxSeconds(),
                d.catchDistance(), d.vanishMinSeconds(), d.vanishMaxSeconds(), d.respawnMinDistance(),
                d.personalSpace(), d.stuckMillis(), d.dreadPageWeight(), d.dreadTimeWeight(),
                d.dreadIsolationWeight(), d.isolationRadius(), d.betrayalCatchCount(),
                d.betrayalChance(), d.betrayalGlowSeconds(), d.slownessSeconds(), d.routeLinkDistance(),
                chance, minMillis, maxMillis);
    }

    /** A route that offers the first allowed point at least a block away. */
    public static RouteProvider route(Pos... points) {
        return new RouteProvider() {
            @Override
            public Optional<RouteStep> next(Pos current, java.util.function.Predicate<Pos> allowed,
                                            java.util.random.RandomGenerator random) {
                return Arrays.stream(points)
                        .filter(point -> point.distance(current) >= 1.0D)
                        .filter(allowed)
                        .findFirst()
                        .map(point -> new RouteStep(point, 0));
            }

            @Override
            public List<Pos> points() {
                return List.of(points);
            }
        };
    }

    private Contexts() {
    }
}
