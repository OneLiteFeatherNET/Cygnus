package net.onelitefeather.cygnus.setup.map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class MapDataCategoryTest {

    @ParameterizedTest(name = "Test invalid entry: {0}")
    @ValueSource(ints = {-1, 9, 100})
    void testInvalidEntry(int entry) {
        assertThrows(IllegalArgumentException.class, () -> MapDataCategory.byId(entry));
    }

    @Test
    void testValidEntry() {
        assertEquals("Builder", MapDataCategory.byId(1).getName());
    }

    @Test
    void testSettingsCategoriesComeLast() {
        assertEquals(MapDataCategory.PAGE_SETTINGS, MapDataCategory.byId(7));
        assertEquals(MapDataCategory.CREEK_SETTINGS, MapDataCategory.byId(8));
    }
}
