package net.onelitefeather.cygnus.setup.functional;

import de.icevizion.aves.map.BaseMap;
import net.onelitefeather.cygnus.setup.util.SetupMode;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * This functional interface is used to load the map data from a given path.
 * The map data is used to create a new {@link BaseMap} instance.
 * If the map data is not found, it will create a blank map instance.
 */
@FunctionalInterface
public interface MapDataLoader {

    /**
     * Loads the map data from the given path.
     *
     * @param path      the path to the map data
     * @param setupMode the setup mode of the map
     * @return the loaded map data
     */
    @NotNull BaseMap loadMapData(@NotNull Path path, @NotNull SetupMode setupMode);
}
