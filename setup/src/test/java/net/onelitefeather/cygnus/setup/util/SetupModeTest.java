package net.onelitefeather.cygnus.setup.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class SetupModeTest {

    @ParameterizedTest
    @ValueSource(ints = {-1, 100, 5})
    void testInvalidParseMode(int mode) {
        assertFalse(SetupMode.isMode(SetupMode.LOBBY, mode));
    }

    @Test
    void testIsMode() {
        assertTrue(SetupMode.isMode(SetupMode.LOBBY, 0));
        assertTrue(SetupMode.isMode(SetupMode.GAME, 1));
        assertFalse(SetupMode.isMode(SetupMode.LOBBY, 1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"lobby", "game"})
    void testParseMode(String mode) {
        SetupMode setupMode = SetupMode.parseMode(mode);
        assertNotNull(setupMode);
        assertEquals(mode, setupMode.getName());
    }

    @Test
    void testParseModeInvalid() {
        SetupMode setupMode = SetupMode.parseMode("invalid");
        assertNull(setupMode);
    }

    @Test
    void testGetValues() {
        SetupMode[] values = SetupMode.getValues();
        assertArrayEquals(values, SetupMode.getValues());
    }
}
