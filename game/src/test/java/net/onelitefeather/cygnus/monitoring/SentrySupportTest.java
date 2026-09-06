package net.onelitefeather.cygnus.monitoring;

import io.sentry.Sentry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;

class SentrySupportTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void testInitStaysOffWithoutADsn(String dsn) {
        assertFalse(SentrySupport.init(dsn), "Sentry must not be set up without a DSN");
        assertFalse(Sentry.isEnabled(), "no client may be created without a DSN");
    }

    @Test
    void testInitRejectsAMalformedDsn() {
        // Sentry parses the DSN itself and refuses one it cannot make sense of. That must leave
        // reporting off rather than take the game start down with it.
        assertFalse(SentrySupport.init("not-a-dsn"));
        assertFalse(Sentry.isEnabled());
    }
}
