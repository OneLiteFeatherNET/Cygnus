package net.onelitefeather.cygnus.setup.inventory;

import de.icevizion.aves.inventory.InventoryLayout;
import de.icevizion.aves.inventory.InventorySlot;
import de.icevizion.aves.inventory.pageable.PageableInventory;
import de.icevizion.aves.inventory.slot.ISlot;
import de.icevizion.aves.inventory.util.LayoutCalculator;
import de.icevizion.aves.util.Components;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.common.page.PageResource;
import net.onelitefeather.cygnus.setup.util.SetupItems;
import net.onelitefeather.cygnus.setup.util.SetupMessages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.icevizion.aves.inventory.util.InventoryConstants.CANCEL_CLICK;
import static net.onelitefeather.cygnus.setup.util.FormatHelper.DECIMAL_FORMAT;

public class DataViewInventory {

    private static final int[] BLOCKED_SLOTS = LayoutCalculator.fillRow(InventoryType.CHEST_5_ROW);
    private static final int[] DATA_SLOTS = LayoutCalculator.quad(0, InventoryType.CHEST_4_ROW.getSize() - 1);
    private final Map<Long, ISlot> dataCache;
    private final PageableInventory pageableInventory;

    public DataViewInventory(@NotNull GameMap gameMap, @NotNull Player player) {
        this.dataCache = getSlots(gameMap);
        InventoryLayout layout = InventoryLayout.fromType(InventoryType.CHEST_6_ROW);
        layout.setItems(BLOCKED_SLOTS, SetupItems.DECORATION, CANCEL_CLICK);
        this.pageableInventory = PageableInventory
                .builder()
                .title(Component.text("Data view"))
                .type(InventoryType.CHEST_6_ROW)
                .layout(layout)
                .player(player)
                .slotRange(DATA_SLOTS)
                .values(new ArrayList<>(dataCache.values()))
                .build();
    }


    public void openInventory() {
        pageableInventory.open();
    }

    public void clear() {
        this.pageableInventory.unregister();
    }

    private @NotNull Map<Long, ISlot> getSlots(@NotNull GameMap gameMap) {
        Map<Long, ISlot> slotMap = new HashMap<>();

        if (gameMap.getName() != null) {
            long hashCode = gameMap.getName().hashCode();
            slotMap.put(hashCode, getSlot(Pos.ZERO, Material.NAME_TAG, Component.text("Name")));
        }

        if (gameMap.getBuilders() != null && gameMap.getBuilders().length != 0) {
            long hashCode = gameMap.getBuilders().hashCode();
            List<Component> builders = new ArrayList<>();
            builders.add(Component.empty());
            for (int i = 0; i < gameMap.getBuilders().length; i++) {
                String builder = gameMap.getBuilders()[i];
                if (builder == null) continue;
                Component component = Component.text("-", NamedTextColor.GRAY)
                        .append(Component.space())
                        .append(Component.text(builder, NamedTextColor.GOLD));
                builders.add(component);
            }
            builders.add(Component.empty());
            ItemStack stack = ItemStack.builder(Material.DARK_OAK_SIGN)
                    .customName(Component.text("Builders", NamedTextColor.GOLD))
                    .lore(builders)
                    .build();
            InventorySlot slot = new InventorySlot(stack);
            slotMap.put(hashCode, slot);
        }

        if (gameMap.hasSpawn()) {
            long hashCode = gameMap.getSpawn().hashCode();
            slotMap.put(hashCode, getSlot(gameMap.getSpawn(), Material.GREEN_BED, Component.text("Spawn")));
        }

        if (gameMap.getSlenderSpawn() != null) {
            long hashCode = gameMap.getSlenderSpawn().hashCode();
            slotMap.put(hashCode, getSlot(gameMap.getSlenderSpawn(), Material.ENDERMAN_SPAWN_EGG, Component.text("Slender Spawn")));
        }

        if (!gameMap.getSurvivorSpawns().isEmpty()) {
            for (Pos survivorSpawn : gameMap.getSurvivorSpawns()) {
                long hashCode = survivorSpawn.hashCode();
                slotMap.put(hashCode, getSlot(survivorSpawn, Material.GREEN_DYE, Component.text("Survivor Spawn")));
            }
        }

        if (!gameMap.getPageFaces().isEmpty()) {
            for (PageResource pageFace : gameMap.getPageFaces()) {
                long hashCode = pageFace.hashCode();
                slotMap.put(hashCode, getSlot(pageFace.position(), Material.PAPER, Component.text("Page Face")));
            }
        }

        return slotMap;
    }

    private @NotNull InventorySlot getSlot(@NotNull Point pos, @NotNull Material material, @NotNull Component displayName) {
        List<Component> lore = pos == Pos.ZERO ? null : getPosLore(pos);
        ItemStack itemStack = getSlotIcon(material, displayName, lore);
        return new InventorySlot(itemStack);
    }

    private @NotNull ItemStack getSlotIcon(@NotNull Material material, @NotNull Component component, @Nullable List<Component> lore) {
        ItemStack.Builder builder = ItemStack.builder(material).customName(component);
        if (lore != null && !lore.isEmpty()) {
            builder.lore(lore);
        }
        return builder.build();
    }

    public static @NotNull List<Component> getPosLore(@NotNull Point point) {
        List<Component> components = Components.pointToLore(MiniMessage.miniMessage(), point, DECIMAL_FORMAT);
        List<Component> loreList = new ArrayList<>();
        loreList.add(Component.empty());
        loreList.addAll(components);
        loreList.add(Component.empty());
        loreList.addAll(SetupMessages.ACTION_LORE);
        return loreList;
    }
}

