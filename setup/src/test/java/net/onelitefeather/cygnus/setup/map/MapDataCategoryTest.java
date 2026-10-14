package net.onelitefeather.cygnus.setup.map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class MapDataCategoryTest {

    @ParameterizedTest(name = "Test invalid entry: {0}")
    @ValueSource(ints = {-1, 7, 100})
    void testInvalidEntry(int entry) {
        assertThrows(IllegalArgumentException.class, () -> MapDataCategory.byId(entry));
    }

    @Test
    void testValidEntry() {
        assertEquals("Builder", MapDataCategory.byId(1).getName());
    }
}
