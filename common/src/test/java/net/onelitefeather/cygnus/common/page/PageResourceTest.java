package net.onelitefeather.cygnus.common.page;

import net.minestom.server.coordinate.Vec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PageResourceTest {

    @Test
    void testPageResourceCreation() {
        PageResource resource = new PageResource(Vec.ZERO, "test");
        assertEquals(Vec.ZERO, resource.position());
        assertNotEquals("north", resource.face());
    }
}
