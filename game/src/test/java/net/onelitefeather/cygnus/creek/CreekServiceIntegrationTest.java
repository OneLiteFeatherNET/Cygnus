package net.onelitefeather.cygnus.creek;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.ActionBarPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.config.CreekConfig;
import net.onelitefeather.cygnus.common.creek.CreekRoute;
import net.onelitefeather.cygnus.creek.body.CreakingBody;
import net.onelitefeather.cygnus.creek.consequence.CatchConsequence;
import net.onelitefeather.cygnus.creek.debug.CreekDebug;
import net.onelitefeather.cygnus.creek.state.VanishState;
import net.onelitefeather.cygnus.creek.world.PathRoute;
import net.onelitefeather.cygnus.creek.world.RandomPointRoute;
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

    private CreekService service(Instance instance, Set<Player> survivors, List<Pos> points, AtomicLong clock) {
        return new CreekService(CreekConfig.DEFAULT, () -> survivors, () -> instance, () -> points, () -> List.of(),
                CreakingBody::spawn, (_, _, _) -> 0.0D, consequence, new RoundClock(clock::get), new Random(3), new CreekDebug());
    }

    @Test
    @DisplayName("At the start he is in the world, but out of sight")
    void startsOutOfSight(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0));
        CreekService service = service(instance, Set.of(survivor), List.of(new Pos(10, 40, 10)), new AtomicLong());

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
        CreekService service = service(instance, Set.of(survivor), List.of(new Pos(10, 40, 10)), new AtomicLong());
        service.start();
        Creek creek = service.creek();
        assertNotNull(creek);

        service.stop();

        assertTrue(creek.body().entity().isRemoved());
        assertNull(service.creek());
        assertEquals(1, cleanUps.get());
    }

    @Test
    @DisplayName("Without any point to walk between he stays away")
    void staysAwayWithoutPoints(Env env) {
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
                () -> List.of(new Pos(10, 40, 10)), () -> List.of(), CreakingBody::spawn, (_, _, _) -> 0.0D, consequence, roundClock, new Random(3), new CreekDebug());

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
                () -> List.of(new Pos(10, 40, 10)), () -> List.of(), CreakingBody::spawn, (_, _, _) -> 0.0D, consequence,
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
        CreekRoute route = new CreekRoute("Weg", List.of(new Vec(10, 40, 10), new Vec(20, 40, 10)));
        CreekService service = new CreekService(CreekConfig.DEFAULT, () -> Set.of(survivor), () -> instance,
                List::of, () -> List.of(route), CreakingBody::spawn, (_, _, _) -> 0.0D, consequence,
                new RoundClock(new AtomicLong()::get), new Random(3), new CreekDebug());

        service.start();

        assertInstanceOf(PathRoute.class, service.route());
        assertNotNull(service.creek());
        service.stop();
    }

    @Test
    @DisplayName("Without valid routes the creek falls back to pages and spawns")
    void fallsBackWithoutRoutes(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0, 40, 0));
        CreekService service = service(instance, Set.of(survivor), List.of(new Pos(10, 40, 10)), new AtomicLong());

        service.start();

        assertInstanceOf(RandomPointRoute.class, service.route());
        service.stop();
    }
}
