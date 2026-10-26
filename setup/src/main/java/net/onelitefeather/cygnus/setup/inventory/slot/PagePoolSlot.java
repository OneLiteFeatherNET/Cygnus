package net.onelitefeather.cygnus.setup.inventory.slot;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.theevilreaper.aves.inventory.click.ClickHolder;
import net.theevilreaper.aves.inventory.slot.Slot;

import java.util.List;
import java.util.function.BiConsumer;

import static net.onelitefeather.cygnus.setup.util.SetupMessages.DELETE_CLICK;
import static net.onelitefeather.cygnus.setup.util.SetupMessages.SELECT_CLICK;

/**
 * One page pool in the pool overview. Left click makes it the active pool, right click deletes it.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.21.0
 */
public final class PagePoolSlot extends Slot {

    private final String name;
    private final int pageCount;
    private final boolean active;

    /**
     * Creates the slot.
     *
     * @param name      the pool's name
     * @param pageCount how many pages the pool has
     * @param active    whether new pages go into this pool
     * @param select    called with the clicking player and the name on a left click
     * @param delete    called with the clicking player and the name on a right click
     */
    public PagePoolSlot(String name, int pageCount, boolean active, BiConsumer<Player, String> select,
                        BiConsumer<Player, String> delete) {
        this.name = name;
        this.pageCount = pageCount;
        this.active = active;
        this.setClick((player, _, click, _, result) -> {
            result.accept(ClickHolder.cancelClick());
            switch (click) {
                case Click.Left _ -> {
                    player.closeInventory();
                    select.accept(player, name);
                }
                case Click.Right _ -> delete.accept(player, name);
                default -> {
                    // Nothing to do here
                }
            }
        });
    }

    @Override
    public ItemStack getItem() {
        return ItemStack.builder(Material.PAPER)
                .customName(Component.text(this.name, this.active ? NamedTextColor.GREEN : NamedTextColor.YELLOW))
                .lore(List.of(
                        Component.empty(),
                        Component.text("Pages: ", NamedTextColor.GRAY)
                                .append(Component.text(this.pageCount, NamedTextColor.GOLD)),
                        Component.empty(),
                        SELECT_CLICK,
                        DELETE_CLICK,
                        Component.empty()))
                .build();
    }
}
