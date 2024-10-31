package net.onelitefeather.cygnus.stamina;

import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StaminaColorsTest {

    @Test
    void testDrainingColors() {
        StaminaColors draining = StaminaColors.DRAINING;
        assertEquals(NamedTextColor.GOLD, draining.getCompleteColor());
        assertNotEquals(NamedTextColor.RED, draining.getEmptyColor());
    }
}
