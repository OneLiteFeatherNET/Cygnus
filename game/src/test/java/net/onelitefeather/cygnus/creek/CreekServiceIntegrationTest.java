package net.onelitefeather.cygnus.creek;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.ActionBarPacket;
import net.minestom.server.potion.PotionEffect;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.config.CreekConfig;
import net.onelitefeather.cygnus.common.creek.CreekRoute;
import net.onelitefeather.cygnus.creek.body.CreakingBody;
import net.onelitefeather.cygnus.creek.consequence.CatchConsequence;
import net.onelitefeather.cygnus.creek.debug.CreekDebug;
import net.onelitefeather.cygnus.creek.state.PatrolState;
import net.onelitefeather.cygnus.creek.state.VanishState;
import net.onelitefeather.cygnus.creek.world.PathRoute;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreekServiceIntegrationTest extends CygnusPlayerTestBase {

    private final AtomicInteger cleanUps = new AtomicInteger();

    private final CatchConsequence consequence = new CatchConsequence() {
        @Override
        public void apply(Player survivor) {
        }

        @Override
        public void cleanUp() {
            cleanUps.incrementAndGet();
        }
    };

    private static final CreekRoute ROUTE = CreekRoute.ofPositions("Test", List.of(new Vec(10, 40, 10), new Vec(20, 40, 10)));

    private CreekService service(Instance instance, Set<Player> survivors, List<CreekRoute> routes, AtomicLong clock) {
        return new CreekService(CreekConfig.DEFAULT, () -> survivors, () -> instance, () -> routes,
                CreakingBody::spawn, (_, _, _) -> 0.0D, consequence, new RoundClock(clock::get), new Random(3), new CreekDebug());
    }

    @Test
    @DisplayName("At the start he is in the world, but out of sight")
    void startsOutOfSight(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0));
        CreekService service = service(instance, Set.of(survivor), List.of(ROUTE), new AtomicLong());

        service.start();
        service.tick();

        Creek creek = service.creek();
        assertNotNull(creek);
        assertInstanceOf(VanishState.class, creek.state());
        assertFalse(creek.body().isVisibleTo(survivor.getUuid()));
        service.stop();
    }

    @Test
    @DisplayName("At the end he is removed and every consequence is cleaned up")
    void stopRemovesHimAndCleansUp(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0));
        CreekService service = service(instance, Set.of(survivor), List.of(ROUTE), new AtomicLong());
        service.start();
        Creek creek = service.creek();
        assertNotNull(creek);

        service.stop();

        assertTrue(creek.body().entity().isRemoved());
        assertNull(service.creek());
        assertEquals(1, cleanUps.get());
    }

    @Test
    @DisplayName("Without a usable route the creek stays away")
    void staysAwayWithoutRoutes(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0));
        CreekService service = service(instance, Set.of(survivor), List.of(), new AtomicLong());

        service.start();

        assertNull(service.creek());
    }

    @Test
    @DisplayName("The round time counts from the start")
    void roundTimeCountsFromTheStart(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0));
        AtomicLong clock = new AtomicLong(1000L);
        RoundClock roundClock = new RoundClock(clock::get);
        CreekService service = new CreekService(CreekConfig.DEFAULT, () -> Set.of(survivor), () -> instance,
                () -> List.of(ROUTE), CreakingBody::spawn, (_, _, _) -> 0.0D, consequence, roundClock, new Random(3), new CreekDebug());

        service.start();
        clock.set(4000L);

        assertEquals(3000L, roundClock.elapsedMillis());
        service.stop();
        assertEquals(0L, roundClock.elapsedMillis());
    }

    @Test
    @DisplayName("Only players watching the debug get the line")
    void debugGoesToWatchersOnly(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection watcherConnection = env.createConnection();
        Player watcher = watcherConnection.connect(instance, new Pos(0, 40, 0));
        TestConnection otherConnection = env.createConnection();
        Player other = otherConnection.connect(instance, new Pos(3, 40, 0));
        CreekDebug debug = new CreekDebug();
        debug.toggle(watcher.getUuid());
        CreekService service = new CreekService(CreekConfig.DEFAULT, () -> Set.of(watcher, other), () -> instance,
                () -> List.of(ROUTE), CreakingBody::spawn, (_, _, _) -> 0.0D, consequence,
                new RoundClock(new AtomicLong()::get), new Random(3), debug);
        Collector<ActionBarPacket> watched = watcherConnection.trackIncoming(ActionBarPacket.class);
        Collector<ActionBarPacket> unwatched = otherConnection.trackIncoming(ActionBarPacket.class);

        service.start();
        service.tick();

        assertFalse(watched.collect().isEmpty());
        assertTrue(unwatched.collect().isEmpty());
        service.stop();
    }

    @Test
    @DisplayName("With routes on the map the creek walks them")
    void routesAreUsedWhenThereAreAny(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0));
        CreekService service = service(instance, Set.of(survivor), List.of(ROUTE), new AtomicLong());

        service.start();

        assertInstanceOf(PathRoute.class, service.route());
        assertNotNull(service.creek());
        service.stop();
    }

    @Test
    @DisplayName("After the start delay he patrols and a scared survivor gets a variant")
    void patrolsAndStartsAVariant(Env env) {
        Instance instance = env.createFlatInstance();
        // Far from the route points, so the vanish finds a hidden spot to come back at.
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, -60, 0, 0));
        Player other = env.createConnection().connect(instance, new Pos(0, 40, -160, 0, 0));
        AtomicLong clock = new AtomicLong();
        CreekService service = new CreekService(CreekConfig.DEFAULT, () -> Set.of(survivor, other), () -> instance,
                () -> List.of(ROUTE), CreakingBody::spawn,
                (id, _, _) -> id.equals(survivor.getUuid()) ? 0.5D : 0.0D, consequence,
                new RoundClock(clock::get), new Random(3), new CreekDebug());
        service.start();

        clock.set(CreekConfig.DEFAULT.vanishMaxSeconds() * 1000L + 1L);
        service.tick();
        service.tick();

        Creek creek = service.creek();
        assertNotNull(creek);
        assertInstanceOf(PatrolState.class, creek.state());
        CreekVariants variants = service.variants();
        assertNotNull(variants);
        assertTrue(variants.running().containsKey(survivor.getUuid()));
        service.stop();
    }

    @Test
    @DisplayName("At the end every variant is removed and every consequence is cleaned up")
    void stopRemovesVariantsAndCleansUp(Env env) {
        Instance instance = env.createFlatInstance();
        // Far from the route points, so the vanish finds a hidden spot to come back at.
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, -60, 0, 0));
        Player other = env.createConnection().connect(instance, new Pos(0, 40, -160, 0, 0));
        AtomicLong clock = new AtomicLong();
        CreekService service = new CreekService(CreekConfig.DEFAULT, () -> Set.of(survivor, other), () -> instance,
                () -> List.of(ROUTE), CreakingBody::spawn,
                (id, _, _) -> id.equals(survivor.getUuid()) ? 0.5D : 0.0D, consequence,
                new RoundClock(clock::get), new Random(3), new CreekDebug());
        service.start();
        clock.set(CreekConfig.DEFAULT.vanishMaxSeconds() * 1000L + 1L);
        service.tick();
        service.tick();
        CreekVariants variants = service.variants();
        assertNotNull(variants);
        Creek variant = variants.running().get(survivor.getUuid());
        assertNotNull(variant);

        service.stop();

        assertTrue(variant.body().entity().isRemoved());
        assertNull(service.variants());
        assertEquals(1, cleanUps.get());
    }

    @Test
    @DisplayName("At the end no stun from the patrol is left on a survivor")
    void stopClearsTheSelectionEffects(Env env) {
        Instance instance = env.createFlatInstance();
        // far from every route point: the teleport has no target, so a selection always stuns
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, -60, 0, 0));
        AtomicLong clock = new AtomicLong();
        CreekService service = service(instance, Set.of(survivor), List.of(ROUTE), clock);
        service.start();
        clock.set(CreekConfig.DEFAULT.vanishMaxSeconds() * 1000L + 1L);
        service.tick();
        Creek creek = service.creek();
        assertNotNull(creek);
        assertInstanceOf(PatrolState.class, creek.state());

        creek.body().teleport(new Pos(0, 40, -58));
        service.tick();
        clock.addAndGet(1000L);
        service.tick();
        assertTrue(survivor.hasEffect(PotionEffect.SLOWNESS), "the patrol stunned the survivor");

        service.stop();

        assertFalse(survivor.hasEffect(PotionEffect.SLOWNESS));
    }

    @Test
    @DisplayName("Once a survivor has a variant, the patrolling creek is hidden from them")
    void patrolIsHiddenFromTheHaunted(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, -60, 0, 0));
        Player other = env.createConnection().connect(instance, new Pos(0, 40, -160, 0, 0));
        AtomicLong clock = new AtomicLong();
        CreekService service = new CreekService(CreekConfig.DEFAULT, () -> Set.of(survivor, other), () -> instance,
                () -> List.of(ROUTE), CreakingBody::spawn,
                (id, _, _) -> id.equals(survivor.getUuid()) ? 0.5D : 0.0D, consequence,
                new RoundClock(clock::get), new Random(3), new CreekDebug());
        service.start();
        clock.set(CreekConfig.DEFAULT.vanishMaxSeconds() * 1000L + 1L);
        service.tick();
        Creek creek = service.creek();
        assertNotNull(creek);
        assertTrue(creek.body().isVisibleTo(survivor.getUuid()), "before the variant everyone sees the patrol");

        service.tick();

        assertFalse(creek.body().isVisibleTo(survivor.getUuid()));
        assertTrue(creek.body().isVisibleTo(other.getUuid()));
        service.stop();
    }
}
