package net.onelitefeather.cygnus.pass;

import net.kyori.adventure.text.Component;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.common.config.SlenderPassConfig;
import net.onelitefeather.cygnus.stats.FinishReason;
import net.onelitefeather.cygnus.stats.FinishedRound;
import net.onelitefeather.cygnus.stats.InMemorySlenderPassRepository;
import net.onelitefeather.cygnus.stats.RoundStats.Role;
import net.onelitefeather.cygnus.stats.RoundStatsTracker;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the pity counter on the round snapshots the statistics build. Every test has its own repository, feedback and
 * tracker, and runs its completions on the calling thread.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.21.0
 */
class PityCounterTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-26T12:00:00Z"), ZoneOffset.UTC);
    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
    private static final UUID ROUND = UUID.fromString("00000000-0000-0000-0000-00000000f001");

    private final InMemorySlenderPassRepository repository = new InMemorySlenderPassRepository(CLOCK);
    private final RecordingFeedback feedback = new RecordingFeedback();

    @Test
    void theRoundThatReachesThePityThresholdCreditsOnePassAndSaysSo() {
        PityCounter counter = counter(3, 5);

        for (int round = 0; round < 3; round++) {
            counter.roundFinished(playRound(Map.of(ALICE, Role.SURVIVOR, BOB, Role.SLENDER), tracker -> { }));
        }

        assertEquals(1, this.repository.balance(ALICE).join());
        assertEquals(List.of(Messages.passEarnedPity(1)), this.feedback.messagesTo(ALICE));
    }

    @Test
    void aRoundBelowTheThresholdCreditsNothing() {
        PityCounter counter = counter(3, 5);

        for (int round = 0; round < 2; round++) {
            counter.roundFinished(playRound(Map.of(ALICE, Role.SURVIVOR, BOB, Role.SLENDER), tracker -> { }));
        }

        assertEquals(0, this.repository.balance(ALICE).join());
        assertTrue(this.feedback.messagesTo(ALICE).isEmpty());
    }

    @Test
    void aPlayerWhoQuitsBeforeTheEndIsNotCounted() {
        PityCounter counter = counter(2, 5);
        counter.roundFinished(playRound(Map.of(ALICE, Role.SURVIVOR, BOB, Role.SLENDER), tracker -> { }));

        counter.roundFinished(playRound(Map.of(ALICE, Role.SURVIVOR, BOB, Role.SLENDER), tracker -> tracker.playerLeft(ALICE)));

        assertEquals(0, this.repository.balance(ALICE).join(), "the quit round must not reach the threshold");
        counter.roundFinished(playRound(Map.of(ALICE, Role.SURVIVOR, BOB, Role.SLENDER), tracker -> { }));
        assertEquals(1, this.repository.balance(ALICE).join(), "the second full round reaches the threshold");
    }

    @Test
    void aRoundAsSlenderResetsTheCounter() {
        PityCounter counter = counter(2, 5);
        counter.roundFinished(playRound(Map.of(ALICE, Role.SURVIVOR, BOB, Role.SLENDER), tracker -> { }));

        counter.roundFinished(playRound(Map.of(ALICE, Role.SLENDER, BOB, Role.SURVIVOR), tracker -> { }));
        counter.roundFinished(playRound(Map.of(ALICE, Role.SURVIVOR, BOB, Role.SLENDER), tracker -> { }));

        assertEquals(0, this.repository.balance(ALICE).join(), "the Slender round starts the count again");
    }

    @Test
    void aTakeoverDuringTheRoundResetsTheCounter() {
        PityCounter counter = counter(2, 5);
        counter.roundFinished(playRound(Map.of(ALICE, Role.SURVIVOR, BOB, Role.SLENDER), tracker -> { }));

        counter.roundFinished(playRound(Map.of(ALICE, Role.SURVIVOR, BOB, Role.SLENDER), tracker -> tracker.slenderTakenOver(ALICE)));
        counter.roundFinished(playRound(Map.of(ALICE, Role.SURVIVOR, BOB, Role.SLENDER), tracker -> { }));

        assertEquals(0, this.repository.balance(ALICE).join(), "a survivor who was taken over starts the count again");
    }

    @Test
    void aPassAtTheCapIsNeitherCreditedNorAnnounced() {
        this.repository.give(ALICE, 5).join();
        PityCounter counter = counter(1, 5);

        counter.roundFinished(playRound(Map.of(ALICE, Role.SURVIVOR, BOB, Role.SLENDER), tracker -> { }));

        assertEquals(5, this.repository.balance(ALICE).join());
        assertTrue(this.feedback.messagesTo(ALICE).isEmpty());
    }

    private PityCounter counter(int pityRounds, int maxBalance) {
        return new PityCounter(this.repository, new SlenderPassConfig(pityRounds, maxBalance), Runnable::run, this.feedback);
    }

    private static FinishedRound playRound(Map<UUID, Role> players, Consumer<RoundStatsTracker> events) {
        RoundStatsTracker tracker = new RoundStatsTracker(CLOCK, ZoneOffset.UTC);
        tracker.roundStarted(players);
        events.accept(tracker);
        return tracker.finish(FinishReason.TIME_OVER, () -> ROUND).orElseThrow();
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
            throw new AssertionError("the pity counter never speaks to the lobby");
        }

        @Override
        public void showBalance(UUID player, int balance) {
            throw new AssertionError("the pity counter never shows a balance on the item");
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
