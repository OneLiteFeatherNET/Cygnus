package net.onelitefeather.cygnus.setup.listener;

import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreekRouteListenerTest {

    @Test
    @DisplayName("A point sits in the middle on top of the clicked block")
    void pointOnTopOfTheBlock() {
        assertEquals(new Vec(3.5, 81, -1.5), CreekRouteListener.pointOnTop(new Vec(3, 80, -2), Block.STONE));
    }

    @Test
    @DisplayName("A point sits on the real height of a lower block")
    void pointOnTopOfALowerBlock() {
        assertEquals(new Vec(3.5, 80.5, -1.5), CreekRouteListener.pointOnTop(new Vec(3, 80, -2), Block.STONE_SLAB));
        assertEquals(new Vec(3.5, 80.9375, -1.5), CreekRouteListener.pointOnTop(new Vec(3, 80, -2), Block.DIRT_PATH));
        assertEquals(new Vec(3.5, 80.0625, -1.5), CreekRouteListener.pointOnTop(new Vec(3, 80, -2), Block.WHITE_CARPET));
    }

    @Test
    @DisplayName("A point on a plant sits where the plant grows")
    void pointOnAPlant() {
        assertEquals(new Vec(3.5, 80, -1.5), CreekRouteListener.pointOnTop(new Vec(3, 80, -2), Block.FERN));
    }
}
