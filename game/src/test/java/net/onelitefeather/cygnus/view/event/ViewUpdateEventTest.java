package net.onelitefeather.cygnus.view.event;

import net.minestom.server.event.Event;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ViewUpdateEventTest {

    @Test
    void testEventCreation() {
        ViewUpdateEvent viewUpdateEvent = new ViewUpdateEvent(100);
        assertEquals(100, viewUpdateEvent.ticks());
        assertInstanceOf(Event.class, viewUpdateEvent);
    }
}
