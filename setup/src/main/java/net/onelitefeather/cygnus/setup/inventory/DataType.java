package net.onelitefeather.cygnus.setup.inventory;

import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("java:S3252")
public enum DataType {

    SPAWN(Material.GREEN_BED),
    SLENDER(Material.ENDERMAN_SPAWN_EGG),
    SURVIVOR(Material.GREEN_DYE),
    PAGE(Material.PAPER);

    private final Material material;

    DataType(@NotNull Material material) {
        this.material = material;
    }

    public @NotNull Material getMaterial() {
        return material;
    }
}

