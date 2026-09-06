package net.onelitefeather.cygnus.common.page;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Creates page items for the game.
 * <p>
 * When custom pages are enabled, a random item model is assigned to the page.
 * Available models are {@code page_1}, {@code page_2}, {@code page_4}, {@code page_5}, and {@code page_6}.
 * If custom pages are disabled, a regular paper item is returned instead.
 *
 * @author theEvilReaper
 * @version 1.1.0
 * @since 2.6.6
 */
public interface PageCreator {

    /**
     * Number of available custom page models.
     */
    int MAX_CUSTOM_PAGE_ID = 6;

    /**
     * Creates a page item with the given page number.
     * <p>
     * If custom pages are enabled, one of the available page models is chosen
     * randomly. Otherwise, the returned item only contains its display name.
     * </p>
     * <p>
     * Every page carries an enchantment glint override. The glint is what makes a page readable as
     * an item at all in the unlit parts of a map - without it a page blends into the wall it hangs
     * on. The override is used instead of a real enchantment so the item stays free of an
     * enchantment tooltip once a player picks it up.
     * </p>
     *
     * @param pageCount the number displayed on the page
     * @return the created page item
     */
    default ItemStack createPageItem(int pageCount) {
        ItemStack.Builder builder = ItemStack.builder(Material.PAPER)
                .customName(Component.text("Page: " + pageCount));

        int randomPage = ThreadLocalRandom.current().nextInt(1, MAX_CUSTOM_PAGE_ID + 1);

        return builder
                .set(DataComponents.ITEM_MODEL, Key.key("cygnus", "page_" + randomPage).asString())
                .set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
                .build();
    }
}