package net.onelitefeather.cygnus.setup.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class SetupModeTest {

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
