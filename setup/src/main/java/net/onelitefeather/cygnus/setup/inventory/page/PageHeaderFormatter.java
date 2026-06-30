package net.onelitefeather.cygnus.setup.inventory.page;

import net.kyori.adventure.text.Component;

public final class PageHeaderFormatter {

    public static Component format(int current, int max) {
        return Component.text("Page " + current + " of " + max);
    }

    private PageHeaderFormatter() {

    }
}
