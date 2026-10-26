package net.onelitefeather.cygnus.creek.consequence.catching;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}
