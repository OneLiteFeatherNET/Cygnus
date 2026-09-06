package net.onelitefeather.cygnus.common.page;

import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MicrotusExtension.class)
class PageCreatorTest {

    private final PageCreator pageCreator = new PageCreator() {};

    @Test
    void testCustomPageCreation(Env ignored) {
        for (int i = 0; i < 100; i++) {
            ItemStack page = pageCreator.createPageItem(5);

            assertEquals(Material.PAPER, page.material());
            assertTrue(page.has(DataComponents.ITEM_MODEL));

            String model = page.get(DataComponents.ITEM_MODEL);

            assertNotNull(model);
            assertTrue(model.matches("cygnus:page_[1-6]"));
        }
    }
}
