package net.onelitefeather.cygnus.setup.inventory.view;

import net.onelitefeather.cygnus.setup.map.MapDataCategory;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;


/**
 * Defines the available inventory modes for the {@link MapDataOverviewInventory}.
 * Each mode specifies which {@link MapDataCategory} entries are displayed and at which slot positions.
 *
 * <p>The slot indices and categories are aligned by position — the first category maps to the first slot index, and so on.</p>
 *
 * <ul>
 *   <li>{@link #LOBBY} — displays name, author, and spawn; used for lobby map setup.</li>
 *   <li>{@link #GAME} — additionally displays the slender category; used for game map setup.</li>
 * </ul>
 *
 * @author Joltra
 * @version 1.0.0
 * @since 2.5.0
 */
public enum InventoryMode {
    LOBBY(new int[]{11, 13, 15}, MapDataCategory.NAME, MapDataCategory.AUTHOR, MapDataCategory.SPAWN),
    GAME(new int[]{10, 12, 14, 16}, MapDataCategory.NAME, MapDataCategory.AUTHOR, MapDataCategory.SPAWN, MapDataCategory.SLENDER);

    private final EnumSet<MapDataCategory> categories;
    private final int[] slots;

    /**
     * Creates a new entry for this enumeration
     *
     * @param slots      of the entry
     * @param categories of the entry
     */
    InventoryMode(int[] slots, MapDataCategory... categories) {
        this.categories = EnumSet.copyOf(Arrays.asList(categories));
        this.slots = slots;
    }

    /**
     * Returns the range of slots
     *
     * @return an array of slot indices corresponding to the categories in this mode
     */
    public int[] getSlots() {
        return slots;
    }

    /**
     * Returns the categories from the entry.
     *
     * @return a set of {@link MapDataCategory} values
     */
    public Set<MapDataCategory> getCategories() {
        return categories;
    }
}