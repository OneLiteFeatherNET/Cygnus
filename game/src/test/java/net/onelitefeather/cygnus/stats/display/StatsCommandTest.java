package net.onelitefeather.cygnus.stats.display;

import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.SystemChatPacket;
import net.minestom.server.network.player.GameProfile;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.stats.FakeStatsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static net.onelitefeather.cygnus.stats.display.StatsDisplayTestSupport.plain;
import static net.onelitefeather.cygnus.stats.display.StatsDisplayTestSupport.texts;
import static net.onelitefeather.cygnus.stats.display.StatsDisplayTestSupport.year;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives {@code /stats} through the command manager of the test environment: who is looked up, which
 * year, and what the sender is told when the lookup cannot be answered. The futures of the fake
 * repository are completed already, so every answer is there when the command returns.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.18.0
 */
@ExtendWith(MicrotusExtension.class)
class StatsCommandTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneId.of("UTC"));
    private static final UUID GHOST = UUID.fromString("00000000-0000-0000-0000-0000000000f1");

    private final FakeStatsRepository repository = new FakeStatsRepository();
    private Env env;
    private Instance instance;
    private Player viewer;
    private Player other;
    private Collector<SystemChatPacket> chat;

    @BeforeEach
    void setUp(Env env) {
        this.env = env;
        this.instance = env.createFlatInstance();
        TestConnection viewerConnection = env.createConnection(new GameProfile(UUID.randomUUID(), "Viewer"));
        this.viewer = viewerConnection.connect(this.instance, new Pos(0, 40, 0));
        this.other = env.createConnection(new GameProfile(UUID.randomUUID(), "Other")).connect(this.instance, new Pos(1, 40, 0));
        this.chat = viewerConnection.trackIncoming(SystemChatPacket.class);
        PlayerStatsService service = new PlayerStatsService(this.repository, CLOCK, ZoneId.of("UTC"),
                env.process().connection(), Runnable::run);
        env.process().command().register(new StatsCommand(service));
    }

    private void run(String command) {
        this.env.process().command().execute(this.viewer, command);
    }

    private String lastText() {
        List<String> texts = texts(this.chat);
        assertTrue(!texts.isEmpty(), "the sender must have been told something");
        return texts.getLast();
    }

    @Test
    @DisplayName("Without arguments the sender sees their own statistics of the current year")
    void selfOverview() {
        this.repository.store(year(this.viewer.getUuid(), 2026, 12, 6, 0, 0));

        run("stats");

        String text = lastText();
        assertTrue(text.contains(this.viewer.getUsername()) && text.contains("2026"), text);
        assertTrue(text.contains("Rounds played: 12"), text);
        assertEquals(0, this.repository.nameReads, "the sender is known, no name lookup needed");
    }

    @Test
    @DisplayName("Another online player is found by name without asking the database for the name")
    void otherOnlinePlayer() {
        this.repository.store(year(this.other.getUuid(), 2026, 7, 3, 0, 0));

        run("stats " + this.other.getUsername().toUpperCase());

        String text = lastText();
        assertTrue(text.contains(this.other.getUsername()), "the name is shown as the player spells it: " + text);
        assertTrue(text.contains("Rounds played: 7"), text);
        assertEquals(0, this.repository.nameReads, "online players are resolved through the server");
    }

    @Test
    @DisplayName("A player who is offline is found through the stored name")
    void otherOfflinePlayer() {
        this.repository.knowPlayer(GHOST, "Ghost");
        this.repository.store(year(GHOST, 2026, 5, 2, 0, 0));

        run("stats ghost");

        String text = lastText();
        assertTrue(text.contains("Ghost") && text.contains("Rounds played: 5"), text);
        assertEquals(1, this.repository.nameReads);
    }

    @Test
    @DisplayName("A year argument reads that year")
    void yearArgument() {
        this.repository.knowPlayer(GHOST, "Ghost");
        this.repository.store(year(GHOST, 2026, 5, 2, 0, 0));
        this.repository.store(year(GHOST, 2025, 3, 1, 0, 0));

        run("stats Ghost 2025");

        String text = lastText();
        assertTrue(text.contains("2025") && text.contains("Rounds played: 3"), text);
    }

    @Test
    @DisplayName("An unknown player gets a message with the name they typed")
    void unknownPlayer() {
        run("stats Nobody");

        assertTrue(lastText().contains("No statistics found for Nobody."), lastText());
    }

    @Test
    @DisplayName("A known player without rows in the year is told so, with the years that exist")
    void noStatisticsInTheYear() {
        this.repository.knowPlayer(GHOST, "Ghost");
        this.repository.store(year(GHOST, 2026, 5, 2, 0, 0));
        this.repository.store(year(GHOST, 2025, 3, 1, 0, 0));

        run("stats Ghost 2024");

        String text = lastText();
        assertTrue(text.contains("Ghost has no statistics for 2024."), text);
        assertTrue(text.contains("2026, 2025"), "the years with statistics are listed: " + text);
    }

    @Test
    @DisplayName("The sender without a round this year is told that they have not played yet")
    void selfWithoutRounds() {
        run("stats");

        assertTrue(lastText().contains("You have not played a round in 2026 yet."), lastText());
    }

    @Test
    @DisplayName("Disabled statistics answer every lookup with the same message")
    void disabled() {
        this.repository.enabled = false;

        run("stats");
        run("stats Ghost 2025");

        List<String> texts = texts(this.chat);
        assertEquals(2, texts.size());
        texts.forEach(text -> assertTrue(text.contains("Statistics are not available on this server."), text));
        assertEquals(0, this.repository.yearReads + this.repository.nameReads, "nothing is read");
    }

    @Test
    @DisplayName("A year that is not a number or out of range prints the usage")
    void invalidYear() {
        run("stats Ghost abc");
        run("stats Ghost 1999");

        List<String> texts = texts(this.chat);
        assertEquals(2, texts.size());
        texts.forEach(text -> assertTrue(text.contains("Usage: /stats [player] [year]"), text));
        assertEquals(0, this.repository.nameReads, "an invalid year is rejected before any lookup");
    }

    @Test
    @DisplayName("A database error is reported generically")
    void databaseError() {
        this.repository.readFails = new IllegalStateException("database is gone");

        run("stats Ghost");

        String text = lastText();
        assertTrue(text.contains("Could not load statistics, please try again later."), text);
        assertTrue(!text.contains("database is gone"), "the cause stays in the log: " + text);
    }

    @Test
    @DisplayName("The console cannot ask for its own statistics")
    void consoleHasNoSelf() {
        List<Component> received = new ArrayList<>();
        CommandSender console = new StatsDisplayTestSupport.RecordingSender(received);

        this.env.process().command().execute(console, "stats");

        assertEquals(1, received.size());
        assertTrue(plain(received.getFirst()).contains("Only players can look at their own statistics."), plain(received.getFirst()));
    }

    @Test
    @DisplayName("The console may look at a named player")
    void consoleMayLookAtAPlayer() {
        List<Component> received = new ArrayList<>();
        CommandSender console = new StatsDisplayTestSupport.RecordingSender(received);
        this.repository.store(year(this.other.getUuid(), 2026, 7, 3, 0, 0));

        this.env.process().command().execute(console, "stats " + this.other.getUsername());

        assertEquals(1, received.size());
        assertTrue(plain(received.getFirst()).contains("Rounds played: 7"), plain(received.getFirst()));
    }

    @Test
    @DisplayName("The menu subcommand opens the menu of the same player and year")
    void menuSubcommand() {
        this.repository.knowPlayer(GHOST, "Ghost");
        this.repository.store(year(GHOST, 2025, 3, 1, 0, 0));

        run("stats open-menu Ghost 2025");

        StatsMenu menu = assertInstanceOf(StatsMenu.class, this.viewer.getOpenInventory(), "the menu must be open");
        String title = plain(menu.getTitle());
        assertTrue(title.contains("Ghost") && title.contains("2025"), title);
    }

    @Test
    @DisplayName("The menu subcommand of an unknown player opens nothing")
    void menuOfUnknownPlayer() {
        run("stats open-menu Nobody 2025");

        assertNull(this.viewer.getOpenInventory(), "there is nothing to show");
        assertTrue(lastText().contains("No statistics found for Nobody."), lastText());
    }

    @Test
    @DisplayName("A player may be called menu")
    void aPlayerCalledMenuIsStillFound() {
        this.repository.knowPlayer(GHOST, "menu");
        this.repository.store(year(GHOST, 2026, 5, 2, 0, 0));

        run("stats menu");

        assertTrue(lastText().contains("Rounds played: 5"), lastText());
    }
}
