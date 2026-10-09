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
import java.util.function.BiConsumer;
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
        return actions(caught, selected, vanished, (_, _) -> {}, _ -> {});
    }

    /** Actions that hand everything but the stalk sounds to the given consumers. */
    public static CreekActions actions(Consumer<UUID> caught, Consumer<UUID> selected, Consumer<Pos> vanished,
                                       BiConsumer<UUID, Integer> stareBeats, Consumer<UUID> staresBroken) {
        return actions(caught, selected, vanished, stareBeats, staresBroken, (_, _) -> {});
    }

    /** Actions that hand everything the creek does to the given consumers. */
    public static CreekActions actions(Consumer<UUID> caught, Consumer<UUID> selected, Consumer<Pos> vanished,
                                       BiConsumer<UUID, Integer> stareBeats, Consumer<UUID> staresBroken,
                                       BiConsumer<UUID, Double> stalkSounds) {
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

            @Override
            public void stareBeat(UUID survivor, int beat) {
                stareBeats.accept(survivor, beat);
            }

            @Override
            public void stareBroken(UUID survivor) {
                staresBroken.accept(survivor);
            }

            @Override
            public void stalkSound(UUID survivor, double progress) {
                stalkSounds.accept(survivor, progress);
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
                new Random(7), HuntCooldowns.none());
    }

    static CreekContext context(long now, CreekBody body, RouteProvider route, CreekConfig config,
                                SurvivorView... survivors) {
        return new CreekContext(now, List.of(survivors), body, route, SPOTS, NO_ACTIONS, config, new Random(7), HuntCooldowns.none());
    }

    /** A context with the given actions and random source. */
    static CreekContext context(long now, CreekBody body, RouteProvider route, CreekActions actions,
                                RandomGenerator random, SurvivorView... survivors) {
        return new CreekContext(now, List.of(survivors), body, route, SPOTS, actions, CONFIG, random, HuntCooldowns.none());
    }

    /** A context that keeps the hunt breathers in the given record and every catch in the list. */
    static CreekContext hunting(long now, CreekBody body, HuntCooldowns hunts, List<UUID> caught,
                                SurvivorView... survivors) {
        return new CreekContext(now, List.of(survivors), body, route(), SPOTS, actions(caught::add, _ -> {}), CONFIG,
                new Random(7), hunts);
    }

    /** A context that records every survivor the state selects. */
    static CreekContext selecting(long now, CreekBody body, RouteProvider route, List<UUID> selected,
                                  SurvivorView... survivors) {
        return new CreekContext(now, List.of(survivors), body, route, SPOTS, actions(_ -> {}, selected::add), CONFIG,
                new Random(7), HuntCooldowns.none());
    }

    /** The defaults with other random stops. */
    static CreekConfig randomStops(double chance, int minMillis, int maxMillis) {
        return copy(CreekConfig.DEFAULT.activeWithLastSurvivor(), chance, minMillis, maxMillis);
    }

    /** The defaults, but the creek sits out the round once only one survivor is left. */
    public static CreekConfig withoutLastSurvivor() {
        CreekConfig defaults = CreekConfig.DEFAULT;
        return copy(false, defaults.randomStopChance(), defaults.randomStopMinMillis(), defaults.randomStopMaxMillis());
    }

    private static CreekConfig copy(boolean activeWithLastSurvivor, double chance, int minMillis, int maxMillis) {
        CreekConfig defaults = CreekConfig.DEFAULT;
        return new CreekConfig(
                defaults.enabled(), activeWithLastSurvivor, defaults.sightRange(), defaults.sightViewAngle(),
                defaults.wanderPauseMillis(), defaults.wanderSpeed(), defaults.huntSpeed(), defaults.stalkThreshold(), defaults.huntThreshold(),
                defaults.stalkMinDistance(), defaults.stalkMaxDistance(), defaults.stalkMinAngle(), defaults.stalkMaxAngle(),
                defaults.stalkRevealMillis(), defaults.stalkMinSeconds(), defaults.stalkMaxSeconds(), defaults.huntMaxSeconds(),
                defaults.huntMinStalkSeconds(), defaults.huntCooldownSeconds(),
                defaults.catchDistance(), defaults.vanishMinSeconds(), defaults.vanishMaxSeconds(), defaults.respawnMinDistance(),
                defaults.personalSpace(), defaults.stuckMillis(), defaults.betrayalCatchCount(),
                defaults.betrayalChance(), defaults.betrayalGlowSeconds(), defaults.slownessSeconds(), defaults.routeLinkDistance(),
                chance, minMillis, maxMillis, defaults.launchHeight(), defaults.swapChance(), defaults.launchDamage());
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
