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
 * The available models range from {@code page_0} up to {@code page_(MAX_CUSTOM_PAGE_ID - 1)}.
 * If custom pages are disabled, a regular paper item is returned instead.
 *
 * @author theEvilReaper
 * @version 1.0.0
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
     *
     * @param pageCount the number displayed on the page
     * @return the created page item
     */
    default ItemStack createPageItem(boolean customPage, int pageCount) {
        ItemStack.Builder builder = ItemStack.builder(Material.PAPER)
                .customName(Component.text("Page: " + pageCount));

        if (!customPage) {
            return builder.build();
        }

        int randomPage = ThreadLocalRandom.current().nextInt(MAX_CUSTOM_PAGE_ID);

        return builder
                .set(DataComponents.ITEM_MODEL, Key.key("cygnus", "page_" + randomPage).asString())
                .build();
    }
}