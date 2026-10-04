package net.onelitefeather.cygnus.sanity;

import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.config.SanityConfig;
import net.onelitefeather.cygnus.common.page.event.PageFoundEvent;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.event.GameStartEvent;
import net.onelitefeather.cygnus.event.SlenderReviveEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SanityServiceTest extends CygnusPlayerTestBase {

    private static final double EPSILON = 1.0E-9;
    /** The defaults without decay, scars, time floor or cap. So a test can look at gains alone. */
    private static final SanityConfig NO_DECAY = new SanityConfig(0.5D, 0.10D, 0.10D, 20, 0.30D, 0.25D, 0.0D,
            0.015D, 0.0D, 0.0D, 1.0D);
    private static final long ROUND_MILLIS = 600_000L;

    private double pageProgress;
    private final AtomicLong clock = new AtomicLong();
    private Set<Player> survivors = Set.of();

    private SanityService service(SanityConfig config) {
        return new SanityService(config, true, () -> this.pageProgress, ROUND_MILLIS, this.clock::get,
                () -> this.survivors);
    }

    private static double dread(SanityService service, Player player) {
        return service.dreadOf(player.getUuid());
    }

    private static Player connect(Env env, Instance instance, Pos position) {
        return env.createConnection().connect(instance, position);
    }

    @Test
    @DisplayName("Someone who is not tracked gets the page floor")
    void untrackedGetsTheFloor() {
        SanityService service = service(NO_DECAY);
        this.pageProgress = 0.5D;

        assertEquals(0.25D, service.dreadOf(UUID.randomUUID()), EPSILON);
    }

    @Test
    @DisplayName("Without any found pages there is no floor")
    void noFoundPagesNoFloor() {
        SanityService service = service(NO_DECAY);

        assertEquals(0.0D, service.dreadOf(UUID.randomUUID()), EPSILON);
    }

    @Test
    @DisplayName("The start of a round tracks every survivor, calm")
    void gameStartTracksSurvivors(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = connect(env, instance, new Pos(0, 40, 0));
        SanityService service = service(NO_DECAY);
        this.survivors = Set.of(survivor);
        env.process().eventHandler().addChild(service.node());

        EventDispatcher.call(new GameStartEvent());
        service.caught(survivor.getUuid());

        assertEquals(0.30D, dread(service, survivor), EPSILON);
    }

    @Test
    @DisplayName("A page only scares the one who found it")
    void pageScaresOnlyTheFinder(Env env) {
        Instance instance = env.createFlatInstance();
        Player finder = connect(env, instance, new Pos(0, 40, 0));
        Player other = connect(env, instance, new Pos(2, 40, 0));
        SanityService service = service(NO_DECAY);
        this.survivors = Set.of(finder, other);
        env.process().eventHandler().addChild(service.node());
        EventDispatcher.call(new GameStartEvent());

        EventDispatcher.call(new PageFoundEvent(finder, 1, 8));

        assertEquals(0.10D, dread(service, finder), EPSILON);
        assertEquals(0.0D, dread(service, other), EPSILON);
    }

    @Test
    @DisplayName("Being caught scares the survivor")
    void caughtScares(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = connect(env, instance, new Pos(0, 40, 0));
        SanityService service = service(NO_DECAY);
        service.track(survivor);

        service.caught(survivor.getUuid());

        assertEquals(0.30D, dread(service, survivor), EPSILON);
    }

    @Test
    @DisplayName("Sightings count once per cooldown")
    void sightingsRespectTheCooldown(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = connect(env, instance, new Pos(0, 40, 0));
        SanityService service = service(NO_DECAY);
        service.track(survivor);

        service.sighted(survivor.getUuid());
        this.clock.set(10_000L);
        service.sighted(survivor.getUuid());
        assertEquals(0.10D, dread(service, survivor), EPSILON);

        this.clock.set(20_000L);
        service.sighted(survivor.getUuid());
        assertEquals(0.20D, dread(service, survivor), EPSILON);
    }

    @Test
    @DisplayName("A death scares every other survivor, however far, and the dead one is dropped")
    void deathScaresEveryone(Env env) {
        Instance instance = env.createFlatInstance();
        Player dead = connect(env, instance, new Pos(0, 40, 0));
        Player near = connect(env, instance, new Pos(10, 40, 0));
        Player far = connect(env, instance, new Pos(500, 40, 0));
        SanityService service = service(NO_DECAY);
        this.survivors = Set.of(dead, near, far);
        env.process().eventHandler().addChild(service.node());
        EventDispatcher.call(new GameStartEvent());
        service.caught(dead.getUuid());

        EventDispatcher.call(new PlayerDeathEvent(dead, Component.empty(), Component.empty()));

        assertEquals(0.25D, dread(service, near), EPSILON);
        assertEquals(0.25D, dread(service, far), EPSILON);
        assertEquals(0.0D, dread(service, dead), EPSILON, "the dead one reads as the floor");
    }

    @Test
    @DisplayName("Fear wears off with the default decay, down to the scar the catch left")
    void fearWearsOff(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = connect(env, instance, new Pos(0, 40, 0));
        SanityService service = service(SanityConfig.DEFAULT);
        service.track(survivor);

        service.caught(survivor.getUuid());
        this.clock.set(60_000L);

        assertEquals(0.075D, dread(service, survivor), EPSILON);
    }

    @Test
    @DisplayName("The floor grows with the time played once the round has started")
    void timeRaisesTheFloor(Env env) {
        SanityService service = service(SanityConfig.DEFAULT);
        env.process().eventHandler().addChild(service.node());
        this.clock.set(ROUND_MILLIS);
        assertEquals(0.0D, service.dreadOf(UUID.randomUUID()), EPSILON, "no time floor before the start");

        EventDispatcher.call(new GameStartEvent());
        this.clock.addAndGet(ROUND_MILLIS / 2);

        assertEquals(0.075D, service.dreadOf(UUID.randomUUID()), EPSILON);
    }

    @Test
    @DisplayName("Pages and time together stay below the cap")
    void floorIsCapped(Env env) {
        SanityService service = service(SanityConfig.DEFAULT);
        env.process().eventHandler().addChild(service.node());
        EventDispatcher.call(new GameStartEvent());
        this.pageProgress = 1.0D;
        this.clock.addAndGet(ROUND_MILLIS);

        assertEquals(0.55D, service.dreadOf(UUID.randomUUID()), EPSILON);
    }

    @Test
    @DisplayName("Being stalked raises the fear over time")
    void stalkingScares(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = connect(env, instance, new Pos(0, 40, 0));
        SanityService service = service(NO_DECAY);
        service.track(survivor);

        for (long now = 0L; now <= 10_000L; now += 100L) {
            this.clock.set(now);
            service.stalked(survivor.getUuid());
        }

        assertEquals(0.15D, dread(service, survivor), EPSILON);
    }

    @Test
    @DisplayName("Found pages keep the fear up")
    void pagesRaiseTheFloor(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = connect(env, instance, new Pos(0, 40, 0));
        SanityService service = service(SanityConfig.DEFAULT);
        service.track(survivor);
        this.pageProgress = 1.0D;

        assertEquals(0.5D, dread(service, survivor), EPSILON);
    }

    @Test
    @DisplayName("Anyone not tracked is ignored")
    void untrackedCallsAreIgnored(Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = connect(env, instance, new Pos(0, 40, 0));
        SanityService service = service(NO_DECAY);
        this.survivors = Set.of();
        env.process().eventHandler().addChild(service.node());
        EventDispatcher.call(new GameStartEvent());

        EventDispatcher.call(new PageFoundEvent(slender, 1, 8));
        service.caught(slender.getUuid());
        service.sighted(slender.getUuid());
        service.stalked(slender.getUuid());

        assertEquals(0.0D, dread(service, slender), EPSILON);
    }

    @Test
    @DisplayName("Leaving drops a survivor")
    void disconnectDrops(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = connect(env, instance, new Pos(0, 40, 0));
        SanityService service = service(NO_DECAY);
        this.survivors = Set.of(survivor);
        env.process().eventHandler().addChild(service.node());
        EventDispatcher.call(new GameStartEvent());
        service.caught(survivor.getUuid());

        EventDispatcher.call(new PlayerDisconnectEvent(survivor));

        assertEquals(0.0D, dread(service, survivor), EPSILON);
    }

    @Test
    @DisplayName("The end of a round clears everyone")
    void gameFinishClears(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = connect(env, instance, new Pos(0, 40, 0));
        SanityService service = service(NO_DECAY);
        this.survivors = Set.of(survivor);
        env.process().eventHandler().addChild(service.node());
        EventDispatcher.call(new GameStartEvent());
        service.caught(survivor.getUuid());

        EventDispatcher.call(new GameFinishEvent(GameFinishEvent.Reason.TIME_OVER));

        assertEquals(0.0D, dread(service, survivor), EPSILON);
    }

    @Test
    @DisplayName("A survivor who becomes the slender is dropped, so their death scares nobody")
    void newSlenderIsDropped(Env env) {
        Instance instance = env.createFlatInstance();
        Player promoted = connect(env, instance, new Pos(0, 40, 0));
        Player near = connect(env, instance, new Pos(5, 40, 0));
        SanityService service = service(NO_DECAY);
        this.survivors = Set.of(promoted, near);
        env.process().eventHandler().addChild(service.node());
        EventDispatcher.call(new GameStartEvent());

        EventDispatcher.call(new SlenderReviveEvent(promoted));
        EventDispatcher.call(new PlayerDeathEvent(promoted, Component.empty(), Component.empty()));

        assertEquals(0.0D, dread(service, near), EPSILON);
    }

    @Test
    @DisplayName("The given switch decides whether the service runs")
    void followsSwitch() {
        assertTrue(service(NO_DECAY).enabled());
        assertFalse(new SanityService(NO_DECAY, false, () -> 0.0D, ROUND_MILLIS, this.clock::get, Set::of).enabled());
    }
}
