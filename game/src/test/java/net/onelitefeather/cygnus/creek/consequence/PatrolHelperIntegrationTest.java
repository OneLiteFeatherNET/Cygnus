package net.onelitefeather.cygnus.creek.consequence;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.play.FacePlayerPacket;
import net.minestom.server.network.packet.server.play.SoundEffectPacket;
import net.minestom.server.potion.PotionEffect;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatrolHelperIntegrationTest extends CygnusPlayerTestBase {

    /** Where the creek's eyes are, a few blocks in front of the survivors at the origin. */
    private static final Supplier<Pos> CREEK_EYES = () -> new Pos(0, 42.5, 5);

    @Test
    @DisplayName("The first beat of a stare darkens the target's view")
    void firstBeatDarkens(Env env) {
        Instance instance = env.createFlatInstance();
        Player target = env.createConnection().connect(instance, new Pos(0, 40, 0));
        PatrolHelper patrol = new PatrolHelper(new Random(1));

        patrol.stareBeat(target, 0, new Pos(0, 40, 5));

        assertTrue(target.hasEffect(PotionEffect.DARKNESS));
        patrol.cleanUp();
    }

    @Test
    @DisplayName("Later beats only sound, they add no darkness")
    void laterBeatsOnlySound(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player target = connection.connect(instance, new Pos(0, 40, 0));
        Collector<SoundEffectPacket> sounds = connection.trackIncoming(SoundEffectPacket.class);
        PatrolHelper patrol = new PatrolHelper(new Random(1));

        patrol.stareBeat(target, 1, new Pos(0, 40, 5));

        assertFalse(target.hasEffect(PotionEffect.DARKNESS));
        assertFalse(sounds.collect().isEmpty());
    }

    @Test
    @DisplayName("A broken stare takes the darkness back at once")
    void brokenStareLiftsTheDarkness(Env env) {
        Instance instance = env.createFlatInstance();
        Player target = env.createConnection().connect(instance, new Pos(0, 40, 0));
        PatrolHelper patrol = new PatrolHelper(new Random(1));
        patrol.stareBeat(target, 0, new Pos(0, 40, 5));

        patrol.stareBroken(target);

        assertFalse(target.hasEffect(PotionEffect.DARKNESS));
    }

    @Test
    @DisplayName("A stun turns the survivor's head towards the creek")
    void stunTurnsTheHead(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player selected = connection.connect(instance, new Pos(0, 40, 0));
        Collector<FacePlayerPacket> faces = connection.trackIncoming(FacePlayerPacket.class);
        PatrolHelper patrol = new PatrolHelper(new Random(1));

        patrol.stun(selected, CREEK_EYES, List.of(selected));
        env.tick();

        assertFalse(faces.collect().isEmpty());
        patrol.cleanUp();
    }

    @Test
    @DisplayName("A stun slows the selected survivor and does not count as a catch")
    void stunSlowsTheSelected(Env env) {
        Instance instance = env.createFlatInstance();
        Player selected = env.createConnection().connect(instance, new Pos(0, 40, 0));
        PatrolHelper patrol = new PatrolHelper(new Random(1));

        patrol.stun(selected, CREEK_EYES, List.of(selected));

        assertTrue(selected.hasEffect(PotionEffect.SLOWNESS));
        assertFalse(selected.hasEffect(PotionEffect.BLINDNESS));
        assertFalse(selected.hasTag(StagedCatchConsequence.CATCHES));
    }

    @Test
    @DisplayName("A stun blinds other survivors nearby, nobody else")
    void stunBlindsOtherSurvivorsNearby(Env env) {
        Instance instance = env.createFlatInstance();
        Player selected = env.createConnection().connect(instance, new Pos(0, 40, 0));
        Player near = env.createConnection().connect(instance, new Pos(5, 40, 0));
        Player far = env.createConnection().connect(instance, new Pos(20, 40, 0));
        Player slender = env.createConnection().connect(instance, new Pos(3, 40, 0));
        PatrolHelper patrol = new PatrolHelper(new Random(1));

        patrol.stun(selected, CREEK_EYES, List.of(selected, near, far));

        assertTrue(near.hasEffect(PotionEffect.BLINDNESS));
        assertFalse(far.hasEffect(PotionEffect.BLINDNESS));
        assertFalse(slender.hasEffect(PotionEffect.BLINDNESS), "only survivors go blind");
        assertFalse(selected.hasEffect(PotionEffect.BLINDNESS));
    }

    @Test
    @DisplayName("Vanishing blinds the survivors nearby, nobody else")
    void vanishBlindsSurvivorsNearby(Env env) {
        Instance instance = env.createFlatInstance();
        Player near = env.createConnection().connect(instance, new Pos(5, 40, 0));
        Player far = env.createConnection().connect(instance, new Pos(20, 40, 0));
        Player slender = env.createConnection().connect(instance, new Pos(3, 40, 0));
        PatrolHelper patrol = new PatrolHelper(new Random(1));

        patrol.vanished(new Pos(0, 40, 0), List.of(near, far));

        assertTrue(near.hasEffect(PotionEffect.BLINDNESS));
        assertFalse(far.hasEffect(PotionEffect.BLINDNESS));
        assertFalse(slender.hasEffect(PotionEffect.BLINDNESS), "only survivors go blind");
    }

    /** Builds a two blocks high wall of stone, on the floor of a flat instance. */
    private static void wall(Instance instance, int fromX, int toX, int fromZ, int toZ) {
        for (int x = fromX; x <= toX; x++) {
            for (int z = fromZ; z <= toZ; z++) {
                instance.setBlock(x, 40, z, Block.STONE);
                instance.setBlock(x, 41, z, Block.STONE);
            }
        }
    }

    private static Instance flat(Env env) {
        Instance instance = env.createFlatInstance();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                instance.loadChunk(x, z).join();
            }
        }
        return instance;
    }

    @Test
    @DisplayName("On open ground a fling sends the survivor the full distance away from the creek")
    void flingsAwayFromTheCreek(Env env) {
        Instance instance = flat(env);
        Player selected = env.createConnection().connect(instance, new Pos(8.5, 40, 8.5));
        PatrolHelper patrol = new PatrolHelper(new Random(1));

        assertTrue(patrol.flingAway(selected, new Pos(8.5, 40, 6.5)));

        Vec velocity = selected.getVelocity();
        assertEquals(PatrolHelper.FLING_MAX_DISTANCE * PatrolHelper.FLING_SPEED_PER_BLOCK, velocity.z(), 1.0E-6);
        assertEquals(0.0D, velocity.x(), 1.0E-6);
        assertEquals(PatrolHelper.FLING_LIFT, velocity.y(), 1.0E-6);
    }

    @Test
    @DisplayName("With a tree in the way the fling only goes as far as there is room")
    void flingsOnlyAsFarAsThereIsRoom(Env env) {
        Instance instance = flat(env);
        Player selected = env.createConnection().connect(instance, new Pos(8.5, 40, 8.5));
        wall(instance, 8, 8, 14, 14);
        PatrolHelper patrol = new PatrolHelper(new Random(1));

        assertTrue(patrol.flingAway(selected, new Pos(8.5, 40, 6.5)));

        double distance = selected.getVelocity().z() / PatrolHelper.FLING_SPEED_PER_BLOCK;
        assertTrue(distance >= PatrolHelper.FLING_MIN_DISTANCE && distance < 6.0D, "flung " + distance + " blocks");
    }

    @Test
    @DisplayName("With the way ahead blocked the fling goes to a free side")
    void flingsToAFreeSide(Env env) {
        Instance instance = flat(env);
        Player selected = env.createConnection().connect(instance, new Pos(8.5, 40, 8.5));
        wall(instance, 0, 15, 10, 10);
        PatrolHelper patrol = new PatrolHelper(new Random(1));

        assertTrue(patrol.flingAway(selected, new Pos(8.5, 40, 6.5)));

        Vec velocity = selected.getVelocity();
        assertEquals(0.0D, velocity.z(), 1.0E-6, "straight and diagonal are blocked");
        assertTrue(Math.abs(velocity.x()) >= PatrolHelper.FLING_MIN_DISTANCE * PatrolHelper.FLING_SPEED_PER_BLOCK);
    }

    @Test
    @DisplayName("Boxed in on every side, the survivor is stunned instead")
    void stunsWhenBoxedIn(Env env) {
        Instance instance = flat(env);
        Player selected = env.createConnection().connect(instance, new Pos(8.5, 40, 8.5));
        wall(instance, 6, 11, 6, 6);
        wall(instance, 6, 11, 11, 11);
        wall(instance, 6, 6, 6, 11);
        wall(instance, 11, 11, 6, 11);
        // nextBoolean() is false for 0, so selected() tries the fling first.
        PatrolHelper patrol = new PatrolHelper(() -> 0L);

        patrol.selected(selected, () -> new Pos(8.5, 42.5, 7.5), List.of(selected));

        assertEquals(Vec.ZERO, selected.getVelocity());
        assertTrue(selected.hasEffect(PotionEffect.SLOWNESS));
    }

    @Test
    @DisplayName("Cleaning up removes every slowness and blindness it applied")
    void cleanUpRemovesTheEffects(Env env) {
        Instance instance = env.createFlatInstance();
        Player selected = env.createConnection().connect(instance, new Pos(0, 40, 0));
        Player near = env.createConnection().connect(instance, new Pos(5, 40, 0));
        PatrolHelper patrol = new PatrolHelper(new Random(1));
        patrol.stun(selected, CREEK_EYES, List.of(selected, near));

        patrol.cleanUp();

        assertFalse(selected.hasEffect(PotionEffect.SLOWNESS));
        assertFalse(near.hasEffect(PotionEffect.BLINDNESS));
    }
}
