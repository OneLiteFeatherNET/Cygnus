package net.onelitefeather.cygnus.setup.map;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.cygnus.setup.util.SetupTags;

import java.util.EnumMap;
import java.util.Map;

public enum MapDataCategory {

    NAME("Name", Material.ACACIA_SIGN, NamedTextColor.YELLOW),
    AUTHOR("Builder", Material.DARK_OAK_DOOR, NamedTextColor.AQUA),
    SPAWN("Spawn", Material.COMPASS, NamedTextColor.RED),
    SLENDER("Slender", Material.ENDER_EYE, NamedTextColor.DARK_PURPLE),
    SURVIVOR("Surivior", Material.CLOCK, NamedTextColor.GREEN),
    PAGE("Page", Material.PAPER, NamedTextColor.WHITE);

    private static final MapDataCategory[] VALUES = values();
    private static final Map<MapDataCategory, ItemStack> DEFAULT_CACHE = new EnumMap<>(MapDataCategory.class);

    private final String name;
    private final Material material;
    private final NamedTextColor color;

    MapDataCategory(String name, Material material, NamedTextColor color) {
        this.name = name;
        this.material = material;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public Material getMaterial() {
        return material;
    }

    public NamedTextColor getColor() {
        return color;
    }

    public static MapDataCategory[] getValues() {
        return VALUES;
    }

    /**
     * Returns a default {@link ItemStack} with lore when no data is given.
     *
     * @param category to get the pre-cached item
     * @return the cached item
     */
    public static ItemStack getDefaultItem(MapDataCategory category) {
        return DEFAULT_CACHE.computeIfAbsent(category, mapDataCategory ->
                ItemStack.builder(mapDataCategory.getMaterial())
                        .customName(Component.text(mapDataCategory.name, mapDataCategory.color))
                        .set(SetupTags.MAP_DATA_CATEGORY_TAG, category)
                        .build());
    }

    public static MapDataCategory byId(int id) {
        if  (id < 0 || id >= VALUES.length) {
            throw new IllegalArgumentException("Invalid map data category ID: " + id);
        }
        return VALUES[id];
    }
}
