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
import net.onelitefeather.cygnus.creek.state.Contexts;
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
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
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

        Creek creek = service.creek();
        assertNotNull(creek);
        assertInstanceOf(PathRoute.class, creek.route());
        service.stop();
    }

    @Test
    @DisplayName("At the end he and every variant are removed and every consequence is cleaned up")
    void stopRemovesEverythingAndCleansUp(Env env) {
        Round round = this.roundWithVariant(env, CreekConfig.DEFAULT);
        Creek creek = round.service().creek();
        CreekVariants variants = round.service().variants();
        assertNotNull(creek);
        assertNotNull(variants);
        Creek variant = variants.running().get(round.scared().getUuid());
        assertNotNull(variant);

        round.service().stop();

        assertTrue(creek.body().entity().isRemoved());
        assertTrue(variant.body().entity().isRemoved());
        assertNull(round.service().creek());
        assertNull(round.service().variants());
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
    @DisplayName("After the start delay he patrols, a scared survivor gets a variant and no longer sees the patrol")
    void patrolsAndHidesFromTheHaunted(Env env) {
        Round round = this.roundWithVariant(env, CreekConfig.DEFAULT);
        Creek creek = round.service().creek();
        CreekVariants variants = round.service().variants();
        assertNotNull(creek);
        assertNotNull(variants);
        assertInstanceOf(PatrolState.class, creek.state());
        assertTrue(variants.running().containsKey(round.scared().getUuid()));
        assertTrue(creek.body().isVisibleTo(round.scared().getUuid()), "the variant only just started");

        round.service().tick();

        assertFalse(creek.body().isVisibleTo(round.scared().getUuid()));
        assertTrue(creek.body().isVisibleTo(round.other().getUuid()));
        round.service().stop();
    }

    @Test
    @DisplayName("Every creek walks the routes with a walker of its own")
    void everyCreekHasItsOwnWalker(Env env) {
        Round round = this.roundWithVariant(env, CreekConfig.DEFAULT);
        Creek creek = round.service().creek();
        CreekVariants variants = round.service().variants();
        assertNotNull(creek);
        assertNotNull(variants);
        Creek variant = variants.running().get(round.scared().getUuid());
        assertNotNull(variant);

        assertInstanceOf(PathRoute.class, variant.route());
        assertNotSame(creek.route(), variant.route());
        assertEquals(creek.route().points(), variant.route().points());
        round.service().stop();
    }

    @Test
    @DisplayName("Each survivor's dread is worked out once per step, however many creeks there are")
    void dreadOncePerStep(Env env) {
        Round round = this.roundWithVariant(env, CreekConfig.DEFAULT);
        CreekVariants variants = round.service().variants();
        assertNotNull(variants);
        assertEquals(1, variants.running().size());
        round.dreadCalls().set(0);

        round.service().tick();

        assertEquals(2, round.dreadCalls().get());
        round.service().stop();
    }

    @Test
    @DisplayName("With the last survivor and the setting off, he is gone for good and every variant ends")
    void goneForGoodWithTheLastSurvivor(Env env) {
        Round round = this.roundWithVariant(env, Contexts.withoutLastSurvivor());
        Creek creek = round.service().creek();
        CreekVariants variants = round.service().variants();
        assertNotNull(creek);
        assertNotNull(variants);
        Creek variant = variants.running().get(round.scared().getUuid());
        assertNotNull(variant);
        round.service().tick();
        assertInstanceOf(PatrolState.class, creek.state(),
                "hiding the patrol from the haunted survivor is not being down to the last one");

        round.survivors().set(Set.of(round.scared()));
        round.service().tick();

        assertInstanceOf(VanishState.class, creek.state());
        assertTrue(((VanishState) creek.state()).isForever());
        assertTrue(variants.running().isEmpty());
        assertTrue(variant.body().entity().isRemoved());
        round.service().stop();
    }

    /**
     * A started round with two survivors far from the route, of whom only the first is scared
     * enough for a variant. It runs one step past the start delay: the patrol is out, and the
     * variant has just started.
     */
    private Round roundWithVariant(Env env, CreekConfig config) {
        Instance instance = env.createFlatInstance();
        // Far from the route points, so the vanish finds a hidden spot to come back at.
        Player scared = env.createConnection().connect(instance, new Pos(0, 40, -60, 0, 0));
        Player other = env.createConnection().connect(instance, new Pos(0, 40, -160, 0, 0));
        AtomicReference<Set<Player>> survivors = new AtomicReference<>(Set.of(scared, other));
        AtomicInteger dreadCalls = new AtomicInteger();
        AtomicLong clock = new AtomicLong();
        CreekService service = new CreekService(config, survivors::get, () -> instance, () -> List.of(ROUTE),
                CreakingBody::spawn, (id, _, _) -> {
                    dreadCalls.incrementAndGet();
                    return id.equals(scared.getUuid()) ? 0.5D : 0.0D;
                }, consequence,
                new RoundClock(clock::get), new Random(3), new CreekDebug());
        service.start();
        clock.set(CreekConfig.DEFAULT.vanishMaxSeconds() * 1000L + 1L);
        service.tick();
        return new Round(service, scared, other, survivors, dreadCalls);
    }

    private record Round(CreekService service, Player scared, Player other, AtomicReference<Set<Player>> survivors,
                         AtomicInteger dreadCalls) {
    }

}
