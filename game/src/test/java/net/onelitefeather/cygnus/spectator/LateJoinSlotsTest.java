package net.onelitefeather.cygnus.spectator;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link LateJoinSlots}. Every test builds its own instance and runs without any clock or sleep.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.21.0
 */
class LateJoinSlotsTest {

    @Test
    void reservingFreePlaceSucceeds() {
        LateJoinSlots slots = new LateJoinSlots(2);
        UUID player = UUID.randomUUID();

        assertTrue(slots.tryReserve(player), "A free place must be granted to a new player");
        assertTrue(slots.holds(player), "The player must hold the place after a successful reservation");
    }

    @Test
    void reservingAgainForSamePlayerUsesNoSecondPlace() {
        LateJoinSlots slots = new LateJoinSlots(2);
        UUID player = UUID.randomUUID();
        slots.tryReserve(player);

        assertTrue(slots.tryReserve(player), "A player already holding a place must be reported as holding it");
        assertEquals(1, slots.size(), "A repeated reservation must not consume a second place");
    }

    @Test
    void fullSetRefusesNewPlayer() {
        LateJoinSlots slots = new LateJoinSlots(1);
        slots.tryReserve(UUID.randomUUID());

        assertFalse(slots.tryReserve(UUID.randomUUID()), "A new player must be refused when all places are taken");
    }

    @Test
    void limitZeroIsDisabled() {
        LateJoinSlots slots = new LateJoinSlots(0);

        assertFalse(slots.enabled(), "A limit of 0 must report the feature as disabled");
    }

    @Test
    void limitZeroRefusesReservation() {
        LateJoinSlots slots = new LateJoinSlots(0);

        assertFalse(slots.tryReserve(UUID.randomUUID()), "A disabled feature must refuse every reservation");
    }

    @Test
    void releaseFreesPlaceForNextPlayer() {
        LateJoinSlots slots = new LateJoinSlots(1);
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        slots.tryReserve(first);

        slots.release(first);

        assertTrue(slots.tryReserve(second), "Releasing a place must make it available to the next player");
    }

    @Test
    void releasingPlayerWithoutPlaceChangesNothing() {
        LateJoinSlots slots = new LateJoinSlots(2);
        UUID holder = UUID.randomUUID();
        slots.tryReserve(holder);

        slots.release(UUID.randomUUID());

        assertEquals(1, slots.size(), "Releasing a player who holds nothing must not change the count");
        assertTrue(slots.holds(holder), "Releasing a stranger must not remove an existing holder");
    }

    @Test
    void clearEmptiesSet() {
        LateJoinSlots slots = new LateJoinSlots(2);
        UUID player = UUID.randomUUID();
        slots.tryReserve(player);

        slots.clear();

        assertEquals(0, slots.size(), "Clear must remove every reserved place");
        assertFalse(slots.holds(player), "Clear must remove the holder as well");
    }

    @Test
    void negativeLimitThrows() {
        assertThrows(IllegalArgumentException.class, () -> new LateJoinSlots(-1),
                "A negative limit must be rejected");
    }

    @Test
    void competingThreadsGetExactlyOneLastPlace() throws Exception {
        LateJoinSlots slots = new LateJoinSlots(1);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger winners = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> compete(slots, startGate, winners));
            Future<?> second = executor.submit(() -> compete(slots, startGate, winners));
            startGate.countDown();
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        assertEquals(1, winners.get(), "Exactly one of two players competing for the last place must win it");
        assertEquals(1, slots.size(), "The set must hold exactly the one winning player");
    }

    private static void compete(LateJoinSlots slots, CountDownLatch startGate, AtomicInteger winners) {
        try {
            startGate.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        if (slots.tryReserve(UUID.randomUUID())) {
            winners.incrementAndGet();
        }
    }
}
