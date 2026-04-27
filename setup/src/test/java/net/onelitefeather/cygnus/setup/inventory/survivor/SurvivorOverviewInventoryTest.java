package net.onelitefeather.cygnus.setup.inventory.survivor;

import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.common.map.GameMapBuilder;
import net.onelitefeather.cygnus.setup.SetupPlayerTestBase;
import net.theevilreaper.aves.inventory.InventoryLayout;
import net.theevilreaper.aves.inventory.function.InventoryClick;
import net.theevilreaper.aves.inventory.slot.ISlot;
import net.theevilreaper.aves.inventory.util.InventoryConstants;
import net.theevilreaper.aves.inventory.util.LayoutCalculator;
import org.junit.jupiter.api.Test;

import static net.onelitefeather.cygnus.setup.util.SetupItems.DECORATION;
import static org.junit.jupiter.api.Assertions.*;

class SurvivorOverviewInventoryTest extends SetupPlayerTestBase {

    @Test
    void testSurvivorOverviewLayout(Env env) {
        Instance instance = env.createEmptyInstance();
        Player player = env.createPlayer(instance);

        GameMapBuilder gameMapBuilder = new GameMapBuilder();
        SurvivorOverviewInventory inventory = new SurvivorOverviewInventory(player, gameMapBuilder);

        InventoryLayout layout = inventory.getLayout();
        assertNotNull(layout, "The inventory layout should not be null");

        int[] frameSlots = LayoutCalculator.frame(0, layout.getSize() - 1);

        for (int i = 0; i < frameSlots.length; i++) {
            ISlot currentSlot = layout.getSlot(frameSlots[i]);
            assertNotNull(currentSlot, "Slot " + frameSlots[i] + " should not be null");
            ItemStack currentItem = currentSlot.getItem();
            assertNotNull(currentItem, "Item in slot " + frameSlots[i] + " should not be null");
            assertEquals(DECORATION, currentItem);

            InventoryClick currentClick = currentSlot.getClick();
            assertNotNull(currentClick, "Click in slot " + frameSlots[i] + " should not be null");
            assertEquals(InventoryConstants.CANCEL_CLICK, currentClick);
        }
        env.destroyInstance(instance, true);
    }
}