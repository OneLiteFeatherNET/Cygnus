package net.onelitefeather.cygnus.common;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessagesTest {

    @Test
    void testMapAnnouncementWithBuilders() {
        Component message = Messages.getMapAnnouncementMessage("Granskoga", List.of("Alice", "Bob"));

        assertEquals(
                "──────────────────────\nNow playing: Granskoga\nBuilt by: Alice, Bob\n──────────────────────",
                PlainTextComponentSerializer.plainText().serialize(message)
        );
    }

    @Test
    void testMapAnnouncementWithNullBuilders() {
        Component message = Messages.getMapAnnouncementMessage("Granskoga", null);

        assertEquals(
                "──────────────────────\nNow playing: Granskoga\n──────────────────────",
                PlainTextComponentSerializer.plainText().serialize(message)
        );
    }

    @Test
    void testMapAnnouncementWithEmptyBuilders() {
        Component message = Messages.getMapAnnouncementMessage("Granskoga", List.of());

        assertEquals(
                "──────────────────────\nNow playing: Granskoga\n──────────────────────",
                PlainTextComponentSerializer.plainText().serialize(message)
        );
    }

    @Test
    void testMapAnnouncementWidthScalesWithLongestLine() {
        Component message = Messages.getMapAnnouncementMessage(
                "Very Long Map Name Example",
                List.of("Alice", "Bob", "Charlie", "Dave")
        );

        assertEquals(
                "───────────────────────────────────────\nNow playing: Very Long Map Name Example\nBuilt by: Alice, Bob, Charlie, Dave\n───────────────────────────────────────",
                PlainTextComponentSerializer.plainText().serialize(message)
        );
    }
}
