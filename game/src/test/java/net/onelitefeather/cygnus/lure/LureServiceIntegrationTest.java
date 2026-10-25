package net.onelitefeather.cygnus.lure;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.DestroyEntitiesPacket;
import net.minestom.server.network.packet.server.play.SpawnEntityPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.config.LureConfig;
import net.onelitefeather.cygnus.entity.DeadPlayerMannequin;
import net.onelitefeather.cygnus.jumpscare.JumpScareManager;
import net.onelitefeather.cygnus.telemetry.TickSections;
import net.onelitefeather.cygnus.utils.Eyes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LureServiceIntegrationTest extends CygnusPlayerTestBase {

    /** Lures on every roll. */
    private static final LureConfig ALWAYS = new LureConfig(true, 30, 1.0D, 0.30D, 4, 60);

    private static final Pos ALONE = new Pos(0.5, 40, 0.5, 0, 0);

    private final AtomicLong clock = new AtomicLong();
    private final List<Player> survivors = new ArrayList<>();
    private final List<String> consequences = new ArrayList<>();
    private JumpScareManager jumpScares;

    @Test
    @DisplayName("Without a dead survivor nobody is lured")
    void noCorpseNoLure(Env env) {
        Instance instance = flat(env);
        Player survivor = join(env, instance, ALONE);
        LureService service = service(ALWAYS, instance);

        steps(service, 600);

        assertTrue(service.figureOf(survivor.getUuid()).isEmpty());
    }

    @Test
    @DisplayName("Right after a turnaround scare nobody is lured")
    void noLureRightAfterATurnaroundScare(Env env) {
        Instance instance = flat(env);
        Player survivor = join(env, instance, ALONE);
        corpse(env, instance);
        LureService service = service(ALWAYS, instance);
        assertTrue(this.jumpScares.force(survivor));

        steps(service, 20);

        assertTrue(service.figureOf(survivor.getUuid()).isEmpty());
    }

    @Test
    @DisplayName("A survivor with company is not lured")
    void notAloneNoLure(Env env) {
        Instance instance = flat(env);
        Player survivor = join(env, instance, ALONE);
        join(env, instance, new Pos(10.5, 40, 0.5));
        corpse(env, instance);
        LureService service = service(ALWAYS, instance);

        steps(service, 600);

        assertTrue(service.figureOf(survivor.getUuid()).isEmpty());
    }

    @Test
    @DisplayName("A lone survivor sees a dead teammate, and the turnaround scare waits")
    void aloneWithCorpseShowsTheFigure(Env env) {
        Instance instance = flat(env);
        Player survivor = join(env, instance, ALONE);
        Player dead = corpse(env, instance);
        LureService service = service(ALWAYS, instance);

        service.tick();

        LureFigure figure = service.figureOf(survivor.getUuid()).orElseThrow();
        assertEquals(dead.getUuid(), figure.identity());
        assertTrue(this.jumpScares.isTurnaroundPaused(survivor.getUuid()));
    }

    @Test
    @DisplayName("Looking at the figure fires the scare, strikes at once and follows up after the dissolve")
    void gazeFiresTheScare(Env env) {
        Instance instance = flat(env);
        TestConnection connection = env.createConnection();
        Player survivor = connection.connect(instance, ALONE);
        this.survivors.add(survivor);
        corpse(env, instance);
        LureService service = service(ALWAYS, instance);
        service.tick();
        lookAt(survivor, service.figureOf(survivor.getUuid()).orElseThrow());
        Collector<SpawnEntityPacket> spawns = connection.trackIncoming(SpawnEntityPacket.class);

        service.tick();
        this.clock.addAndGet(LureService.GAZE_MILLIS);
        service.tick();

        assertTrue(service.figureOf(survivor.getUuid()).isEmpty());
        assertTrue(spawns.collect().stream().anyMatch(packet -> packet.type() == EntityType.MANNEQUIN));
        assertEquals(List.of("struck:" + survivor.getUuid() + "@" + this.clock.get()), this.consequences,
                "the follow-up waits for the dissolve");
        assertFalse(this.jumpScares.isTurnaroundPaused(survivor.getUuid()));

        this.clock.addAndGet(LureService.FOLLOW_UP_MILLIS);
        service.tick();

        assertEquals("followUp:" + survivor.getUuid(), this.consequences.getLast());
    }

    @Test
    @DisplayName("After one scare a survivor is never lured again this round")
    void secondLureNeverComes(Env env) {
        Instance instance = flat(env);
        Player survivor = join(env, instance, ALONE);
        corpse(env, instance);
        LureService service = service(ALWAYS, instance);
        fire(service, survivor);

        steps(service, 600);

        assertTrue(service.figureOf(survivor.getUuid()).isEmpty());
    }

    @Test
    @DisplayName("A figure nobody noticed is gone after the wait and the lure stays open")
    void unseenFigureDespawnsAndKeepsTheLure(Env env) {
        Instance instance = flat(env);
        TestConnection connection = env.createConnection();
        Player survivor = connection.connect(instance, ALONE);
        this.survivors.add(survivor);
        corpse(env, instance);
        LureService service = service(ALWAYS, instance);
        service.tick();
        LureFigure first = service.figureOf(survivor.getUuid()).orElseThrow();
        lookAway(survivor, first);

        this.clock.addAndGet(LureService.WAIT_MILLIS);
        service.tick();

        assertTrue(service.figureOf(survivor.getUuid()).isEmpty());
        assertFalse(this.jumpScares.isTurnaroundPaused(survivor.getUuid()));

        this.clock.addAndGet(1_000L);
        service.tick();

        assertTrue(service.figureOf(survivor.getUuid()).isPresent(), "an unseen lure does not use up the round's lure");
    }

    @Test
    @DisplayName("A survivor who gets company loses the figure at once")
    void companionArrivingRemovesTheFigure(Env env) {
        Instance instance = flat(env);
        TestConnection connection = env.createConnection();
        Player survivor = connection.connect(instance, ALONE);
        this.survivors.add(survivor);
        Player companion = join(env, instance, new Pos(100.5, 40, 0.5));
        corpse(env, instance);
        LureService service = service(ALWAYS, instance);
        service.tick();
        assertTrue(service.figureOf(survivor.getUuid()).isPresent());
        Collector<DestroyEntitiesPacket> destroys = connection.trackIncoming(DestroyEntitiesPacket.class);

        companion.teleport(new Pos(10.5, 40, 0.5)).join();
        service.tick();

        assertTrue(service.figureOf(survivor.getUuid()).isEmpty());
        assertEquals(1, destroys.collect().size());
    }

    @Test
    @DisplayName("A survivor who leaves the round while a figure waits takes it with them")
    void survivorGoneEndsTheWait(Env env) {
        Instance instance = flat(env);
        TestConnection connection = env.createConnection();
        Player survivor = connection.connect(instance, ALONE);
        this.survivors.add(survivor);
        corpse(env, instance);
        LureService service = service(ALWAYS, instance);
        service.tick();
        Collector<DestroyEntitiesPacket> destroys = connection.trackIncoming(DestroyEntitiesPacket.class);

        this.survivors.remove(survivor);
        service.tick();

        assertTrue(service.figureOf(survivor.getUuid()).isEmpty());
        assertEquals(1, destroys.collect().size());
        assertFalse(this.jumpScares.isTurnaroundPaused(survivor.getUuid()));
        assertDoesNotThrow(service::tick);
    }

    @Test
    @DisplayName("Stopping removes every figure, forgets every lure and cleans up the consequence")
    void stopClearsEverything(Env env) {
        Instance instance = flat(env);
        Player waiting = join(env, instance, ALONE);
        corpse(env, instance);
        LureService service = service(ALWAYS, instance);
        service.tick();

        service.stop();

        assertEquals(List.of("cleanUp"), this.consequences);

        assertTrue(service.figureOf(waiting.getUuid()).isEmpty());
        assertFalse(this.jumpScares.isTurnaroundPaused(waiting.getUuid()));
        assertFalse(service.isRunning());

        service.tick();
        assertTrue(service.figureOf(waiting.getUuid()).isEmpty(), "a step that comes after the stop does nothing");

        service.start();
        fire(service, waiting);
        service.stop();
        // Like the end of a round: the corpses and the scare cooldowns go, a new death brings a new corpse.
        this.jumpScares.cleanUp();
        corpse(env, instance);
        this.jumpScares.register(this.pendingCorpses.getLast());
        service.start();
        service.tick();

        assertTrue(service.figureOf(waiting.getUuid()).isPresent(), "a new round starts with every lure open");
        service.stop();
    }

    private LureService service(LureConfig config, Instance instance) {
        this.jumpScares = new JumpScareManager(TickSections.NONE);
        for (DeadPlayerMannequin corpse : this.pendingCorpses) {
            this.jumpScares.register(corpse);
        }
        LureSpotFinder spots = new LureSpotFinder(() -> instance);
        return new LureService(config, () -> List.copyOf(this.survivors), this.jumpScares, spots, new LureConsequence() {
            @Override
            public void struck(Player survivor, long now) {
                consequences.add("struck:" + survivor.getUuid() + "@" + now);
            }

            @Override
            public void followUp(Player survivor) {
                consequences.add("followUp:" + survivor.getUuid());
            }

            @Override
            public void cleanUp() {
                consequences.add("cleanUp");
            }
        }, this.clock::get, new Random(5), TickSections.NONE);
    }

    private final List<DeadPlayerMannequin> pendingCorpses = new ArrayList<>();

    private Player join(Env env, Instance instance, Pos at) {
        Player player = env.createConnection().connect(instance, at);
        this.survivors.add(player);
        return player;
    }

    /**
     * A dead survivor far away, whose corpse lies in the world.
     */
    private Player corpse(Env env, Instance instance) {
        Player dead = env.createConnection().connect(instance, new Pos(-200.5, 40, 0.5));
        DeadPlayerMannequin corpse = DeadPlayerMannequin.sleeping(dead);
        corpse.setInstance(instance, new Pos(-200.5, 40, 0.5));
        this.pendingCorpses.add(corpse);
        return dead;
    }

    /**
     * Shows the figure, looks at it and waits out the gaze and the follow-up.
     */
    private void fire(LureService service, Player survivor) {
        service.tick();
        lookAt(survivor, service.figureOf(survivor.getUuid()).orElseThrow());
        service.tick();
        this.clock.addAndGet(LureService.GAZE_MILLIS);
        service.tick();
        this.clock.addAndGet(LureService.FOLLOW_UP_MILLIS);
        service.tick();
    }

    private void steps(LureService service, int count) {
        for (int i = 0; i < count; i++) {
            service.tick();
            this.clock.addAndGet(100L);
        }
    }

    private static void lookAt(Player survivor, LureFigure figure) {
        Pos eyes = Eyes.of(survivor.getPosition());
        Pos look = eyes.withLookAt(Eyes.of(figure.position()));
        survivor.teleport(survivor.getPosition().withView(look.yaw(), look.pitch())).join();
    }

    private static void lookAway(Player survivor, LureFigure figure) {
        Pos eyes = Eyes.of(survivor.getPosition());
        Pos look = eyes.withLookAt(Eyes.of(figure.position()));
        survivor.teleport(survivor.getPosition().withView(look.yaw() + 180.0F, 0.0F)).join();
    }

    private static Instance flat(Env env) {
        Instance instance = env.createFlatInstance();
        for (int x = -14; x <= 7; x++) {
            for (int z = -3; z <= 2; z++) {
                instance.loadChunk(x, z).join();
            }
        }
        return instance;
    }
}
