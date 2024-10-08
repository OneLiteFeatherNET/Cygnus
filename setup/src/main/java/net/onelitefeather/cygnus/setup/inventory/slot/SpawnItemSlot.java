package net.onelitefeather.cygnus.setup.inventory.slot;

import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Point;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("java:S3252")
public final class SpawnItemSlot extends PositionBaseSlot {

    private static final Component SPAWN_NAME = Component.text("Spawn");
    private final ItemStack stack;

    public SpawnItemSlot(@NotNull Point point) {
        super(point);
        this.stack = ItemStack.builder(Material.GREEN_BED)
                .customName(SPAWN_NAME)
                .lore(getPosLore(this.point))
                .build();
        setClick(this::handleClick);
    }

    @Override
    public ItemStack getItem() {
        return this.stack;
    }
}
