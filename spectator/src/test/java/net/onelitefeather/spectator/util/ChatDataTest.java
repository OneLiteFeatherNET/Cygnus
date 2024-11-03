package net.onelitefeather.spectator.util;

import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatDataTest {

    @Test
    void testBasicCreation() {
        ChatData chatData = new ChatData(Component.text("Hello, world!"), Component.empty());
        assertNotNull(chatData);
        assertNotEquals(Component.empty(), chatData.prefix());
        assertEquals(Component.empty(), chatData.separator());
    }
}
