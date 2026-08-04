package net.onelitefeather.cygnus.jumpscare;

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
import net.onelitefeather.cygnus.entity.DeadPlayerMannequin;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit/Integration test for {@link JumpScareManager} and {@link DeadPlayerMannequin}.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
class JumpScareManagerTest extends CygnusPlayerTestBase {

    @Test
    void testRegisterAndUnregisterMannequin(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        DeadPlayerMannequin mannequin = DeadPlayerMannequin.sleeping(player);
        JumpScareManager manager = new JumpScareManager();

        assertTrue(manager.getActiveMannequins().isEmpty());

        manager.register(mannequin);
        assertEquals(1, manager.getActiveMannequins().size());

        manager.unregister(mannequin);
        assertTrue(manager.getActiveMannequins().isEmpty());

        env.destroyInstance(instance, true);
    }

    @Test
    void testCleanUp(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        Pos deathPos = new Pos(10, 64, 10);

        DeadPlayerMannequin mannequin = DeadPlayerMannequin.sleeping(player);
        mannequin.setInstance(instance, deathPos);

        JumpScareManager manager = new JumpScareManager();
        manager.register(mannequin);

        assertEquals(1, manager.getActiveMannequins().size());

        manager.cleanUp();

        assertTrue(manager.getActiveMannequins().isEmpty());
        env.destroyInstance(instance, true);
    }

    @Test
    void testJumpScarePhantomOnlySentToVictim(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Pos deathPos = new Pos(10, 41, 10);

        TestConnection corpseOwnerConn = env.createConnection();
        Player corpseOwner = corpseOwnerConn.connect(instance, deathPos);

        TestConnection victimConn = env.createConnection();
        Player victim = victimConn.connect(instance, new Pos(12, 41, 10));

        TestConnection bystanderConn = env.createConnection();
        Player bystander = bystanderConn.connect(instance, new Pos(11, 41, 10));

        DeadPlayerMannequin corpse = DeadPlayerMannequin.sleeping(corpseOwner);
        corpse.setInstance(instance, deathPos);

        JumpScareManager manager = new JumpScareManager();
        manager.register(corpse);

        // Let auto-viewability establish before triggering, so the "before" state is known.
        for (int i = 0; i < 5; i++) env.tick();
        assertTrue(corpse.isViewer(victim), "corpse should be visible to the victim before the scare");
        assertTrue(corpse.isViewer(bystander), "corpse should be visible to the bystander before the scare");

        Collector<SpawnEntityPacket> victimSpawns = victimConn.trackIncoming(SpawnEntityPacket.class);
        Collector<SpawnEntityPacket> bystanderSpawns = bystanderConn.trackIncoming(SpawnEntityPacket.class);

        assertTrue(manager.force(victim));

        List<SpawnEntityPacket> victimPackets = victimSpawns.collect();
        List<SpawnEntityPacket> bystanderPackets = bystanderSpawns.collect();

        assertEquals(1, victimPackets.size(), "the victim must receive exactly the phantom's spawn packet");
        assertEquals(EntityType.MANNEQUIN, victimPackets.getFirst().type());
        assertTrue(bystanderPackets.isEmpty(), "a bystander must never receive the phantom's spawn packet");

        env.destroyInstance(instance, true);
    }

    @Test
    void testJumpScareHidesOriginalCorpseOnlyFromVictim(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Pos deathPos = new Pos(10, 41, 10);

        TestConnection corpseOwnerConn = env.createConnection();
        Player corpseOwner = corpseOwnerConn.connect(instance, deathPos);

        TestConnection victimConn = env.createConnection();
        Player victim = victimConn.connect(instance, new Pos(12, 41, 10));

        TestConnection bystanderConn = env.createConnection();
        Player bystander = bystanderConn.connect(instance, new Pos(11, 41, 10));

        DeadPlayerMannequin corpse = DeadPlayerMannequin.sleeping(corpseOwner);
        corpse.setInstance(instance, deathPos);

        JumpScareManager manager = new JumpScareManager();
        manager.register(corpse);

        for (int i = 0; i < 5; i++) env.tick();
        assertTrue(corpse.isViewer(victim));
        assertTrue(corpse.isViewer(bystander));

        assertTrue(manager.force(victim));

        assertFalse(corpse.isViewer(victim), "the victim must not see the resting corpse while the phantom stands behind them");
        assertTrue(corpse.isViewer(bystander), "a bystander must keep seeing the resting corpse untouched");

        env.destroyInstance(instance, true);
    }

    @Test
    void testCorpseVisibilityRestoredAfterJumpScareDespawn(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Pos deathPos = new Pos(10, 41, 10);

        TestConnection corpseOwnerConn = env.createConnection();
        Player corpseOwner = corpseOwnerConn.connect(instance, deathPos);

        TestConnection victimConn = env.createConnection();
        Player victim = victimConn.connect(instance, new Pos(12, 41, 10));

        DeadPlayerMannequin corpse = DeadPlayerMannequin.sleeping(corpseOwner);
        corpse.setInstance(instance, deathPos);

        JumpScareManager manager = new JumpScareManager();
        manager.register(corpse);

        for (int i = 0; i < 5; i++) env.tick();
        assertTrue(manager.force(victim));
        assertFalse(corpse.isViewer(victim));

        Collector<DestroyEntitiesPacket> destroyPackets = victimConn.trackIncoming(DestroyEntitiesPacket.class);

        // Despawn is scheduled 50 ticks after the trigger.
        for (int i = 0; i < 55; i++) env.tick();

        assertTrue(corpse.isViewer(victim), "the victim should see the real corpse again once the phantom despawns");
        assertFalse(destroyPackets.collect().isEmpty(), "the victim should receive a destroy packet for the despawned phantom");

        env.destroyInstance(instance, true);
    }
}
