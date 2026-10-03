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
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

/**
 * Builds contexts for the state tests: the design's settings, ground everywhere, fixed chance.
 */
public final class Contexts {

    /** The defaults, without random stops, so wandering tests do not depend on chance. */
    static final CreekConfig CONFIG = randomStops(0.0D, 0, 0);
    static final CreekSight SIGHT = new CreekSight(CONFIG.sightRange(), CONFIG.sightViewAngle());
    static final SpotFinder SPOTS = new SpotFinder(SIGHT, Optional::of);

    /** Actions that do nothing. */
    public static final CreekActions NO_ACTIONS = actions(_ -> {}, _ -> {});

    /** Actions that hand every catch and selection to the given consumers. */
    public static CreekActions actions(Consumer<UUID> caught, Consumer<UUID> selected) {
        return actions(caught, selected, _ -> {});
    }

    /** Actions that hand every catch, selection and vanish to the given consumers. */
    public static CreekActions actions(Consumer<UUID> caught, Consumer<UUID> selected, Consumer<Pos> vanished) {
        return new CreekActions() {
            @Override
            public void caught(UUID survivor) {
                caught.accept(survivor);
            }

            @Override
            public void selected(UUID survivor) {
                selected.accept(survivor);
            }

            @Override
            public void vanished(Pos where) {
                vanished.accept(where);
            }
        };
    }

    /** A random source that answers every roll with the given value. */
    public static RandomGenerator rolling(double value) {
        return new RandomGenerator() {
            @Override
            public long nextLong() {
                return 0L;
            }

            @Override
            public double nextDouble() {
                return value;
            }
        };
    }

    static CreekContext context(long now, CreekBody body, RouteProvider route, List<UUID> caught,
                                  SurvivorView... survivors) {
        return new CreekContext(now, List.of(survivors), body, route, SPOTS, actions(caught::add, _ -> {}), CONFIG,
                new Random(7));
    }

    static CreekContext context(long now, CreekBody body, RouteProvider route, CreekConfig config,
                                SurvivorView... survivors) {
        return new CreekContext(now, List.of(survivors), body, route, SPOTS, NO_ACTIONS, config, new Random(7));
    }

    /** A context with the given actions and random source. */
    static CreekContext context(long now, CreekBody body, RouteProvider route, CreekActions actions,
                                RandomGenerator random, SurvivorView... survivors) {
        return new CreekContext(now, List.of(survivors), body, route, SPOTS, actions, CONFIG, random);
    }

    /** A context that records every survivor the state selects. */
    static CreekContext selecting(long now, CreekBody body, RouteProvider route, List<UUID> selected,
                                  SurvivorView... survivors) {
        return new CreekContext(now, List.of(survivors), body, route, SPOTS, actions(_ -> {}, selected::add), CONFIG,
                new Random(7));
    }

    /** The defaults with other random stops. */
    static CreekConfig randomStops(double chance, int minMillis, int maxMillis) {
        return copy(CreekConfig.DEFAULT.activeWithLastSurvivor(), chance, minMillis, maxMillis);
    }

    /** The defaults, but the creek sits out the round once only one survivor is left. */
    public static CreekConfig withoutLastSurvivor() {
        CreekConfig d = CreekConfig.DEFAULT;
        return copy(false, d.randomStopChance(), d.randomStopMinMillis(), d.randomStopMaxMillis());
    }

    private static CreekConfig copy(boolean activeWithLastSurvivor, double chance, int minMillis, int maxMillis) {
        CreekConfig d = CreekConfig.DEFAULT;
        return new CreekConfig(
                d.enabled(), activeWithLastSurvivor, d.sightRange(), d.sightViewAngle(),
                d.wanderPauseMillis(), d.wanderSpeed(), d.huntSpeed(), d.stalkThreshold(), d.huntThreshold(),
                d.stalkMinDistance(), d.stalkMaxDistance(), d.stalkMinAngle(), d.stalkMaxAngle(),
                d.stalkRevealMillis(), d.stalkMinSeconds(), d.stalkMaxSeconds(), d.huntMaxSeconds(),
                d.catchDistance(), d.vanishMinSeconds(), d.vanishMaxSeconds(), d.respawnMinDistance(),
                d.personalSpace(), d.stuckMillis(), d.betrayalCatchCount(),
                d.betrayalChance(), d.betrayalGlowSeconds(), d.slownessSeconds(), d.routeLinkDistance(),
                chance, minMillis, maxMillis);
    }

    /** A route that offers the first allowed point at least a block away. */
    public static RouteProvider route(Pos... points) {
        return new RouteProvider() {
            @Override
            public Optional<RouteStep> next(Pos current, Predicate<Pos> allowed,
                                            RandomGenerator random) {
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
