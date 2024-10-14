package net.onelitefeather.cygnus.setup.inventory;

import de.icevizion.aves.inventory.GlobalInventoryBuilder;
import de.icevizion.aves.inventory.InventoryLayout;
import de.icevizion.aves.inventory.util.LayoutCalculator;
import de.icevizion.aves.util.functional.PlayerConsumer;
import de.icevizion.aves.util.functional.VoidConsumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.inventory.InventoryCloseEvent;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.cygnus.setup.util.SetupItems;
import net.onelitefeather.cygnus.setup.util.SetupTags;
import org.jetbrains.annotations.NotNull;

import static de.icevizion.aves.inventory.util.InventoryConstants.CANCEL_CLICK;

@SuppressWarnings("java:S3252")
public class ConfirmInventory extends GlobalInventoryBuilder {

    private static final int[] DECO_SLOTS;
    private static final int[] CONFIRM_SLOTS;
    private static final int[] CANCEL_SLOTS;

    private static final ItemStack CONFIRM_STACK;
    private static final ItemStack CANCEL_STACK;

    static {
        DECO_SLOTS = LayoutCalculator.quad(0, InventoryType.CHEST_4_ROW.getSize() - 1);
        CONFIRM_SLOTS = LayoutCalculator.from(10, 11, 19, 20);
        CANCEL_SLOTS = LayoutCalculator.from(14, 15, 23, 24);

        CONFIRM_STACK = ItemStack.builder(Material.GREEN_CONCRETE)
                .customName(Component.text("Confirm", NamedTextColor.GREEN))
                .build();

        CANCEL_STACK = ItemStack.builder(Material.RED_CONCRETE)
                .customName(Component.text("Cancel", NamedTextColor.RED))
                .build();
    }

    public ConfirmInventory(@NotNull PlayerConsumer consumer) {
        super(Component.text("Confirm"), InventoryType.CHEST_4_ROW);

        InventoryLayout layout = InventoryLayout.fromType(getType());
        layout.setItems(DECO_SLOTS, SetupItems.DECORATION, CANCEL_CLICK);

        layout.setItems(CONFIRM_SLOTS, CONFIRM_STACK, (player, i, clickType, result) -> {
            player.setTag(SetupTags.DELETE_TAG, true);
            EventDispatcher.call(new InventoryCloseEvent(player.getOpenInventory(), player));
            player.closeInventory();
        });

        layout.setItems(CANCEL_SLOTS, CANCEL_STACK, (player, i, clickType, result) -> player.closeInventory());

        setLayout(layout);

        setCloseFunction(closeEvent -> {
            Player player = closeEvent.getPlayer();
            if (!player.hasTag(SetupTags.DELETE_TAG)) return;
            consumer.accept(player);
            player.removeTag(SetupTags.DELETE_TAG);
        });

        this.invalidateDataLayout();
    }
}
