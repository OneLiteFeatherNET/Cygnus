package net.onelitefeather.cygnus.pass;

import net.kyori.adventure.text.Component;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.common.config.SlenderPassConfig;
import net.onelitefeather.cygnus.stats.InMemorySlenderPassRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the monthly rank quota with a fixed clock and a fake LuckPerms meta. Every test has its own repository and
 * feedback and runs its completions on the calling thread.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.21.0
 */
class MonthlyQuotaTest {

    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final Instant OCTOBER = Instant.parse("2026-10-15T10:00:00Z");

    private final InMemorySlenderPassRepository repository = new InMemorySlenderPassRepository(clockAt(OCTOBER));
    private final RecordingFeedback feedback = new RecordingFeedback();

    @Test
    void theFirstJoinOfTheMonthCreditsTheQuotaAndSaysSo() {
        quota(clockAt(OCTOBER), player -> 2, 5).onLobbyJoin(ALICE);

        assertEquals(2, this.repository.balance(ALICE).join());
        assertEquals(List.of(Messages.passEarnedQuota(2)), this.feedback.messagesTo(ALICE));
    }

    @Test
    void aSecondJoinInTheSameMonthCreditsNothing() {
        MonthlyQuota quota = quota(clockAt(OCTOBER), player -> 2, 5);
        quota.onLobbyJoin(ALICE);

        quota.onLobbyJoin(ALICE);

        assertEquals(2, this.repository.balance(ALICE).join());
        assertEquals(1, this.feedback.messagesTo(ALICE).size(), "only the first join of the month is announced");
    }

    @Test
    void missedMonthsAreNotPaidBack() {
        quota(clockAt(Instant.parse("2026-03-10T10:00:00Z")), player -> 2, 10).onLobbyJoin(ALICE);

        quota(clockAt(OCTOBER), player -> 2, 10).onLobbyJoin(ALICE);

        assertEquals(4, this.repository.balance(ALICE).join(), "March and October are paid, the months in between are not");
    }

    @Test
    void theMonthChangesAtMidnightUtc() {
        quota(clockAt(Instant.parse("2026-10-31T23:30:00Z")), player -> 1, 5).onLobbyJoin(ALICE);

        quota(clockAt(Instant.parse("2026-11-01T00:30:00Z")), player -> 1, 5).onLobbyJoin(ALICE);

        assertEquals(2, this.repository.balance(ALICE).join(), "October and November are two months, so two quotas");
    }

    @Test
    void aQuotaOfZeroMarksTheMonthAndCreditsNothing() {
        quota(clockAt(OCTOBER), player -> 0, 5).onLobbyJoin(ALICE);

        quota(clockAt(OCTOBER), player -> 2, 5).onLobbyJoin(ALICE);

        assertEquals(0, this.repository.balance(ALICE).join(), "the month is already marked, so the later quota waits for November");
        assertTrue(this.feedback.messagesTo(ALICE).isEmpty());
    }

    private MonthlyQuota quota(Clock clock, MonthlyQuota.MonthlyQuotaSource source, int maxBalance) {
        InMemorySlenderPassRepository passes = this.repository;
        return new MonthlyQuota(passes, clock, source, new SlenderPassConfig(10, maxBalance), Runnable::run, this.feedback);
    }

    private static Clock clockAt(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }

    private static final class RecordingFeedback implements PassFeedback {

        private final List<UUID> recipients = new ArrayList<>();
        private final List<Component> messages = new ArrayList<>();

        @Override
        public void tell(UUID player, Component message) {
            this.recipients.add(player);
            this.messages.add(message);
        }

        @Override
        public void tellLobby(Component message, UUID except) {
            throw new AssertionError("the quota never speaks to the lobby");
        }

        @Override
        public void showBalance(UUID player, int balance) {
            throw new AssertionError("the quota never shows a balance on the item");
        }

        List<Component> messagesTo(UUID player) {
            List<Component> sent = new ArrayList<>();
            for (int index = 0; index < this.recipients.size(); index++) {
                if (this.recipients.get(index).equals(player)) {
                    sent.add(this.messages.get(index));
                }
            }
            return sent;
        }
    }
}
