package net.onelitefeather.cygnus.common.config;

import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Shared assertions for the config record tests.
 */
public final class ConfigAssertions {

    private ConfigAssertions() {
    }

    /**
     * Asserts that the creation is rejected with a message naming the given field.
     *
     * @param field    the field the message has to name
     * @param creation the creation of the config that has to fail
     */
    public static void assertRejected(String field, Executable creation) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, creation);
        assertTrue(exception.getMessage().contains(field), exception.getMessage());
    }
}
