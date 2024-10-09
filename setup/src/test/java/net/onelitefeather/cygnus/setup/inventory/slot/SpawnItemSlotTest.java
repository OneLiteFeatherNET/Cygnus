package net.onelitefeather.cygnus.setup.inventory.slot;

import de.icevizion.aves.inventory.slot.ISlot;
import de.icevizion.aves.inventory.util.InventoryConstants;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.inventory.condition.InventoryConditionResult;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MicrotusExtension.class)
class SpawnItemSlotTest {

    @Test
    void testSpawnItemSlot() {
        ISlot spawnItemSlot = new SpawnItemSlot(Pos.ZERO);
        assertNotNull(spawnItemSlot);
        ItemStack slotItem = spawnItemSlot.getItem();
        assertNotNull(slotItem);
        assertEquals(Material.GREEN_BED, slotItem.material());
        assertTrue(slotItem.has(ItemComponent.CUSTOM_NAME));
        assertTrue(slotItem.has(ItemComponent.LORE));
        assertNotEquals(InventoryConstants.CANCEL_CLICK, spawnItemSlot.getClick());
    }

    @Test
    void testSpawnTeleportation(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        player.setInstance(instance).join();
        assertEquals(Pos.ZERO, player.getPosition());
        Pos spawnPos = new Pos(10, 10, 10);
        ISlot spawnItemSlot = new SpawnItemSlot(spawnPos);
        assertNotNull(spawnItemSlot);

        assertNotNull(spawnItemSlot.getClick());
        InventoryConditionResult result = new InventoryConditionResult(spawnItemSlot.getItem(), spawnItemSlot.getItem());
        spawnItemSlot.getClick().onClick(player, 10, ClickType.LEFT_CLICK, result);
        assertEquals(spawnPos, player.getPosition());
        env.destroyInstance(instance, true);
    }
}
