package net.onelitefeather.cygnus.common.page;

import net.minestom.server.coordinate.Vec;
import net.minestom.server.utils.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PageResourceTest {

    @Test
    void testPageResourceCreation() {
        PageResource resource = new PageResource(Vec.ZERO, Direction.EAST);
        assertEquals(Vec.ZERO, resource.position());
        assertNotEquals(Direction.NORTH, resource.face());
    }
}
