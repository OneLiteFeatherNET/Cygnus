package net.onelitefeather.cygnus.monitoring;

import io.sentry.Sentry;
import io.sentry.SentryOptions;
import net.minestom.server.exception.ExceptionHandler;
import net.minestom.server.exception.ExceptionManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class SentrySupportTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void testInitStaysOffWithoutADsn(String dsn) {
        assertFalse(SentrySupport.init(dsn, "1.0.0", "Slender-1", new ExceptionManager()),
                "Sentry must not be set up without a DSN");
        assertFalse(Sentry.isEnabled(), "no client may be created without a DSN");
    }

    @Test
    void testInitRejectsAMalformedDsn() {
        // Sentry parses the DSN itself and refuses one it cannot make sense of. That must leave
        // reporting off rather than take the game start down with it.
        assertFalse(SentrySupport.init("not-a-dsn", "1.0.0", "Slender-1", new ExceptionManager()));
        assertFalse(Sentry.isEnabled());
    }

    @Test
    void testNoDsnLeavesTheDefaultExceptionHandlerUntouched() {
        ExceptionManager manager = new ExceptionManager();
        ExceptionHandler before = manager.getExceptionHandler();

        SentrySupport.init(null, "1.0.0", "Slender-1", manager);

        assertSame(before, manager.getExceptionHandler(), "without a DSN Minestom's handler must stay");
    }

    @Test
    void testHandlerForwardsTheThrowableToCapture() {
        Throwable failure = new IllegalStateException("boom");
        List<Throwable> captured = new ArrayList<>();

        SentrySupport.forwarding(throwable -> { }, captured::add).handleException(failure);

        assertEquals(List.of(failure), captured, "the throwable must reach Sentry");
    }

    @Test
    void testHandlerStillHandlesTheThrowableWithTheDelegate() {
        Throwable failure = new IllegalStateException("boom");
        List<Throwable> handled = new ArrayList<>();

        SentrySupport.forwarding(handled::add, throwable -> { }).handleException(failure);

        assertEquals(List.of(failure), handled, "the previous handler must still log the throwable");
    }

    @Test
    void testHandlerStillHandlesTheThrowableWhenCaptureFails() {
        Throwable failure = new IllegalStateException("boom");
        List<Throwable> handled = new ArrayList<>();

        SentrySupport.forwarding(handled::add, throwable -> {
            throw new IllegalArgumentException("sentry is down");
        }).handleException(failure);

        assertEquals(List.of(failure), handled, "a failing Sentry call must not swallow the log");
    }

    @Test
    void testOptionsCarryReleaseAndServerName() {
        SentryOptions options = new SentryOptions();

        SentrySupport.configure(options, "https://key@example.invalid/1", "2.18.3", "Slender-2");

        assertEquals("cygnus-game@2.18.3", options.getRelease());
        assertEquals("Slender-2", options.getServerName());
    }

    @Test
    void testOptionsLeaveServerNameUnsetWithoutAServiceName() {
        SentryOptions options = new SentryOptions();
        String defaultServerName = options.getServerName();

        SentrySupport.configure(options, "https://key@example.invalid/1", "2.18.3", null);

        assertEquals(defaultServerName, options.getServerName(), "Sentry's own default (hostname) must stay");
    }

    @Test
    void testOptionsLeaveReleaseUnsetForAnUnknownVersion() {
        SentryOptions options = new SentryOptions();

        SentrySupport.configure(options, "https://key@example.invalid/1", "unknown", "Slender-2");

        assertNull(options.getRelease(), "a made-up release would only pollute Sentry");
    }

    @Test
    void testServiceNameIsTheCloudNetDirectoryWithoutItsUniqueId() {
        Path directory = Path.of("temp", "services", "Slender-1_" + UUID.randomUUID());

        assertEquals("Slender-1", SentrySupport.serviceName(directory));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "game", "temp/services/Slender-1", "Slender-1_not-a-uuid"})
    void testServiceNameIsMissingOutsideCloudNet(String directory) {
        assertNull(SentrySupport.serviceName(Path.of(directory)));
    }
}
