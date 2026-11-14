package net.onelitefeather.cygnus.common.page;

import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import net.onelitefeather.vulpes.content.cygnus.CygnusItems;
import net.onelitefeather.vulpes.item.CustomItem;

import java.util.List;
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
     * The page models, generated from the Vulpes content project.
     */
    List<CustomItem> PAGE_ITEMS = List.of(CygnusItems.PAGE_1, CygnusItems.PAGE_2, CygnusItems.PAGE_3,
            CygnusItems.PAGE_4, CygnusItems.PAGE_5, CygnusItems.PAGE_6);

    /**
     * Creates a page item with the given page number.
     * <p>
     * One of the available page models is chosen randomly.
     * </p>
     *
     * @param pageCount the number displayed on the page
     * @return the created page item
     */
    default ItemStack createPageItem(int pageCount) {
        CustomItem page = PAGE_ITEMS.get(ThreadLocalRandom.current().nextInt(PAGE_ITEMS.size()));
        return page.itemStack().withCustomName(Component.text("Page: " + pageCount));
    }
}