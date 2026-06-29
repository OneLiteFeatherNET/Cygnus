package net.onelitefeather.cygnus.setup.map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class MapDataCategoryTest {

    @ParameterizedTest(name = "Test invalid entry: {0}")
    @ValueSource(ints = {-1, 6, 100})
    void testInvalidEntry(int entry) {
        assertThrows(IllegalArgumentException.class, () -> MapDataCategory.byId(entry));
    }

    @Test
    void testValidEntry() {
        MapDataCategory category = MapDataCategory.byId(1);
        assertNotNull(category);
        assertEquals("Builder", category.getName());
    }
}
