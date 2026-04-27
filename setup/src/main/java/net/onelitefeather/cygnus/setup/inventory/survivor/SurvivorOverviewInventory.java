package net.onelitefeather.cygnus.setup.inventory.survivor;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.cygnus.common.map.GameMapBuilder;
import net.onelitefeather.cygnus.setup.event.MapSetupSelectEvent;
import net.onelitefeather.cygnus.setup.event.dialog.DialogContext;
import net.onelitefeather.cygnus.setup.event.dialog.DialogRequestEvent;
import net.onelitefeather.cygnus.setup.event.dialog.DialogTarget;
import net.onelitefeather.cygnus.setup.player.SetupPlayer;
import net.onelitefeather.cygnus.setup.util.SetupMode;
import net.theevilreaper.aves.inventory.InventoryLayout;
import net.theevilreaper.aves.inventory.PersonalInventoryBuilder;
import net.theevilreaper.aves.inventory.click.ClickHolder;
import net.theevilreaper.aves.inventory.util.LayoutCalculator;
import net.theevilreaper.aves.map.MapEntry;
import net.theevilreaper.aves.util.Components;
import org.jetbrains.annotations.Contract;

import java.math.RoundingMode;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

import static net.onelitefeather.cygnus.setup.util.SetupItems.DECORATION;

public class SurvivorOverviewInventory extends PersonalInventoryBuilder {

    private static final int[] SLOT_POSITIONS = LayoutCalculator
            .quad(10, 43);

    private static final DecimalFormat DECIMAL_FORMAT;

    static {
        DECIMAL_FORMAT = new DecimalFormat("#.##");
        DECIMAL_FORMAT.setRoundingMode(RoundingMode.CEILING);
        DECIMAL_FORMAT.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ROOT));
    }

    public SurvivorOverviewInventory(Player player, GameMapBuilder mapBuilder) {
        super(Component.text("Survivor Spawn"), InventoryType.CHEST_5_ROW, player);

        InventoryLayout layout = InventoryLayout.fromType(getType());
        layout.setItems(LayoutCalculator.frame(0, getType().getSize() - 1), DECORATION);
        this.setLayout(layout);

        Set<Pos> survivorSpawns = mapBuilder.getSurvivorSpawns();

        if (survivorSpawns.isEmpty()) return;
        setDataLayoutFunction(dataLayoutFunction -> {
            var dataLayout = dataLayoutFunction == null ? InventoryLayout.fromType(getType()) : dataLayoutFunction;

            dataLayout.blank(SLOT_POSITIONS);
            Iterator<Pos> survivorSpawnIterator = survivorSpawns.iterator();

            for (int i = 0; i < SLOT_POSITIONS.length && survivorSpawnIterator.hasNext(); i++) {
                var currentSpawn = survivorSpawnIterator.next();
                ItemStack spawnItem = getSpawnItem(currentSpawn, i);
                dataLayout.setItem(SLOT_POSITIONS[i], spawnItem, (innerPlayer, slot, clickType, stack, result) ->
                        this.handleClick(currentSpawn, innerPlayer, slot, clickType, stack, result));
            }
            return dataLayout;
        });




        this.invalidateDataLayout();
        this.register();
    }

    /**
     * Handles the click event for the map selection.
     *
     * @param currentPos the current pos being clicked
     * @param player     the player who clicked
     * @param slot       the slot clicked
     * @param clickType  the type of click
     * @param result     the result of the inventory condition
     */
    private void handleClick(Pos currentPos, Player player, int slot, Click clickType, ItemStack stack, Consumer<ClickHolder> result) {
        result.accept(ClickHolder.cancelClick());

        if (clickType instanceof Click.Left) {
            player.teleport(currentPos);
            player.closeInventory();
            return;
        }

        if (clickType instanceof Click.Right) {
            ((SetupPlayer) player).setSurvivorToDelete(currentPos);
            EventDispatcher.call(new DialogRequestEvent(player, DialogTarget.DELETE_SURVIVOR_SPAWN, new DialogContext.PositionContext(currentPos)));
        }
    }

    @Contract(value = "_, _ -> new", pure = true)
    private ItemStack getSpawnItem(Pos position, int index) {
        return ItemStack.builder(Material.COMPASS)
                .lore(getPositionLore(position))
                .customName(Component.text("Spawn " + (index + 1)))
                .build();
    }

    /**
     * Converts the given position argument to a list of components to create textual representation.
     *
     * @param pos to convert
     * @return a list of components
     */
    @Contract(pure = true, value = "_ -> new")
    private List<Component> getPositionLore(Point pos) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.addAll(Components.pointToLore(MiniMessage.miniMessage(), pos, DECIMAL_FORMAT));
        lore.add(Component.empty());
       // lore.add(MouseFont.leftClick(Component.text( " Teleport")));
       // lore.add(MouseFont.rightClick(Component.text(" Delete")));
        return lore;
    }
}
