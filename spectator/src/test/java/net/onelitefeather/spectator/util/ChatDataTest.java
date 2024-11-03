package net.onelitefeather.spectator.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatDataTest {

    @Test
    void testBasicCreation() {
        ChatData chatData = new ChatData(Component.text("Hello, world!"), Component.empty(), NamedTextColor.GRAY);
        assertNotNull(chatData);
        assertEquals(NamedTextColor.GRAY, chatData.messageColor());
        assertNotEquals(Component.empty(), chatData.prefix());
        assertEquals(Component.empty(), chatData.separator());
    }
}
