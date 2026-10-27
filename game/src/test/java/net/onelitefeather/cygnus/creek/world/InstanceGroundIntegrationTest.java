package net.onelitefeather.cygnus.creek.world;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MicrotusExtension.class)
class InstanceGroundIntegrationTest {

    @Test
    @DisplayName("A spot in the air settles onto the floor, centred on its block")
    void settlesOntoTheFloor(Env env) {
        Instance instance = env.createFlatInstance();
        instance.loadChunk(0, 0).join();
        InstanceGround ground = new InstanceGround(() -> instance);

        assertEquals(new Pos(3.5, 40, 3.5), ground.settle(new Pos(3.2, 43, 3.7)).orElseThrow());
    }

    @Test
    @DisplayName("Over open ground nothing stands between two points")
    void openGroundIsClear(Env env) {
        Instance instance = env.createFlatInstance();
        instance.loadChunk(0, 0).join();
        InstanceGround ground = new InstanceGround(() -> instance);

        assertTrue(ground.open(new Pos(1, 41.6, 1), new Pos(1, 41.4, 12)));
    }

    @Test
    @DisplayName("A wall between two points blocks the view")
    void wallBlocks(Env env) {
        Instance instance = env.createFlatInstance();
        instance.loadChunk(0, 0).join();
        for (int y = 40; y < 44; y++) {
            for (int x = -2; x <= 4; x++) {
                instance.setBlock(x, y, 6, Block.STONE);
            }
        }
        InstanceGround ground = new InstanceGround(() -> instance);

        assertFalse(ground.open(new Pos(1, 41.6, 1), new Pos(1, 41.4, 12)));
    }

    @Test
    @DisplayName("An unloaded chunk offers no spot")
    void unloadedChunkHasNoSpot(Env env) {
        Instance instance = env.createFlatInstance();
        InstanceGround ground = new InstanceGround(() -> instance);

        assertTrue(ground.settle(new Pos(10_000, 40, 10_000)).isEmpty());
    }
}
