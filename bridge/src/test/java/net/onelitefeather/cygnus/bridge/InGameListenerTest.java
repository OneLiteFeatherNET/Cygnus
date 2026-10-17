package net.onelitefeather.cygnus.bridge;

import net.onelitefeather.cygnus.common.event.RoundInGameEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class InGameListenerTest {

    @Test
    @DisplayName("The first event switches the service")
    void switchesOnFirstEvent() {
        AtomicInteger calls = new AtomicInteger();
        InGameListener listener = new InGameListener(calls::incrementAndGet);

        listener.accept(new RoundInGameEvent());

        assertEquals(1, calls.get(), "switch must run once");
    }

    @Test
    @DisplayName("A second event does not switch again")
    void ignoresSecondEvent() {
        AtomicInteger calls = new AtomicInteger();
        InGameListener listener = new InGameListener(calls::incrementAndGet);

        listener.accept(new RoundInGameEvent());
        listener.accept(new RoundInGameEvent());

        assertEquals(1, calls.get(), "switch must stay idempotent");
    }

    @Test
    @DisplayName("A failing switch is swallowed")
    void swallowsFailure() {
        InGameListener listener = new InGameListener(() -> {
            throw new IllegalStateException("no cloudnet");
        });

        assertDoesNotThrow(() -> listener.accept(new RoundInGameEvent()));
    }

    @Test
    @DisplayName("A failed switch is not retried by a later event")
    void doesNotRetryAfterFailure() {
        AtomicInteger calls = new AtomicInteger();
        InGameListener listener = new InGameListener(() -> {
            calls.incrementAndGet();
            throw new IllegalStateException("no cloudnet");
        });

        listener.accept(new RoundInGameEvent());
        listener.accept(new RoundInGameEvent());

        assertEquals(1, calls.get(), "a failure must not trigger a second service start");
    }
}
