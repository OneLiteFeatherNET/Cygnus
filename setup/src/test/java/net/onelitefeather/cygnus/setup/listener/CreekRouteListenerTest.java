package net.onelitefeather.cygnus.setup.listener;

import net.minestom.server.coordinate.Vec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreekRouteListenerTest {

    @Test
    @DisplayName("A point sits in the middle on top of the clicked block")
    void pointOnTopOfTheBlock() {
        assertEquals(new Vec(3.5, 81, -1.5), CreekRouteListener.pointOnTop(new Vec(3, 80, -2)));
    }
}
