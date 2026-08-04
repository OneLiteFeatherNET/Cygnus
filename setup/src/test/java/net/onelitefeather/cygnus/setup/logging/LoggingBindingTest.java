package net.onelitefeather.cygnus.setup.logging;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.NOPLogger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that this module ships an SLF4J binding.
 *
 * <p>SLF4J without a provider does not fail — it prints a warning once and discards every message
 * from then on, which looks exactly like a server that has nothing to say. The only way that
 * regression announces itself is a test that asks.</p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.6.7
 */
class LoggingBindingTest {

    @Test
    void testALoggerIsBound() {
        Logger logger = LoggerFactory.getLogger(LoggingBindingTest.class);

        assertFalse(logger instanceof NOPLogger, "No SLF4J provider on the runtime classpath");
        assertTrue(logger.isInfoEnabled(), "Info has to reach the log, or the server runs blind");
    }
}
