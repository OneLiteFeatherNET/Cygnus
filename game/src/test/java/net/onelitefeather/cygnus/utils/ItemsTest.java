package net.onelitefeather.cygnus.utils;

import net.minestom.server.component.DataComponents;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MicrotusExtension.class)
class ItemsTest {

    @Test
    void testSlenderItemsSet(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        assertNotNull(player);

        Items.setSlenderItems(player);
        ItemStack eye = player.getInventory().getItemStack(0x00);
        assertNotNull(eye);
        assertNotEquals(Material.AIR, eye.material());
        assertEquals(Material.ENDER_EYE, eye.material());
        assertEquals(0x00, eye.getTag(Tags.ITEM_TAG).byteValue());

        ItemStack tracker = player.getInventory().getItemStack(Items.TRACKING_SLOT);
        assertEquals(Items.TRACKING_MATERIAL, tracker.material());
        assertEquals(Items.TRACKING_ITEM, tracker.getTag(Tags.ITEM_TAG).byteValue());
        assertFalse(eye.get(DataComponents.LORE, List.of()).isEmpty());
        assertFalse(tracker.get(DataComponents.LORE, List.of()).isEmpty());

        env.destroyInstance(instance, true);
    }

    @Test
    void testPossessionItemSet(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        Items.setPossessionItem(player);
        ItemStack possession = player.getInventory().getItemStack(Items.POSSESSION_SLOT);
        assertEquals(Items.POSSESSION_MATERIAL, possession.material());
        assertEquals(Items.POSSESSION_ITEM, possession.getTag(Tags.ITEM_TAG).byteValue());
        assertFalse(possession.get(DataComponents.LORE, List.of()).isEmpty());

        env.destroyInstance(instance, true);
    }

    @Test
    void testTutorialItemSitsInItsSlot(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        Items.setTutorialItem(player);
        ItemStack tutorial = player.getInventory().getItemStack(Items.TUTORIAL_SLOT);
        assertTrue(Items.isTutorialItem(tutorial), "the tutorial item belongs in its own slot");
        assertEquals(Material.KNOWLEDGE_BOOK, tutorial.material());
        assertFalse(tutorial.get(DataComponents.LORE, List.of()).isEmpty());

        env.destroyInstance(instance, true);
    }

    @Test
    void testRemoveTutorialItemLeavesTheOthers(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        Items.setTutorialItem(player);
        Items.setStatsItem(player);

        Items.removeTutorialItem(player);

        assertFalse(Items.hasTutorialItem(player), "the tutorial item must be gone");
        assertTrue(Items.hasStatsItem(player), "the statistics item is not the tutorial's to take");

        env.destroyInstance(instance, true);
    }

    @Test
    void testRoleItemGettersReturnTheRoundItems(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = env.createPlayer(instance);
        Player survivor = env.createPlayer(instance);

        Items.setSlenderItems(slender);
        Items.setPossessionItem(slender);
        Items.setHealItem(survivor);
        Items.setPageRevealItem(survivor);

        assertEquals(Items.slenderEye(), slender.getInventory().getItemStack(0));
        assertEquals(Items.tracker(), slender.getInventory().getItemStack(Items.TRACKING_SLOT));
        assertEquals(Items.possession(), slender.getInventory().getItemStack(Items.POSSESSION_SLOT));
        assertEquals(Items.painkillers(), survivor.getInventory().getItemStack(Items.HEAL_SLOT));
        assertEquals(Items.pageReveal(), survivor.getInventory().getItemStack(Items.PAGE_REVEAL_SLOT));

        env.destroyInstance(instance, true);
    }
}
