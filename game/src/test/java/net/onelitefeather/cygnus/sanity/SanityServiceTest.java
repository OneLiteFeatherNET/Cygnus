package net.onelitefeather.cygnus.sanity;

import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.SetExperiencePacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.ambient.SoundPreset;
import net.onelitefeather.cygnus.common.config.SanityConfig;
import net.onelitefeather.cygnus.common.page.event.PageFoundEvent;
import net.onelitefeather.cygnus.creek.dread.HuntEnd;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.event.GameStartEvent;
import net.onelitefeather.cygnus.event.SlenderReviveEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SanityServiceTest extends CygnusPlayerTestBase {

    private static final double EPSILON = 1.0E-9;
    /** The defaults without decay, scars, time floor or cap. So a test can look at gains alone. */
    private static final SanityConfig NO_DECAY = new SanityConfig(0.5D, 0.10D, 0.10D, 20, 0.30D, 0.15D, 0.25D, 0.0D, 0.015D, 0.0D, 0.0D, 1.0D);
    private static final long ROUND_MILLIS = 600_000L;

    private double pageProgress;
    private final AtomicLong clock = new AtomicLong();
    private Set<Player> survivors = Set.of();

    private SanityService service(SanityConfig config) {
        return new SanityService(config, () -> this.pageProgress, ROUND_MILLIS, this.clock::get, () -> this.survivors);
    }

    private double dread(SanityService service, Player player) {
        return service.dreadOf(player.getUuid(), this.clock.get());
    }

    private static Player connect(Env env, Instance instance, Pos position) {
        return env.createConnection().connect(instance, position);
    }

    @Test
    @DisplayName("Someone who is not tracked gets the page floor")
    void untrackedGetsTheFloor() {
        SanityService service = service(NO_DECAY);
        this.pageProgress = 0.5D;

        assertEquals(0.25D, service.dreadOf(UUID.randomUUID(), this.clock.get()), EPSILON);
    }

    @Test
    @DisplayName("Without any found pages there is no floor")
    void noFoundPagesNoFloor() {
        SanityService service = service(NO_DECAY);

        assertEquals(0.0D, service.dreadOf(UUID.randomUUID(), this.clock.get()), EPSILON);
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
        assertEquals(0.0D, service.dreadOf(UUID.randomUUID(), this.clock.get()), EPSILON, "no time floor before the start");

        EventDispatcher.call(new GameStartEvent());
        this.clock.addAndGet(ROUND_MILLIS / 2);

        assertEquals(0.075D, service.dreadOf(UUID.randomUUID(), this.clock.get()), EPSILON);
    }

    @Test
    @DisplayName("Pages and time together stay below the cap")
    void floorIsCapped(Env env) {
        SanityService service = service(SanityConfig.DEFAULT);
        env.process().eventHandler().addChild(service.node());
        EventDispatcher.call(new GameStartEvent());
        this.pageProgress = 1.0D;
        this.clock.addAndGet(ROUND_MILLIS);

        assertEquals(0.55D, service.dreadOf(UUID.randomUUID(), this.clock.get()), EPSILON);
    }

    @Test
    @DisplayName("Being picked out by the patrol scares")
    void selectionScares(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = connect(env, instance, new Pos(0, 40, 0));
        SanityService service = service(NO_DECAY);
        service.track(survivor);

        service.selected(survivor.getUuid());

        assertEquals(0.15D, dread(service, survivor), EPSILON);
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
        service.selected(slender.getUuid());

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
    @DisplayName("The observer hears a caught survivor's jump with its source")
    void observerHearsCaught(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = connect(env, instance, new Pos(0, 40, 0));
        java.util.List<String> heard = new java.util.ArrayList<>();
        SanityService service = new SanityService(NO_DECAY, () -> this.pageProgress, ROUND_MILLIS, this.clock::get,
                () -> this.survivors, (id, source, before, after) -> heard.add(source + ":" + before + ">" + after));
        service.track(survivor);

        service.caught(survivor.getUuid());

        assertEquals(java.util.List.of("caught:0.0>0.3"), heard);
    }

    @Test
    @DisplayName("A death is heard once for every other survivor, with the source death")
    void observerHearsDeathForEveryoneElse(Env env) {
        Instance instance = env.createFlatInstance();
        Player dead = connect(env, instance, new Pos(0, 40, 0));
        Player other = connect(env, instance, new Pos(2, 40, 0));
        java.util.List<java.util.UUID> heard = new java.util.ArrayList<>();
        SanityService service = new SanityService(NO_DECAY, () -> this.pageProgress, ROUND_MILLIS, this.clock::get,
                () -> this.survivors, (id, source, before, after) -> {
                    assertEquals("death", source);
                    heard.add(id);
                });
        service.track(dead);
        service.track(other);
        env.process().eventHandler().addChild(service.node());

        EventDispatcher.call(new net.minestom.server.event.player.PlayerDeathEvent(dead, net.kyori.adventure.text.Component.empty(), net.kyori.adventure.text.Component.empty()));

        assertEquals(java.util.List.of(other.getUuid()), heard);
    }

    @Test
    @DisplayName("Observing does not change how scared the survivor is")
    void observerDoesNotChangeTheFear(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = connect(env, instance, new Pos(0, 40, 0));
        SanityService observed = new SanityService(NO_DECAY, () -> this.pageProgress, ROUND_MILLIS, this.clock::get,
                () -> this.survivors, (id, source, before, after) -> {
                });
        SanityService plain = service(NO_DECAY);
        observed.track(survivor);
        plain.track(survivor);

        observed.caught(survivor.getUuid());
        plain.caught(survivor.getUuid());

        assertEquals(dread(plain, survivor), dread(observed, survivor), EPSILON);
    }

    // ---- the sanity on the level display ----

    @Test
    @DisplayName("The level shows the sanity: 100 when calm, 0 when terrified")
    void sanityLevelFollowsTheDread() {
        assertEquals(100, SanityService.sanityLevel(0.0D));
        assertEquals(70, SanityService.sanityLevel(0.3D));
        assertEquals(0, SanityService.sanityLevel(1.0D));
        assertEquals(0, SanityService.sanityLevel(1.4D));
        assertEquals(100, SanityService.sanityLevel(-0.1D));
    }

    @Test
    @DisplayName("Every survivor sees their own sanity as their level")
    void showsTheSanityOfEverySurvivor(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = connect(env, instance, new Pos(0, 40, 0));
        SanityService service = service(NO_DECAY);
        this.survivors = Set.of(survivor);
        env.process().eventHandler().addChild(service.node());
        EventDispatcher.call(new GameStartEvent());

        service.caught(survivor.getUuid());
        service.showSanity();

        assertEquals(70, survivor.getLevel());
        env.process().eventHandler().removeChild(service.node());
    }

    @Test
    @DisplayName("An unchanged sanity is not sent again")
    void anUnchangedSanityIsNotSentAgain(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player survivor = connection.connect(instance, new Pos(0, 40, 0));
        SanityService service = service(NO_DECAY);
        this.survivors = Set.of(survivor);
        env.process().eventHandler().addChild(service.node());
        EventDispatcher.call(new GameStartEvent());
        service.showSanity();
        Collector<SetExperiencePacket> packets = connection.trackIncoming(SetExperiencePacket.class);

        service.showSanity();

        packets.assertEmpty();
        env.process().eventHandler().removeChild(service.node());
    }

    @Test
    @DisplayName("A dead survivor's level goes back to 0 and stays there")
    void aDeadSurvivorShowsNoSanity(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = connect(env, instance, new Pos(0, 40, 0));
        SanityService service = service(NO_DECAY);
        this.survivors = Set.of(survivor);
        env.process().eventHandler().addChild(service.node());
        EventDispatcher.call(new GameStartEvent());
        service.showSanity();

        EventDispatcher.call(new PlayerDeathEvent(survivor, Component.empty(), Component.empty()));
        service.showSanity();

        assertEquals(0, survivor.getLevel());
        env.process().eventHandler().removeChild(service.node());
    }

    @Test
    @DisplayName("A survivor who takes over as the slender loses the sanity level")
    void theNewSlenderShowsNoSanity(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = connect(env, instance, new Pos(0, 40, 0));
        SanityService service = service(NO_DECAY);
        this.survivors = Set.of(survivor);
        env.process().eventHandler().addChild(service.node());
        EventDispatcher.call(new GameStartEvent());
        service.showSanity();

        EventDispatcher.call(new SlenderReviveEvent(survivor));

        assertEquals(0, survivor.getLevel());
        env.process().eventHandler().removeChild(service.node());
    }

    @Test
    @DisplayName("The end of the round takes the sanity off the level")
    void theRoundEndClearsTheSanity(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = connect(env, instance, new Pos(0, 40, 0));
        SanityService service = service(NO_DECAY);
        this.survivors = Set.of(survivor);
        env.process().eventHandler().addChild(service.node());
        EventDispatcher.call(new GameStartEvent());
        service.showSanity();
        assertEquals(100, survivor.getLevel());

        EventDispatcher.call(new GameFinishEvent(GameFinishEvent.Reason.TIME_OVER));

        assertEquals(0, survivor.getLevel());
        env.process().eventHandler().removeChild(service.node());
    }

    @Test
    @DisplayName("A lure raises the fear by its gain and reports it as a lure")
    void lureRaisesTheFear(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = connect(env, instance, new Pos(0, 40, 0));
        java.util.List<String> heard = new java.util.ArrayList<>();
        SanityService service = new SanityService(NO_DECAY, () -> this.pageProgress, ROUND_MILLIS, this.clock::get,
                () -> this.survivors, (id, source, before, after) -> heard.add(source + ":" + before + ">" + after));
        service.track(survivor);

        service.lured(survivor.getUuid(), 0.3D);

        assertEquals(0.3D, dread(service, survivor), EPSILON);
        assertEquals(java.util.List.of(SanityObserver.SOURCE_LURE + ":0.0>0.3"), heard);
    }

    @Test
    @DisplayName("A catch silences the ambient sounds, longer the bigger the jump")
    void catchSilences(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = connect(env, instance, new Pos(0, 40, 0));
        SanityService service = service(NO_DECAY);
        service.track(survivor);
        this.clock.set(10_000L);

        service.caught(survivor.getUuid());

        long silence = SanityService.silenceMillis(NO_DECAY.caughtGain());
        assertEquals(SoundPreset.SCARE, service.presetOf(survivor.getUuid(), 10_000L + silence - 1));
        assertEquals(SoundPreset.FREE, service.presetOf(survivor.getUuid(), 10_000L + silence));
    }

    @Test
    @DisplayName("A found page leaves the ambient sounds alone")
    void pageDoesNotSilence(Env env) {
        Instance instance = env.createFlatInstance();
        Player finder = connect(env, instance, new Pos(0, 40, 0));
        SanityService service = service(NO_DECAY);
        this.survivors = Set.of(finder);
        env.process().eventHandler().addChild(service.node());
        EventDispatcher.call(new GameStartEvent());

        EventDispatcher.call(new PageFoundEvent(finder, 1, 8));

        assertEquals(SoundPreset.FREE, service.presetOf(finder.getUuid(), 0L));
    }

    @Test
    @DisplayName("A crumbled doppelganger silences without scaring")
    void crumbleSilencesWithoutFear(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = connect(env, instance, new Pos(0, 40, 0));
        SanityService service = service(NO_DECAY);
        service.track(survivor);

        service.crumbled(survivor.getUuid());

        assertEquals(0.0D, dread(service, survivor), EPSILON);
        assertEquals(SoundPreset.SCARE, service.presetOf(survivor.getUuid(), SanityService.CRUMBLE_SILENCE_MILLIS - 1));
        assertEquals(SoundPreset.FREE, service.presetOf(survivor.getUuid(), SanityService.CRUMBLE_SILENCE_MILLIS));
    }

    @Test
    @DisplayName("A hunt holds the ambient sounds until it ends, however it ends")
    void huntHoldsUntilItEnds(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = connect(env, instance, new Pos(0, 40, 0));
        SanityService service = service(NO_DECAY);
        service.track(survivor);

        service.hunted(survivor.getUuid());
        assertEquals(SoundPreset.HUNT, service.presetOf(survivor.getUuid(), 60_000L));

        service.huntEnded(survivor.getUuid(), HuntEnd.TIMEOUT);
        assertEquals(SoundPreset.FREE, service.presetOf(survivor.getUuid(), 60_000L));
    }

    @Test
    @DisplayName("Someone who is not tracked leaves the ambient sounds to the fear")
    void untrackedIsFree() {
        assertEquals(SoundPreset.FREE, service(NO_DECAY).presetOf(UUID.randomUUID(), 0L));
    }

    @Test
    @DisplayName("The silence after a jump stays between its shortest and longest")
    void silenceIsClamped() {
        assertEquals(SanityService.MIN_SILENCE_MILLIS, SanityService.silenceMillis(0.01D));
        assertEquals(1_800L, SanityService.silenceMillis(0.15D));
        assertEquals(SanityService.MAX_SILENCE_MILLIS, SanityService.silenceMillis(1.0D));
    }
}
