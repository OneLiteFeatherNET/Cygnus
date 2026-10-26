package net.onelitefeather.cygnus.pass;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.stats.InMemorySlenderPassRepository;
import net.onelitefeather.cygnus.stats.LedgerReason;
import net.onelitefeather.cygnus.stats.NoOpSlenderPassRepository;
import net.onelitefeather.cygnus.stats.SlenderPassRepository;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the lobby reservation and the round-start claim of the slender pass. Every test builds its own service,
 * repository and players; the balance reads complete at once unless a test queues them on purpose.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.21.0
 */
@ExtendWith(MicrotusExtension.class)
class SlenderPassServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-26T12:00:00Z");
    private static final UUID ROUND = UUID.fromString("00000000-0000-0000-0000-00000000f001");

    private final InMemorySlenderPassRepository repository = new InMemorySlenderPassRepository(Clock.fixed(NOW, ZoneOffset.UTC));
    private final RecordingFeedback feedback = new RecordingFeedback();
    private final List<DebitFailure> failures = new ArrayList<>();
    private final Map<UUID, Player> online = new HashMap<>();
    private final AtomicBoolean lobby = new AtomicBoolean(true);
    private final Deque<Runnable> tickQueue = new ArrayDeque<>();
    private Executor tick = Runnable::run;

    private SlenderPassService service(SlenderPassRepository passes) {
        return new SlenderPassService(passes, tick, online::get, lobby::get, feedback,
                (player, roundId, cause) -> failures.add(new DebitFailure(player, roundId)));
    }

    private void drainTickQueue() {
        while (!tickQueue.isEmpty()) {
            tickQueue.poll().run();
        }
    }

    private static Instance instance(Env env) {
        return env.createFlatInstance();
    }

    private Player join(Env env, Instance instance) {
        Player player = env.createPlayer(instance);
        online.put(player.getUuid(), player);
        return player;
    }

    @Test
    void reservationHoldsTheSlotWhenThePlayerHasAPass(@NotNull Env env) {
        Player alice = join(env, instance(env));
        repository.give(alice.getUuid(), 1).join();
        SlenderPassService service = service(repository);

        service.reserve(alice);

        assertEquals(Optional.of(alice.getUuid()), service.reservedBy());
        assertEquals(1, repository.balance(alice.getUuid()).join(), "a reservation must not spend the pass");
    }

    @Test
    void secondPlayerIsRefusedWhileTheSlotIsHeld(@NotNull Env env) {
        Instance instance = instance(env);
        Player alice = join(env, instance);
        Player bob = join(env, instance);
        repository.give(alice.getUuid(), 1).join();
        repository.give(bob.getUuid(), 1).join();
        SlenderPassService service = service(repository);
        service.reserve(alice);

        service.reserve(bob);

        assertEquals(Optional.of(alice.getUuid()), service.reservedBy(), "the first holder keeps the slot");
        assertEquals(Messages.PASS_TAKEN, feedback.lastTold(bob.getUuid()));
        assertEquals(1, repository.balance(bob.getUuid()).join(), "the refused player keeps the pass");
    }

    @Test
    void playerWithZeroBalanceIsRefused(@NotNull Env env) {
        Player bob = join(env, instance(env));
        SlenderPassService service = service(repository);

        service.reserve(bob);

        assertEquals(Optional.empty(), service.reservedBy());
        assertEquals(Messages.PASS_NO_BALANCE, feedback.lastTold(bob.getUuid()));
    }

    @Test
    void reservationIsRefusedOutsideTheLobby(@NotNull Env env) {
        Player alice = join(env, instance(env));
        repository.give(alice.getUuid(), 1).join();
        lobby.set(false);
        SlenderPassService service = service(repository);

        service.reserve(alice);

        assertEquals(Optional.empty(), service.reservedBy());
        assertEquals(Messages.PASS_CLOSED, feedback.lastTold(alice.getUuid()));
    }

    @Test
    void toggleCancelsTheOwnReservationWithoutChangingTheBalance(@NotNull Env env) {
        Player alice = join(env, instance(env));
        repository.give(alice.getUuid(), 1).join();
        SlenderPassService service = service(repository);
        service.reserve(alice);

        service.toggle(alice);

        assertEquals(Optional.empty(), service.reservedBy());
        assertEquals(1, repository.balance(alice.getUuid()).join());
        assertEquals(Messages.PASS_CANCELLED, feedback.lastTold(alice.getUuid()));
    }

    @Test
    void slotIsFreeAgainAfterTheHolderCancels(@NotNull Env env) {
        Instance instance = instance(env);
        Player alice = join(env, instance);
        Player bob = join(env, instance);
        repository.give(alice.getUuid(), 1).join();
        repository.give(bob.getUuid(), 1).join();
        SlenderPassService service = service(repository);
        service.reserve(alice);
        service.cancel(alice);

        service.reserve(bob);

        assertEquals(Optional.of(bob.getUuid()), service.reservedBy());
    }

    @Test
    void cancelWithoutReservationTellsThePlayer(@NotNull Env env) {
        Player alice = join(env, instance(env));
        SlenderPassService service = service(repository);

        service.cancel(alice);

        assertEquals(Messages.PASS_NOTHING_TO_CANCEL, feedback.lastTold(alice.getUuid()));
    }

    @Test
    void lobbyAnnouncementNamesNoPlayer(@NotNull Env env) {
        Instance instance = instance(env);
        Player alice = join(env, instance);
        join(env, instance);
        repository.give(alice.getUuid(), 1).join();
        SlenderPassService service = service(repository);

        service.reserve(alice);

        RecordingFeedback.Lobby announcement = feedback.lobbyMessages().get(0);
        assertEquals(Messages.PASS_REDEEMED, announcement.message());
        assertEquals(alice.getUuid(), announcement.except(), "the holder hears the reservation as a personal message");
        String text = PlainTextComponentSerializer.plainText().serialize(announcement.message());
        assertFalse(text.contains(alice.getUsername()), "the lobby announcement must not name the holder");
    }

    @Test
    void disconnectReleasesTheReservation(@NotNull Env env) {
        Player alice = join(env, instance(env));
        repository.give(alice.getUuid(), 1).join();
        SlenderPassService service = service(repository);
        service.reserve(alice);

        service.release(alice.getUuid());

        assertEquals(Optional.empty(), service.reservedBy());
    }

    @Test
    void reservationWaitsForTheBalanceReadToBeHandledOnTheTick(@NotNull Env env) {
        tick = tickQueue::add;
        Player alice = join(env, instance(env));
        repository.give(alice.getUuid(), 1).join();
        SlenderPassService service = service(repository);

        service.reserve(alice);

        assertEquals(Optional.empty(), service.reservedBy(), "the decision must wait for the tick");
        drainTickQueue();
        assertEquals(Optional.of(alice.getUuid()), service.reservedBy());
    }

    @Test
    void reservationOfPlayerWhoLeftWhileTheBalanceWasReadIsDropped(@NotNull Env env) {
        tick = tickQueue::add;
        Instance instance = instance(env);
        Player alice = join(env, instance);
        Player bob = join(env, instance);
        repository.give(alice.getUuid(), 1).join();
        repository.give(bob.getUuid(), 1).join();
        SlenderPassService service = service(repository);
        service.reserve(alice);
        online.remove(alice.getUuid());
        service.release(alice.getUuid());

        drainTickQueue();

        assertEquals(Optional.empty(), service.reservedBy(), "an offline player must not hold the slot");
        service.reserve(bob);
        drainTickQueue();
        assertEquals(Optional.of(bob.getUuid()), service.reservedBy(),
                "the slot must be free for the next player after the offline reservation was dropped");
    }

    @Test
    void roundStartSelectsTheHolderAndSpendsOneTimeWithTheRound(@NotNull Env env) {
        Player alice = join(env, instance(env));
        repository.give(alice.getUuid(), 2).join();
        SlenderPassService service = service(repository);
        service.reserve(alice);

        Player slender = service.claimRound(ROUND);

        assertEquals(alice, slender);
        assertEquals(Optional.empty(), service.reservedBy(), "the slot is used up by the round");
        assertEquals(1, repository.balance(alice.getUuid()).join());
        InMemorySlenderPassRepository.LedgerEntry redeem = repository.ledger().get(1);
        assertEquals(LedgerReason.REDEEM, redeem.reason());
        assertEquals(-1, redeem.delta());
        assertEquals(ROUND, redeem.roundId());
        assertTrue(failures.isEmpty());
    }

    @Test
    void roundStartWithoutReservationSelectsNobody(@NotNull Env env) {
        join(env, instance(env));
        SlenderPassService service = service(repository);

        assertNull(service.claimRound(ROUND));
        assertTrue(repository.ledger().isEmpty(), "nothing is debited without a reservation");
    }

    @Test
    void roundStartDropsTheReservationOfAnOfflineHolderWithoutDebit(@NotNull Env env) {
        Player alice = join(env, instance(env));
        repository.give(alice.getUuid(), 1).join();
        SlenderPassService service = service(repository);
        service.reserve(alice);
        online.remove(alice.getUuid());

        assertNull(service.claimRound(ROUND));
        assertEquals(Optional.empty(), service.reservedBy());
        assertEquals(1, repository.balance(alice.getUuid()).join(), "the pass stays with the player");
        assertEquals(1, repository.ledger().size(), "no REDEEM row is written for an offline holder");
    }

    @Test
    void failedDebitKeepsTheRoleForTheRound(@NotNull Env env) {
        Player alice = join(env, instance(env));
        repository.give(alice.getUuid(), 1).join();
        SlenderPassService service = service(repository);
        service.reserve(alice);
        repository.set(alice.getUuid(), 0).join();

        Player slender = service.claimRound(ROUND);

        assertEquals(alice, slender, "the holder plays the Slender even though the debit failed");
        assertEquals(0, repository.balance(alice.getUuid()).join());
        assertTrue(repository.ledger().stream().noneMatch(row -> row.reason() == LedgerReason.REDEEM),
                "a failed debit writes no REDEEM row");
    }

    @Test
    void failedDebitIsReportedWithTheRound(@NotNull Env env) {
        Player alice = join(env, instance(env));
        repository.give(alice.getUuid(), 1).join();
        SlenderPassService service = service(repository);
        service.reserve(alice);
        repository.set(alice.getUuid(), 0).join();

        service.claimRound(ROUND);

        assertEquals(List.of(new DebitFailure(alice.getUuid(), ROUND)), failures);
    }

    @Test
    void lobbyJoinShowsTheBalanceOnTheItem(@NotNull Env env) {
        Player alice = join(env, instance(env));
        repository.give(alice.getUuid(), 3).join();
        SlenderPassService service = service(repository);

        service.onLobbyJoin(alice);

        assertEquals(List.of(new RecordingFeedback.Shown(alice.getUuid(), 3)), feedback.shown());
    }

    @Test
    void disabledFeatureAnswersUnavailable(@NotNull Env env) {
        Player alice = join(env, instance(env));
        SlenderPassService service = service(new NoOpSlenderPassRepository());

        service.reserve(alice);

        assertFalse(service.available());
        assertEquals(Optional.empty(), service.reservedBy());
        assertEquals(Messages.PASS_UNAVAILABLE, feedback.lastTold(alice.getUuid()));
    }

    @Test
    void adminGiveReportsTheNewBalance(@NotNull Env env) {
        Player alice = join(env, instance(env));
        SlenderPassService service = service(repository);
        List<SlenderPassRepository.Adjustment> results = new ArrayList<>();

        service.give(alice.getUuid(), 2, results::add);

        assertEquals(List.of(new SlenderPassRepository.Adjustment(2, 2)), results);
    }

    @Test
    void reservationGetsAnItemRefreshWithTheNewBalance(@NotNull Env env) {
        Player alice = join(env, instance(env));
        repository.give(alice.getUuid(), 2).join();
        SlenderPassService service = service(repository);

        service.reserve(alice);

        assertNotNull(feedback.shown().stream().filter(shown -> shown.balance() == 2).findFirst().orElse(null),
                "the item must show the balance that was read for the reservation");
    }

    private record DebitFailure(UUID player, UUID roundId) {
    }

    /**
     * Records every message the service sends, so the tests can assert on the text and on the audience.
     */
    private static final class RecordingFeedback implements PassFeedback {

        private final List<Sent> personal = new ArrayList<>();
        private final List<Lobby> lobby = new ArrayList<>();
        private final List<Shown> shown = new ArrayList<>();

        record Sent(UUID player, Component message) {
        }

        record Lobby(Component message, UUID except) {
        }

        record Shown(UUID player, int balance) {
        }

        @Override
        public void tell(UUID player, Component message) {
            personal.add(new Sent(player, message));
        }

        @Override
        public void tellLobby(Component message, UUID except) {
            lobby.add(new Lobby(message, except));
        }

        @Override
        public void showBalance(UUID player, int balance) {
            shown.add(new Shown(player, balance));
        }

        Component lastTold(UUID player) {
            for (int index = personal.size() - 1; index >= 0; index--) {
                if (personal.get(index).player().equals(player)) {
                    return personal.get(index).message();
                }
            }
            throw new AssertionError("no message was sent to " + player);
        }

        List<Lobby> lobbyMessages() {
            return lobby;
        }

        List<Shown> shown() {
            return shown;
        }
    }
}
