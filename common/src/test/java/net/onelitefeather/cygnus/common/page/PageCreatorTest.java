package net.onelitefeather.cygnus.common.page;

import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.util.ClearSystemProperty;
import org.junit.jupiter.api.util.SetSystemProperty;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MicrotusExtension.class)
class PageCreatorTest {

    private final PageCreator pageCreator = new PageCreator() {};

    @AfterEach
    @ClearSystemProperty(key = "cygnus.custom_pages")
    void tearDown() {
    }

    @Test
    @SetSystemProperty(key = "cygnus.custom_pages", value = "false")
    void shouldCreateDefaultPageWhenCustomPagesAreDisabled(Env ignored) {
        boolean useCustomPage = Boolean.parseBoolean(System.getProperty("cygnus.custom_pages"));
        ItemStack page = pageCreator.createPageItem(useCustomPage, 5);

        assertEquals(Material.PAPER, page.material());

        assertTrue(page.has(DataComponents.ITEM_MODEL));
        String model = page.get(DataComponents.ITEM_MODEL);
        assertEquals("minecraft:paper", model);
    }

    @Test
    @SetSystemProperty(key = "cygnus.custom_pages", value = "true")
    void shouldAssignCustomModelWhenCustomPagesAreEnabled(Env ignored) {
        boolean useCustomPage = Boolean.parseBoolean(System.getProperty("cygnus.custom_pages"));
        ItemStack page = pageCreator.createPageItem(useCustomPage, 5);

        assertTrue(page.has(DataComponents.ITEM_MODEL));
        String model = page.get(DataComponents.ITEM_MODEL);

        assertNotNull(model);
        assertTrue(model.matches("cygnus:page_[0-5]"));
    }
}