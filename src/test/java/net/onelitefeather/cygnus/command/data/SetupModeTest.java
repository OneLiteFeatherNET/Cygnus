package net.onelitefeather.cygnus.command.data;

import net.onelitefeather.cygnus.setup.SetupMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SetupModeTest {

    @Test
    void testModeParsingWhichReturnsNull() {
        assertNull(SetupMode.parseMode("End"));
    }

    @Test
    void testModeParsingWhichIsValid() {
        var partString = "game";
        var parsedPart = SetupMode.parseMode(partString);
        assertNotNull(parsedPart);
        assertEquals(partString, parsedPart.getName());
    }

    @Test
    void testGetValuesCall() {
        var values = SetupMode.values();
        assertArrayEquals(values, SetupMode.getValues());
        assertEquals(values.length, SetupMode.getValues().length);
    }

}