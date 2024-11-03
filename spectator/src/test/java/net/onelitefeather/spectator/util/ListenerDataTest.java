package net.onelitefeather.spectator.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ListenerDataTest {

    @Test
    void testListenerDataCreation() {
        ListenerData listenerData = ListenerData.of(true, true);
        assertTrue(listenerData.detectSpectatorQuit());
        assertFalse(listenerData.detectSpectatorJoin());
        assertTrue(listenerData.detectSpectatorChat());
    }
}
