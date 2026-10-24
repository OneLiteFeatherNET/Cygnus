package net.onelitefeather.cygnus.lure;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.DestroyEntitiesPacket;
import net.minestom.server.network.packet.server.play.SpawnEntityPacket;
import net.minestom.server.potion.PotionEffect;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.config.LureConfig;
import net.onelitefeather.cygnus.creek.CreekRespite;
import net.onelitefeather.cygnus.creek.state.SurvivorView;
import net.onelitefeather.cygnus.creek.world.InstanceGround;
import net.onelitefeather.cygnus.entity.DeadPlayerMannequin;
import net.onelitefeather.cygnus.jumpscare.JumpScareManager;
import net.onelitefeather.cygnus.telemetry.TickSections;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LureServiceIntegrationTest extends CygnusPlayerTestBase {

    /** Rolls a lure on every step and always follows up with a betrayal. */
    private static final LureConfig ALWAYS = new LureConfig(true, 30, 1, 1.0D, 15, 25, 80, 130, 3, 20, 700, 35,
            0.30D, 0.0D, 1.0D, 0.0D, 4, 60);

    private static final Pos ALONE = new Pos(0.5, 40, 0.5, 0, 0);

    private final AtomicLong clock = new AtomicLong();
    private final List<Player> survivors = new ArrayList<>();
    private final List<String> fear = new ArrayList<>();
    private final List<UUID> revealed = new ArrayList<>();
    private final CreekRespite respite = new CreekRespite();
    private JumpScareManager jumpScares;
    private @Nullable Player slender;

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
    @DisplayName("Looking at the figure fires the scare, scares, spares and follows up after the dissolve")
    void gazeFiresTheScare(Env env) {
        Instance instance = flat(env);
        TestConnection connection = env.createConnection();
        Player survivor = connection.connect(instance, ALONE);
        this.survivors.add(survivor);
        corpse(env, instance);
        this.slender = env.createConnection().connect(instance, new Pos(-300.5, 40, 0.5));
        LureService service = service(ALWAYS, instance);
        service.tick();
        lookAt(survivor, service.figureOf(survivor.getUuid()).orElseThrow());
        Collector<SpawnEntityPacket> spawns = connection.trackIncoming(SpawnEntityPacket.class);

        service.tick();
        this.clock.addAndGet(700L);
        service.tick();

        assertTrue(service.figureOf(survivor.getUuid()).isEmpty());
        assertTrue(spawns.collect().stream().anyMatch(packet -> packet.type() == EntityType.MANNEQUIN));
        assertEquals(List.of(survivor.getUuid() + ":0.3"), this.fear);
        assertTrue(this.respite.spares(survivor.getUuid(), this.clock.get()));
        assertFalse(this.jumpScares.isTurnaroundPaused(survivor.getUuid()));
        assertTrue(this.revealed.isEmpty(), "the follow-up waits for the dissolve");

        this.clock.addAndGet(LureService.FOLLOW_UP_MILLIS);
        service.tick();

        assertEquals(List.of(survivor.getUuid()), this.revealed);
    }

    @Test
    @DisplayName("Slowness follows up as a potion effect")
    void slownessFollowsUp(Env env) {
        Instance instance = flat(env);
        Player slowed = join(env, instance, ALONE);
        corpse(env, instance);
        LureConfig slow = new LureConfig(true, 30, 1, 1.0D, 15, 25, 80, 130, 3, 20, 700, 35,
                0.30D, 1.0D, 0.0D, 0.0D, 4, 60);
        LureService service = service(slow, instance);

        fire(service, slowed);

        assertTrue(slowed.hasEffect(PotionEffect.SLOWNESS));
    }

    @Test
    @DisplayName("Blindness follows up as a potion effect")
    void blindnessFollowsUp(Env env) {
        Instance instance = flat(env);
        Player blinded = join(env, instance, ALONE);
        corpse(env, instance);
        LureConfig blind = new LureConfig(true, 30, 1, 1.0D, 15, 25, 80, 130, 3, 20, 700, 35,
                0.30D, 0.0D, 0.0D, 1.0D, 4, 60);
        LureService service = service(blind, instance);

        fire(service, blinded);

        assertTrue(blinded.hasEffect(PotionEffect.BLINDNESS));
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

        this.clock.addAndGet(20_000L);
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
    @DisplayName("Stopping removes every figure, forgets every lure and clears the respite")
    void stopClearsEverything(Env env) {
        Instance instance = flat(env);
        Player waiting = join(env, instance, ALONE);
        corpse(env, instance);
        LureService service = service(ALWAYS, instance);
        service.tick();
        UUID spared = UUID.randomUUID();
        this.respite.spare(spared, Long.MAX_VALUE);

        service.stop();

        assertFalse(this.respite.spares(spared, this.clock.get()));

        assertTrue(service.figureOf(waiting.getUuid()).isEmpty());
        assertFalse(this.jumpScares.isTurnaroundPaused(waiting.getUuid()));
        assertFalse(service.isRunning());

        fire(service, waiting);
        service.stop();
        service.tick();

        assertTrue(service.figureOf(waiting.getUuid()).isPresent(), "a new round starts with every lure open");
    }

    private LureService service(LureConfig config, Instance instance) {
        this.jumpScares = new JumpScareManager(TickSections.NONE);
        for (DeadPlayerMannequin corpse : this.pendingCorpses) {
            this.jumpScares.register(corpse);
        }
        LureSpotFinder spots = new LureSpotFinder(config, new InstanceGround(() -> instance), () -> instance);
        return new LureService(config, () -> List.copyOf(this.survivors), () -> this.slender, this.jumpScares, spots, this.respite,
                (id, gain) -> this.fear.add(id + ":" + gain), (survivor, _) -> this.revealed.add(survivor.getUuid()),
                this.clock::get, new Random(5), TickSections.NONE);
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
        this.clock.addAndGet(700L);
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
        Pos eyes = survivor.getPosition().add(0, SurvivorView.EYE_HEIGHT, 0);
        Pos look = eyes.withLookAt(figure.position().add(0, SurvivorView.EYE_HEIGHT, 0));
        survivor.teleport(survivor.getPosition().withView(look.yaw(), look.pitch())).join();
    }

    private static void lookAway(Player survivor, LureFigure figure) {
        Pos eyes = survivor.getPosition().add(0, SurvivorView.EYE_HEIGHT, 0);
        Pos look = eyes.withLookAt(figure.position().add(0, SurvivorView.EYE_HEIGHT, 0));
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
