package net.onelitefeather.cygnus.stats.display;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.SystemChatPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.stats.FakeStatsRepository;
import net.onelitefeather.cygnus.stats.YearStatsView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import static net.onelitefeather.cygnus.stats.display.StatsDisplayTestSupport.texts;
import static net.onelitefeather.cygnus.stats.display.StatsDisplayTestSupport.year;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the cache behind the lobby item, the year the service reads and the round-end line. The
 * repository fake completes its futures at once or, when asked, when the test says so.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.16.0
 */
@ExtendWith(MicrotusExtension.class)
class PlayerStatsServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneId.of("UTC"));

    private final FakeStatsRepository repository = new FakeStatsRepository();
    private Env env;
    private Instance instance;
    private Player player;
    private TestConnection connection;
    private Collector<SystemChatPacket> chat;

    @BeforeEach
    void setUp(Env env) {
        this.env = env;
        this.instance = env.createFlatInstance();
        this.connection = env.createConnection();
        this.player = this.connection.connect(this.instance, new Pos(0, 40, 0));
        this.chat = this.connection.trackIncoming(SystemChatPacket.class);
    }

    private PlayerStatsService service() {
        return service(Runnable::run);
    }

    private PlayerStatsService service(Executor serverThread) {
        return new PlayerStatsService(this.repository, CLOCK, ZoneId.of("UTC"), this.env.process().connection(), serverThread);
    }

    @Test
    @DisplayName("The current year follows the configured zone, not the clock's")
    void currentYearFollowsTheZone() {
        Clock newYearsEve = Clock.fixed(Instant.parse("2025-12-31T23:30:00Z"), ZoneId.of("UTC"));

        PlayerStatsService berlin = new PlayerStatsService(this.repository, newYearsEve, ZoneId.of("Europe/Berlin"),
                this.env.process().connection(), Runnable::run);
        PlayerStatsService utc = new PlayerStatsService(this.repository, newYearsEve, ZoneId.of("UTC"),
                this.env.process().connection(), Runnable::run);

        assertEquals(2026, berlin.currentYear(), "half past midnight in Berlin is already the new year");
        assertEquals(2025, utc.currentYear());
    }

    @Test
    @DisplayName("Joining loads the current year into the cache")
    void preloadFillsTheCache() {
        YearStatsView stored = year(this.player.getUuid(), 2026, 4, 2, 0, 0);
        this.repository.store(stored);
        PlayerStatsService service = service();

        service.preload(this.player);

        assertEquals(Optional.of(stored), service.cachedView(this.player.getUuid()));
    }

    @Test
    @DisplayName("A player without rows is cached as an empty year")
    void preloadCachesAnEmptyYear() {
        PlayerStatsService service = service();

        service.preload(this.player);

        assertEquals(Optional.of(YearStatsView.empty(this.player.getUuid(), 2026)), service.cachedView(this.player.getUuid()));
    }

    @Test
    @DisplayName("Disabled statistics load nothing")
    void preloadSkipsDisabledStatistics() {
        this.repository.enabled = false;
        PlayerStatsService service = service();

        service.preload(this.player);

        assertEquals(0, this.repository.yearReads, "no read when statistics are off");
        assertEquals(Optional.empty(), service.cachedView(this.player.getUuid()));
    }

    @Test
    @DisplayName("A failing preload leaves the cache empty and does not raise")
    void failingPreloadLeavesTheCacheEmpty() {
        this.repository.readFails = new IllegalStateException("database is gone");
        PlayerStatsService service = service();

        service.preload(this.player);

        assertEquals(Optional.empty(), service.cachedView(this.player.getUuid()));
        assertTrue(texts(this.chat).isEmpty(), "a background load does not bother the player");
    }

    @Test
    @DisplayName("Leaving drops the cached view")
    void forgetDropsTheView() {
        PlayerStatsService service = service();
        service.preload(this.player);

        service.forget(this.player);

        assertEquals(Optional.empty(), service.cachedView(this.player.getUuid()));
    }

    @Test
    @DisplayName("A load that finishes after the player left is not cached")
    void lateLoadAfterQuitIsDropped() {
        this.repository.deferReads = true;
        PlayerStatsService service = service();
        service.preload(this.player);

        service.forget(this.player);
        this.repository.completeReads();

        assertEquals(Optional.empty(), service.cachedView(this.player.getUuid()), "the cache is bounded to online players");
    }

    @Test
    @DisplayName("A load that started before a round finished does not bring stale numbers back")
    void staleLoadAfterInvalidateIsDropped() {
        this.repository.deferReads = true;
        PlayerStatsService service = service();
        service.preload(this.player);

        service.invalidate();
        this.repository.completeReads();

        assertEquals(Optional.empty(), service.cachedView(this.player.getUuid()));
    }

    @Test
    @DisplayName("Invalidating clears what was cached")
    void invalidateClearsTheCache() {
        PlayerStatsService service = service();
        service.preload(this.player);

        service.invalidate();

        assertEquals(Optional.empty(), service.cachedView(this.player.getUuid()));
    }

    @Test
    @DisplayName("With a cached view the own menu opens at once without a read")
    void cachedMenuOpensInstantly() {
        this.repository.store(year(this.player.getUuid(), 2026, 4, 2, 0, 0));
        PlayerStatsService service = service();
        service.preload(this.player);
        int reads = this.repository.yearReads;
        this.repository.readFails = new IllegalStateException("database is gone");

        service.openOwnMenu(this.player);

        assertInstanceOf(StatsMenu.class, this.player.getOpenInventory(), "the menu is open");
        assertEquals(reads, this.repository.yearReads, "a cache hit reads nothing");
        assertTrue(texts(this.chat).isEmpty(), "no loading message when the view is cached");
    }

    @Test
    @DisplayName("Without a cached view the player is told it loads and the menu follows")
    void uncachedMenuLoadsFirst() {
        this.repository.store(year(this.player.getUuid(), 2026, 4, 2, 0, 0));
        this.repository.deferReads = true;
        PlayerStatsService service = service();

        service.openOwnMenu(this.player);

        assertTrue(texts(this.chat).getFirst().contains("Loading statistics…"), "the player must see that something happens");
        assertNull(this.player.getOpenInventory(), "nothing to show yet");
        this.repository.completeReads();
        assertInstanceOf(StatsMenu.class, this.player.getOpenInventory(), "the menu opens once the view arrived");
    }

    @Test
    @DisplayName("A menu that loads for a player who already left is not opened")
    void loadForALeftPlayerOpensNothing() {
        this.repository.deferReads = true;
        PlayerStatsService service = service();
        service.openOwnMenu(this.player);

        this.player.remove();
        this.repository.completeReads();

        assertNull(this.player.getOpenInventory());
    }

    @Test
    @DisplayName("A failed menu load tells the player and logs instead of opening")
    void failedMenuLoadIsReported() {
        this.repository.readFails = new IllegalStateException("database is gone");
        PlayerStatsService service = service();

        service.openOwnMenu(this.player);

        assertTrue(texts(this.chat).getLast().contains("Could not load statistics, please try again later."), texts(this.chat).toString());
        assertNull(this.player.getOpenInventory());
    }

    @Test
    @DisplayName("Disabled statistics open no menu")
    void disabledOpensNoMenu() {
        this.repository.enabled = false;
        PlayerStatsService service = service();

        service.openOwnMenu(this.player);

        assertTrue(texts(this.chat).getLast().contains("Statistics are not available on this server."));
        assertNull(this.player.getOpenInventory());
    }

    @Test
    @DisplayName("A cached view of last year is not used once the year turned")
    void yearRolloverBypassesTheCache() {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2025-12-31T12:00:00Z"));
        Clock turning = new Clock() {
            @Override
            public ZoneId getZone() {
                return ZoneId.of("UTC");
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return now.get();
            }
        };
        PlayerStatsService service = new PlayerStatsService(this.repository, turning, ZoneId.of("UTC"),
                this.env.process().connection(), Runnable::run);
        service.preload(this.player);
        assertEquals(2025, service.cachedView(this.player.getUuid()).orElseThrow().year(), "cached before the turn");
        now.set(Instant.parse("2026-01-01T12:00:00Z"));
        this.repository.deferReads = true;

        service.openOwnMenu(this.player);

        assertTrue(texts(this.chat).getFirst().contains("Loading statistics…"), "last year's entry must not be shown");
    }

    @Test
    @DisplayName("The round-end line carries the committed totals of the year")
    void roundTotalsLine() {
        PlayerStatsService service = service();

        service.roundSaved(Map.of(this.player.getUuid(), year(this.player.getUuid(), 2026, 12, 5, 34, 3)));

        String text = texts(this.chat).getFirst();
        assertTrue(text.contains("Your 2026 totals"), text);
        assertTrue(text.contains("12 rounds") && text.contains("5 wins") && text.contains("34 pages") && text.contains("3 kills"), text);
    }

    @Test
    @DisplayName("Players who are not in the totals get no line")
    void roundTotalsSkipOthers() {
        PlayerStatsService service = service();

        service.roundSaved(Map.of(UUID.randomUUID(), year(UUID.randomUUID(), 2026, 1, 0, 0, 0)));

        assertTrue(texts(this.chat).isEmpty(), "this player was not part of the round");
    }

    @Test
    @DisplayName("The committed totals replace the cached view")
    void roundTotalsRefreshTheCache() {
        PlayerStatsService service = service();
        service.preload(this.player);
        YearStatsView committed = year(this.player.getUuid(), 2026, 12, 5, 34, 3);

        service.roundSaved(Map.of(this.player.getUuid(), committed));

        assertEquals(Optional.of(committed), service.cachedView(this.player.getUuid()));
    }

    @Test
    @DisplayName("The round-end line is sent on the server thread, not on the thread that finished the write")
    void roundTotalsHopToTheServerThread() {
        PlayerStatsService service = service(this.env.process().scheduler()::scheduleNextTick);

        service.roundSaved(Map.of(this.player.getUuid(), year(this.player.getUuid(), 2026, 1, 0, 0, 0)));

        assertTrue(texts(this.chat).isEmpty(), "nothing is sent before the next tick");
        Collector<SystemChatPacket> afterTick = this.connection.trackIncoming(SystemChatPacket.class);
        this.env.tick();
        assertEquals(1, texts(afterTick).size(), "the line arrives with the tick");
    }

    @Test
    @DisplayName("Online names are offered for completion")
    void onlineNames() {
        List<String> names = service().onlineNames();

        assertTrue(names.contains(this.player.getUsername()), names.toString());
    }
}
