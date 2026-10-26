package net.onelitefeather.cygnus.setup.item;

import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.common.Tags;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@ExtendWith(MicrotusExtension.class)
class SetupItemsTest {

    private Instance instance;
    private Player player;

    @BeforeEach
    void setUp(Env env) {
        this.instance = env.createFlatInstance();
        this.player = env.createPlayer(this.instance);
    }

    @AfterEach
    void tearDown(Env env) {
        env.destroyInstance(this.instance, true);
    }

    @Test
    void testMapSelectionItemSet() {
        SetupItems.setMapSelection(player);
        assertItem(SetupItems.SELECTION, SetupItemId.MAP_SELECTION);
    }

    @Test
    void testLobbyLayoutItemSet() {
        SetupItems.setLobbyLayout(player);
        assertItem(2, SetupItemId.DATA);
        assertItem(6, SetupItemId.SAVE_DATA);
    }

    @Test
    void testPageItemSet() {
        SetupItems.setPageItems(player);
        assertItem(0, SetupItemId.PAGE_POOL_NEW);
        assertItem(4, SetupItemId.PAGE_POOLS);
        assertItem(8, SetupItemId.LEAVE_PAGE);
    }

    @Test
    void testPagePoolEditItemSet() {
        SetupItems.setPagePoolEditItems(player);
        assertItem(0, SetupItemId.PAGES);
        assertItem(6, SetupItemId.PAGE_POOL_FINISH);
        assertItem(8, SetupItemId.LEAVE_PAGE);
    }

    @Test
    void testCreekRouteOverviewItemSet() {
        SetupItems.setCreekRouteOverviewItems(player);
        assertItem(0, SetupItemId.CREEK_NEW);
        assertItem(4, SetupItemId.CREEK_LIST);
        assertItem(8, SetupItemId.CREEK_LEAVE);
    }

    @Test
    void testCreekRouteEditItemSet() {
        SetupItems.setCreekRouteEditItems(player);
        assertItem(0, SetupItemId.CREEK_UNDO);
        assertItem(2, SetupItemId.CREEK_PAUSE);
        assertItem(6, SetupItemId.CREEK_FINISH);
        assertItem(8, SetupItemId.CREEK_LEAVE);
    }

    /**
     * Asserts that the item in the specified slot of the player's inventory carries the expected item id.
     *
     * @param slot   the inventory slot to check
     * @param itemId the expected id from the item
     */
    private void assertItem(int slot, byte itemId) {
        ItemStack itemStack = player.getInventory().getItemStack(slot);
        assertNotEquals(Material.AIR, itemStack.material());
        assertEquals(itemId, itemStack.getTag(Tags.ITEM_TAG).byteValue());
    }
}
