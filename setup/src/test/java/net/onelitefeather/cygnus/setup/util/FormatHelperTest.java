package net.onelitefeather.cygnus.setup.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FormatHelperTest {

    @Test
    void testFormat() {
        double value = 1.234;
        String formatted = FormatHelper.DECIMAL_FORMAT.format(value);
        assertNotNull(formatted);
        assertNotEquals(String.valueOf(value), formatted);
    }
}
