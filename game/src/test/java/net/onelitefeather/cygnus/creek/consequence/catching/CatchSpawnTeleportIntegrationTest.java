package net.onelitefeather.cygnus.creek.consequence.catching;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatchSpawnTeleportIntegrationTest extends CygnusPlayerTestBase {

    private static final Pos SPAWN = new Pos(30, 45, -20, 180, 0);

    @Test
    @DisplayName("The survivor lands on the slender spawn and loses their velocity")
    void teleportsToTheSpawn(Env env) {
        Instance instance = env.createFlatInstance();
        Player caught = env.createConnection().connect(instance, new Pos(0, 40, 0));
        caught.setVelocity(new Vec(1, 2, 3));

        assertTrue(new CatchSpawnTeleport(() -> SPAWN).perform(caught));
        assertEquals(Vec.ZERO, caught.getVelocity());
        env.tick();

        assertEquals(SPAWN.asVec(), caught.getPosition().asVec());
    }

    @Test
    @DisplayName("The survivor keeps their own view direction, not the one of the spawn")
    void keepsTheView(Env env) {
        Instance instance = env.createFlatInstance();
        Player caught = env.createConnection().connect(instance, new Pos(0, 40, 0, 90, 10));

        new CatchSpawnTeleport(() -> SPAWN).perform(caught);
        env.tick();

        assertEquals(90.0F, caught.getPosition().yaw());
        assertEquals(10.0F, caught.getPosition().pitch());
    }

    @Test
    @DisplayName("Without a slender spawn the teleport does not happen")
    void noSpawn(Env env) {
        Instance instance = env.createFlatInstance();
        Player caught = env.createConnection().connect(instance, new Pos(0, 40, 0));

        assertFalse(new CatchSpawnTeleport(() -> null).perform(caught));
        env.tick();

        assertEquals(new Vec(0, 40, 0), caught.getPosition().asVec());
    }

    @Test
    @DisplayName("A survivor who was just sent to the spawn is not sent there again during the cooldown")
    void refusesDuringTheCooldown(Env env) {
        Instance instance = env.createFlatInstance();
        Player caught = env.createConnection().connect(instance, new Pos(0, 40, 0));
        AtomicLong now = new AtomicLong(1_000L);
        CatchSpawnTeleport teleport = new CatchSpawnTeleport(() -> SPAWN, now::get);

        assertTrue(teleport.perform(caught));
        now.addAndGet(CatchSpawnTeleport.COOLDOWN_MILLIS - 1);

        assertFalse(teleport.perform(caught));
    }

    @Test
    @DisplayName("Once the cooldown is over the survivor can be sent to the spawn again")
    void teleportsAgainAfterTheCooldown(Env env) {
        Instance instance = env.createFlatInstance();
        Player caught = env.createConnection().connect(instance, new Pos(0, 40, 0));
        AtomicLong now = new AtomicLong(1_000L);
        CatchSpawnTeleport teleport = new CatchSpawnTeleport(() -> SPAWN, now::get);

        assertTrue(teleport.perform(caught));
        now.addAndGet(CatchSpawnTeleport.COOLDOWN_MILLIS);

        assertTrue(teleport.perform(caught));
    }

    @Test
    @DisplayName("The cooldown of one survivor does not protect another one")
    void cooldownIsPerSurvivor(Env env) {
        Instance instance = env.createFlatInstance();
        Player first = env.createConnection().connect(instance, new Pos(0, 40, 0));
        Player second = env.createConnection().connect(instance, new Pos(5, 40, 0));
        CatchSpawnTeleport teleport = new CatchSpawnTeleport(() -> SPAWN, () -> 1_000L);

        assertTrue(teleport.perform(first));

        assertTrue(teleport.perform(second));
    }
}
