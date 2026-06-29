package net.onelitefeather.cygnus.setup.map;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.onelitefeather.cygnus.setup.util.SetupMessages;
import net.onelitefeather.cygnus.setup.util.SetupTags;

import java.util.EnumMap;
import java.util.Map;

/**
 * Enumeration of each possible map data which is required for the setup.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.5.0
 */
public enum MapDataCategory {

    NAME("Name", Material.ACACIA_SIGN, NamedTextColor.YELLOW),
    AUTHOR("Builder", Material.DARK_OAK_DOOR, NamedTextColor.AQUA),
    SPAWN("Spawn", Material.COMPASS, NamedTextColor.RED),
    SLENDER("Slender", Material.ENDER_EYE, NamedTextColor.DARK_PURPLE),
    SURVIVOR("Survivor", Material.CLOCK, NamedTextColor.GREEN),
    PAGE("Page", Material.PAPER, NamedTextColor.WHITE);

    private static final MapDataCategory[] VALUES = values();
    private static final Map<MapDataCategory, ItemStack> DEFAULT_CACHE = new EnumMap<>(MapDataCategory.class);

    private final String name;
    private final Material material;
    private final NamedTextColor color;

    /**
     * Creates a new instance of the enumeration with the given values.
     *
     * @param name     of the entry
     * @param material of the entry
     * @param color    of the entry
     */
    MapDataCategory(String name, Material material, NamedTextColor color) {
        this.name = name;
        this.material = material;
        this.color = color;
    }

    /**
     * Returns the custom name of the enumeration entry.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the given material of the entry.
     *
     * @return the material
     */
    public Material getMaterial() {
        return material;
    }

    /**
     * Returns the given color value of the entry.
     *
     * @return the color
     */
    public NamedTextColor getColor() {
        return color;
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
                        .lore(SetupMessages.getLore(SetupMessages.SPACE_SEPARATOR
                                        .append(Component.text("Click to add new data", NamedTextColor.WHITE))
                                ))
                                .customName(Component.text(mapDataCategory.name, mapDataCategory.color))
                                .set(SetupTags.MAP_DATA_CATEGORY_TAG, category)
                                .build());
    }

    /**
     * Try to parse a given id to a {@link MapDataCategory}.
     *
     * @param id to get the entry
     * @return the corresponding enum entry
     * @throws IllegalArgumentException when the provided id is valid
     */
    public static MapDataCategory byId(int id) {
        if (id < 0 || id >= VALUES.length) {
            throw new IllegalArgumentException("Invalid map data category ID: " + id);
        }
        return VALUES[id];
    }
}
