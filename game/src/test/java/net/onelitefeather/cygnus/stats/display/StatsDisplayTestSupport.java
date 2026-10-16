package net.onelitefeather.cygnus.stats.display;

import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.tag.TagHandler;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.network.packet.server.play.SystemChatPacket;
import net.minestom.testing.Collector;
import net.onelitefeather.cygnus.stats.YearStatsView;

import java.util.List;
import java.util.UUID;

/**
 * Shared helpers of the display tests.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.16.0
 */
final class StatsDisplayTestSupport {

    private StatsDisplayTestSupport() {
    }

    /**
     * A sender that is not a player and keeps what it was sent.
     */
    record RecordingSender(List<Component> received) implements CommandSender {

        @Override
        public void sendMessage(Component message) {
            this.received.add(message);
        }

        @Override
        public TagHandler tagHandler() {
            return TagHandler.newHandler();
        }

        @Override
        public Identity identity() {
            return Identity.nil();
        }
    }

    /**
     * Reads the chat messages a player received as plain text.
     */
    static List<String> texts(Collector<SystemChatPacket> chat) {
        return chat.collect().stream()
                .map(packet -> PlainTextComponentSerializer.plainText().serialize(packet.message()))
                .toList();
    }

    static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    /**
     * Builds a year with the given rounds, wins, pages and kills and nothing else.
     */
    static YearStatsView year(UUID player, int year, int rounds, int wins, int pages, int kills) {
        return new YearStatsView(player, year, rounds, 0, rounds, 0, wins, 0, 0, 0, 0L, pages, 0, 0, 0, 0L, 0L, 0, 0, 0.0D,
                0, kills, 0, null, 0, 0);
    }
}
