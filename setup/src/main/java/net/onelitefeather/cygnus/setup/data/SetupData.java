package net.onelitefeather.cygnus.setup.data;

import de.icevizion.aves.map.BaseMap;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.common.map.MapEntry;
import net.onelitefeather.cygnus.setup.functional.MapDataLoader;
import net.onelitefeather.cygnus.setup.util.SetupMode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The {@link SetupData} interface defines an object which contains all relevant values about a setup instance.
 * It's a data layer for the setup data and provides some methods to interact with the setup instance.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @see SetupDataImpl
 * @since 1.0.0
 */
public sealed interface SetupData permits SetupDataImpl {

    /**
     * Creates a new instance of the {@link SetupData} interface.
     *
     * @return a new instance of the {@link SetupData} interface
     */
    @Contract(pure = true)
    static @NotNull Builder builder() {
        return new SetupDataBuilder();
    }

    /**
     * Creates a new instance of the {@link SetupData} interface.
     *
     * @param setupData the setup data to create a new instance
     * @return a new instance of the {@link SetupData} interface
     */
    @Contract(pure = true)
    static @NotNull Builder builder(@NotNull SetupData setupData) {
        return new SetupDataBuilder(setupData);
    }

    void triggerInventoryUpdate();

    /**
     * Opens the inventory of the setup data.
     */
    void openInventory();

    /**
     * Updates the title of the {@link net.kyori.adventure.bossbar.BossBar}.
     */
    void updateTitle();

    /**
     * Triggers some code to reset the setup data.
     */
    void reset();

    /**
     * Teleports the player to the setup instance.
     * If the map has a spawn point it will be used otherwise the default spawn point will be used.
     */
    void teleport();

    /**
     * Swaps the page mode of the setup data.
     */
    void swapPageMode();

    /**
     * Returns if the setup data has a page mode.
     *
     * @return {@code true} if the setup data has a page mode
     */
    boolean hasPageMode();

    /**
     * Returns if the setup data has a map.
     *
     * @return {@code true} if the setup data has a map
     */
    boolean hasMap();

    /**
     * Loads the map data into the setup data.
     *
     * @return {@code true} if the map was loaded successfully
     */
    boolean loadMap();

    /**
     * Returns the map entry of the setup data.
     *
     * @return the given map entry
     */
    @NotNull MapEntry getMapEntry();

    /**
     * Returns the setup mode of the setup data.
     *
     * @return the given mode
     */
    @NotNull SetupMode getSetupMode();

    /**
     * Returns the player of the setup data.
     *
     * @return the given player
     */
    @NotNull Player getPlayer();

    /**
     * Returns the base map of the setup data.
     *
     * @return the given base map
     */
    @NotNull BaseMap getBaseMap();

    /**
     * The builder interface for the {@link SetupData} interface.
     *
     * @author theEvilReaper
     * @version 1.0.0
     * @see SetupDataBuilder
     * @since 1.0.0
     */
    sealed interface Builder permits SetupDataBuilder {

        /**
         * Sets the map entry for the setup data.
         *
         * @param mapEntry the map entry for the setup data
         * @return the builder instance
         */
        @NotNull Builder mapEntry(@NotNull MapEntry mapEntry);

        /**
         * Sets the mode for the setup data.
         *
         * @param mode the mode for the setup data
         * @return the builder instance
         */
        @NotNull Builder mode(@NotNull SetupMode mode);

        /**
         * Sets the player for the setup data.
         *
         * @param player the player for the setup data
         * @return the builder instance
         */
        @NotNull Builder player(@NotNull Player player);

        /**
         * Sets the base map for the setup data.
         *
         * @param baseMap the base map for the setup data
         * @return the builder instance
         */
        @NotNull Builder baseMap(@NotNull BaseMap baseMap);

        /**
         * Builds the setup data.
         *
         * @return the created setup data
         */
        @NotNull SetupData build();
    }
}
