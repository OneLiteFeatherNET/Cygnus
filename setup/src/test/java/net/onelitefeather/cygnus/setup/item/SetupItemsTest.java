package net.onelitefeather.cygnus.setup.item;

import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.common.Tags;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MicrotusExtension.class)
class SetupItemsTest {

    @Test
    void testMapSelectionItemSet(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        SetupItems.setMapSelection(player);

        assertItem(player, SetupItems.FOURTH_INDEX, (byte) 0x00);

        env.destroyInstance(instance, true);
    }

    @Test
    void testSaveDataItemSet(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        SetupItems.setSaveData(player);
        assertItem(player, 2, (byte) 0x02);
        assertItem(player, 6, (byte) 0x01);

        env.destroyInstance(instance, true);
    }

    @Test
    void testPageItemSet(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        SetupItems.setPageItems(player);
        assertItem(player, 2, (byte) 0x03);
        assertItem(player, 6, (byte) 0x01);

        env.destroyInstance(instance, true);
    }

    /**
     * Asserts that the item in the specified slot of the player's inventory matches the expected item ID.
     *
     * @param player the player whose inventory is being checked
     * @param slot   the inventory slot to check
     * @param itemId the expected id from the item
     */
    private void assertItem(@NotNull Player player, int slot, byte itemId) {
        ItemStack itemStack = player.getInventory().getItemStack(slot);
        assertNotNull(itemStack);
        assertNotEquals(Material.AIR, itemStack.material());
        assertEquals(itemId, itemStack.getTag(Tags.ITEM_TAG).byteValue());
    }
}
