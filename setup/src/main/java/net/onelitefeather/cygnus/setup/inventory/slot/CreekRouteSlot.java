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

/**
 * One creek route in the route overview. Left click selects it, shift click deletes it.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class CreekRouteSlot extends Slot {

    private final String name;
    private final int points;
    private final boolean active;

    /**
     * Creates the slot.
     *
     * @param name   the route's name
     * @param points how many points the route has
     * @param active whether the route is being edited
     * @param select called with the clicking player and the name on a left click
     * @param delete called with the clicking player and the name on a shift click
     */
    public CreekRouteSlot(String name, int points, boolean active, BiConsumer<Player, String> select,
                          BiConsumer<Player, String> delete) {
        this.name = name;
        this.points = points;
        this.active = active;
        this.setClick((player, _, click, _, result) -> {
            result.accept(ClickHolder.cancelClick());
            switch (click) {
                case Click.Left _ -> {
                    player.closeInventory();
                    select.accept(player, name);
                }
                case Click.LeftShift _ -> delete.accept(player, name);
                default -> {
                    // Nothing to do here
                }
            }
        });
    }

    @Override
    public ItemStack getItem() {
        return ItemStack.builder(Material.LEAD)
                .customName(Component.text(this.name, this.active ? NamedTextColor.GREEN : NamedTextColor.YELLOW))
                .lore(List.of(
                        Component.empty(),
                        Component.text("Points: " + this.points, NamedTextColor.GRAY),
                        Component.empty(),
                        Component.text("Left click: edit this route", NamedTextColor.WHITE),
                        Component.text("Shift click: delete this route", NamedTextColor.RED)))
                .build();
    }
}
