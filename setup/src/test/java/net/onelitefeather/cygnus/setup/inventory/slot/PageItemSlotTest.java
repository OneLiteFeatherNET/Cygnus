package net.onelitefeather.cygnus.setup.inventory.slot;

import de.icevizion.aves.inventory.util.InventoryConstants;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.cygnus.common.page.PageResource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PageItemSlotTest {

    @Test
    void testPageSlot() {
        PageResource pageResource = new PageResource(Pos.ZERO, "NORTH");
        assertNotNull(pageResource);
        assertEquals(Pos.ZERO, pageResource.position());
        assertEquals("NORTH", pageResource.face());
        PageItemSlot pageItemSlot = new PageItemSlot(pageResource, player -> {});

        assertNotNull(pageItemSlot);
        ItemStack slotItem = pageItemSlot.getItem();
        assertNotNull(slotItem);
        assertEquals(Material.PAPER, slotItem.material());
        assertNotEquals(InventoryConstants.CANCEL_CLICK, pageItemSlot.getClick());
    }
}
