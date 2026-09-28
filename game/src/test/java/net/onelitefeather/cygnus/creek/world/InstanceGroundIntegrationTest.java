package net.onelitefeather.cygnus.creek.world;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    @DisplayName("An unloaded chunk offers no spot")
    void unloadedChunkHasNoSpot(Env env) {
        Instance instance = env.createFlatInstance();
        InstanceGround ground = new InstanceGround(() -> instance);

        assertTrue(ground.settle(new Pos(10_000, 40, 10_000)).isEmpty());
    }
}
