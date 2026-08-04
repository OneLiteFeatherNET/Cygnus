package net.onelitefeather.cygnus.common.permission;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Pins the assumption every other test relies on: the LuckPerms loader is kept off the test class
 * path, so Cygnus runs in its LuckPerms-free mode while tests execute.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.6.7
 */
class LuckPermsSupportTest {

    @Test
    void testLoaderIsAbsentDuringTests() {
        assertFalse(LuckPermsSupport.isPresent(), "The test class path must not carry the LuckPerms loader");
    }

    @Test
    void testBootstrapIsSilentWithoutLoader() {
        assertDoesNotThrow(LuckPermsSupport::bootstrap);
    }
}
