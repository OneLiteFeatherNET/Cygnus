package net.onelitefeather.cygnus.setup.inventory.slot;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.cygnus.common.creek.CreekRoute;
import net.onelitefeather.cygnus.common.creek.CreekWaypoint;
import net.theevilreaper.aves.inventory.click.ClickHolder;
import net.theevilreaper.aves.inventory.slot.Slot;

import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

import static net.onelitefeather.cygnus.setup.util.SetupMessages.DELETE_CLICK;
import static net.onelitefeather.cygnus.setup.util.SetupMessages.SELECT_CLICK;

/**
 * One creek route in the route overview. Left click selects it, right click deletes it.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class CreekRouteSlot extends Slot {

    private final CreekRoute route;
    private final boolean active;

    /**
     * Creates the slot.
     *
     * @param route  the route to show
     * @param active whether the route is being edited
     * @param select called with the clicking player and the name on a left click
     * @param delete called with the clicking player and the name on a right click
     */
    public CreekRouteSlot(CreekRoute route, boolean active, BiConsumer<Player, String> select,
                          BiConsumer<Player, String> delete) {
        this.route = route;
        this.active = active;
        String name = route.name();
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
        List<CreekWaypoint> points = this.route.points();
        int startPause = points.isEmpty() ? 0 : points.getFirst().pauseMillis();
        int endPause = points.size() < CreekRoute.MIN_POINTS ? 0 : points.getLast().pauseMillis();
        return ItemStack.builder(Material.LEAD)
                .customName(Component.text(this.route.name(), this.active ? NamedTextColor.GREEN : NamedTextColor.YELLOW))
                .lore(List.of(
                        Component.empty(),
                        Component.text("Points: ", NamedTextColor.GRAY)
                                .append(Component.text(points.size(), NamedTextColor.GOLD)),
                        Component.text("Pause: start ", NamedTextColor.GRAY)
                                .append(Component.text(seconds(startPause), NamedTextColor.GOLD))
                                .append(Component.text(" · end ", NamedTextColor.GRAY))
                                .append(Component.text(seconds(endPause), NamedTextColor.GOLD)),
                        Component.empty(),
                        SELECT_CLICK,
                        DELETE_CLICK,
                        Component.empty()))
                .build();
    }

    private static String seconds(int millis) {
        return String.format(Locale.ROOT, "%.1f s", millis / 1000.0D);
    }
}
