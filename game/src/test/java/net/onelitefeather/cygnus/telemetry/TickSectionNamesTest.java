package net.onelitefeather.cygnus.telemetry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Keeps the section names a small, stable set: they are span attribute values and metric labels.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
class TickSectionNamesTest {

    @Test
    @DisplayName("No two sections share a name")
    void namesAreUnique() {
        assertEquals(TickSectionNames.ALL.size(), new HashSet<>(TickSectionNames.ALL).size());
    }

    @Test
    @DisplayName("Every name is lower case words joined by dashes")
    void namesAreKebabCase() {
        TickSectionNames.ALL.forEach(name ->
                assertTrue(name.matches("[a-z]+(-[a-z]+)*"), name + " is not kebab-case"));
    }
}
