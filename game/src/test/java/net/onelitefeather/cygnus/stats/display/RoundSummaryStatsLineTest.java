package net.onelitefeather.cygnus.stats.display;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.SystemChatPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.stats.FakeStatsRepository;
import net.onelitefeather.cygnus.stats.NoOpStatsRepository;
import net.onelitefeather.cygnus.stats.RoundStats.Role;
import net.onelitefeather.cygnus.stats.RoundStatsCollector;
import net.onelitefeather.cygnus.stats.StatsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static net.onelitefeather.cygnus.stats.display.StatsDisplayTestSupport.texts;
import static net.onelitefeather.cygnus.stats.display.StatsDisplayTestSupport.year;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the extra line of the round summary from the collector to the player's chat: it shows the
 * totals the repository committed, and it stays away when the write failed or statistics are off.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.18.0
 */
@ExtendWith(MicrotusExtension.class)
class RoundSummaryStatsLineTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneId.of("UTC"));

    private Env env;
    private Player player;
    private Collector<SystemChatPacket> chat;

    @BeforeEach
    void setUp(Env env) {
        this.env = env;
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        this.player = connection.connect(instance, new Pos(0, 40, 0));
        this.chat = connection.trackIncoming(SystemChatPacket.class);
    }

    private List<String> finishRoundWith(StatsRepository repository) {
        PlayerStatsService service = new PlayerStatsService(repository, CLOCK, ZoneId.of("UTC"),
                this.env.process().connection(), this.env.process().scheduler()::scheduleNextTick);
        RoundStatsCollector collector = new RoundStatsCollector(repository, CLOCK, ZoneId.of("UTC"),
                () -> UUID.randomUUID().toString(), uuid -> this.player.getUsername(), service::roundSaved);
        collector.roundStarted(Map.of(this.player.getUuid(), Role.SURVIVOR));
        collector.gameFinished(GameFinishEvent.Reason.TIME_OVER);
        this.env.tick();
        return texts(this.chat);
    }

    @Test
    @DisplayName("The line shows the totals the repository committed")
    void lineShowsCommittedTotals() {
        FakeStatsRepository repository = new FakeStatsRepository();
        repository.totals = _ -> Map.of(this.player.getUuid(), year(this.player.getUuid(), 2026, 12, 5, 34, 3));

        List<String> texts = finishRoundWith(repository);

        assertEquals(1, texts.size(), "exactly one extra line: " + texts);
        assertTrue(texts.getFirst().contains("12 rounds") && texts.getFirst().contains("5 wins")
                && texts.getFirst().contains("34 pages") && texts.getFirst().contains("3 kills"), texts.getFirst());
    }

    @Test
    @DisplayName("A failed write leaves the summary without the line")
    void failedWriteSkipsTheLine() {
        FakeStatsRepository repository = new FakeStatsRepository();
        repository.saveFails = new IllegalStateException("database is gone");

        List<String> texts = finishRoundWith(repository);

        assertTrue(texts.isEmpty(), "no line, no error message: " + texts);
    }

    @Test
    @DisplayName("Disabled statistics leave the summary without the line")
    void disabledStatisticsSkipTheLine() {
        List<String> texts = finishRoundWith(new NoOpStatsRepository());

        assertTrue(texts.isEmpty(), "nothing was stored, nothing to show: " + texts);
    }
}
