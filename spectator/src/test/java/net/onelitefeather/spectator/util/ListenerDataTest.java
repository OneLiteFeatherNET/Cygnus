package net.onelitefeather.spectator.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ListenerDataTest {

    @Test
    void testListenerDataCreation() {
        ListenerData listenerData = ListenerData.of(true, true);
        assertTrue(listenerData.detektSpectatorQuit());
        assertFalse(listenerData.detektSpectatorJoin());
        assertTrue(listenerData.detektSpectatorChat());
    }
}
