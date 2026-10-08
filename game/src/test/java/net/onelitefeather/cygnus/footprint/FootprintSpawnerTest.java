package net.onelitefeather.cygnus.footprint;

import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.play.BlockChangePacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FootprintSpawnerTest extends CygnusPlayerTestBase {

    private static Instance flat(Env env) {
        Instance instance = env.createFlatInstance();
        instance.loadChunk(0, 0).join();
        return instance;
    }

    private static void tick(Env env, int ticks) {
        for (int i = 0; i < ticks; i++) env.tick();
    }

    private static FootprintSpawner spawner() {
        return new FootprintSpawner(new FixedRandom(0.0D), 0.33D);
    }

    @Test
    @DisplayName("A print on bare ground goes into the air block above it")
    void spawnsOnGround(Env env) {
        Instance instance = flat(env);
        FootprintSpawner spawner = spawner();

        assertTrue(spawner.spawn(instance, new Pos(0.5, 40, 0.5), FootprintKind.SLENDER, 12, _ -> true));

        assertEquals(1, spawner.live().size());
        Footprint footprint = spawner.live().getFirst();
        assertEquals(new BlockVec(0, 40, 0), footprint.block());
        assertEquals("north", footprint.state().getProperty("facing"));
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("No print where the ground is covered")
    void skipsCoveredGround(Env env) {
        Instance instance = flat(env);
        instance.setBlock(0, 40, 0, Block.SHORT_GRASS);
        FootprintSpawner spawner = spawner();

        assertFalse(spawner.spawn(instance, new Pos(0.5, 40, 0.5), FootprintKind.SLENDER, 12, _ -> true));
        assertTrue(spawner.live().isEmpty());
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("A second print of the same kind on the same block replaces the first and stays visible")
    void replacesOnSameBlock(Env env) {
        Instance instance = flat(env);
        TestConnection connection = env.createConnection();
        Player viewer = connection.connect(instance, new Pos(0.5, 40, 3.5));
        FootprintSpawner spawner = spawner();
        spawner.spawn(instance, new Pos(0.2, 40, 0.2), FootprintKind.SLENDER, 12, _ -> true);
        Footprint first = spawner.live().getFirst();
        Collector<BlockChangePacket> packets = connection.trackIncoming(BlockChangePacket.class);

        spawner.spawn(instance, new Pos(0.8, 40, 0.8), FootprintKind.SLENDER, 12, _ -> true);

        assertEquals(1, spawner.live().size());
        Footprint second = spawner.live().getFirst();
        assertNotSame(first, second);
        List<BlockChangePacket> sent = packets.collect();
        assertEquals(2, sent.size());
        assertEquals(Block.AIR.stateId(), sent.getFirst().blockStateId(), "the old print goes first");
        assertEquals(second.state().stateId(), sent.getLast().blockStateId(), "the new print is shown last");
        assertTrue(second.isShownTo(viewer));
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("Prints of both kinds may share a block")
    void kindsShareBlock(Env env) {
        Instance instance = flat(env);
        FootprintSpawner spawner = spawner();

        spawner.spawn(instance, new Pos(0.5, 40, 0.5), FootprintKind.SLENDER, 12, _ -> true);
        spawner.spawn(instance, new Pos(0.5, 40, 0.5), FootprintKind.SURVIVOR, 12, _ -> true);

        assertEquals(2, spawner.live().size());
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("The task fades a print at its share and removes it at the end of its lifetime")
    void fadesAndRemoves(Env env) {
        Instance instance = flat(env);
        FootprintSpawner spawner = spawner();
        // A lifetime of 3 s with a share of 0.33 fades after 2 s.
        spawner.spawn(instance, new Pos(0.5, 40, 0.5), FootprintKind.SLENDER, 3, _ -> true);
        Footprint footprint = spawner.live().getFirst();

        tick(env, 20 + 1);
        assertFalse(footprint.faded());

        tick(env, 20);
        assertTrue(footprint.faded());

        tick(env, 20);
        assertTrue(spawner.live().isEmpty());
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("A delayed print shows up only after its delay")
    void spawnsLater(Env env) {
        Instance instance = flat(env);
        FootprintSpawner spawner = spawner();

        spawner.spawnLater(5, instance, new Pos(0.5, 40, 0.5), FootprintKind.SLENDER, 12, _ -> true);
        tick(env, 2);
        assertTrue(spawner.live().isEmpty());

        tick(env, 6);
        assertEquals(1, spawner.live().size());
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("Clearing gives every viewer the real block back, also for faded prints, and cancels delayed ones")
    void clear(Env env) {
        Instance instance = flat(env);
        TestConnection connection = env.createConnection();
        connection.connect(instance, new Pos(0.5, 40, 3.5));
        FootprintSpawner spawner = spawner();
        spawner.spawn(instance, new Pos(0.5, 40, 0.5), FootprintKind.SLENDER, 3, _ -> true);
        spawner.spawn(instance, new Pos(2.5, 40, 0.5), FootprintKind.SURVIVOR, 12, _ -> true);
        tick(env, 2 * 20 + 1);
        spawner.spawnLater(5, instance, new Pos(4.5, 40, 0.5), FootprintKind.SLENDER, 12, _ -> true);
        Collector<BlockChangePacket> packets = connection.trackIncoming(BlockChangePacket.class);

        spawner.clear();
        tick(env, 8);

        List<BlockChangePacket> sent = packets.collect();
        assertEquals(2, sent.size());
        assertTrue(sent.stream().allMatch(packet -> packet.blockStateId() == Block.AIR.stateId()));
        assertTrue(spawner.live().isEmpty(), "the cancelled print must not appear");
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("A reloaded chunk gets its prints again, only for players who may see them")
    void resendsOnChunkLoad(Env env) {
        Instance instance = flat(env);
        TestConnection viewerConnection = env.createConnection();
        Player viewer = viewerConnection.connect(instance, new Pos(0.5, 40, 3.5));
        TestConnection otherConnection = env.createConnection();
        Player other = otherConnection.connect(instance, new Pos(1.5, 40, 3.5));
        FootprintSpawner spawner = spawner();
        spawner.spawn(instance, new Pos(0.5, 40, 0.5), FootprintKind.SLENDER, 12, player -> player == viewer);
        Collector<BlockChangePacket> viewerPackets = viewerConnection.trackIncoming(BlockChangePacket.class);
        Collector<BlockChangePacket> otherPackets = otherConnection.trackIncoming(BlockChangePacket.class);

        spawner.resend(viewer, 0, 0);
        spawner.resend(viewer, 5, 5);
        spawner.resend(other, 0, 0);

        viewerPackets.assertSingle(packet -> assertEquals(new BlockVec(0, 40, 0), packet.blockPosition()));
        otherPackets.assertEmpty();
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("A player who left gets nothing when the print goes")
    void forgetsLeaver(Env env) {
        Instance instance = flat(env);
        TestConnection connection = env.createConnection();
        Player leaver = connection.connect(instance, new Pos(0.5, 40, 3.5));
        FootprintSpawner spawner = spawner();
        spawner.spawn(instance, new Pos(0.5, 40, 0.5), FootprintKind.SLENDER, 12, _ -> true);
        Footprint footprint = spawner.live().getFirst();

        spawner.forget(leaver);
        Collector<BlockChangePacket> packets = connection.trackIncoming(BlockChangePacket.class);
        spawner.clear();

        assertFalse(footprint.isShownTo(leaver));
        packets.assertEmpty();
        env.destroyInstance(instance, true);
    }
}
