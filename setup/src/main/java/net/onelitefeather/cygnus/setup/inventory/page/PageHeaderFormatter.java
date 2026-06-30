package net.onelitefeather.cygnus.setup.inventory.page;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Contract;

/**
 * Small helper class to format the current number of pages and the total ones to a {@link Component}.
 *
 * @author theEvilReaper
 * @since 2.6.0
 * @version 1.0.0
 */
public final class PageHeaderFormatter {

    /**
     * Returns a {@link Component} that contains the current page and the total numbers of pages.
     * @param current page count
     * @param max page count
     * @return the created component
     */
    @Contract(value = "_, _ -> new", pure = true)
    public static Component format(int current, int max) {
        return Component.text("Page " + current + " of " + max);
    }

    private PageHeaderFormatter() {
        // Nothing to do here
    }
}
