package net.onelitefeather.cygnus.creek.world;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreekSightIntegrationTest extends CygnusPlayerTestBase {

    @Test
    @DisplayName("A wall between them hides him")
    void wallHidesHim(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createConnection().connect(instance, new Pos(0.5, 40, 0.5, 0, 0));
        EntityCreature creek = new EntityCreature(EntityType.CREAKING);
        creek.setInstance(instance, new Pos(0.5, 40, 10.5)).join();
        CreekSight sight = new CreekSight(48, 35);

        assertTrue(sight.sees(survivor, creek), "nothing stands between them yet");

        for (int y = 40; y < 44; y++) {
            for (int x = -2; x <= 2; x++) {
                instance.setBlock(x, y, 5, Block.STONE);
            }
        }

        assertFalse(sight.sees(survivor, creek), "a wall hides him");
    }
}
