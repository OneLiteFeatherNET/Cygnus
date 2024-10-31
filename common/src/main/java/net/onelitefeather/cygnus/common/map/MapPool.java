package net.onelitefeather.cygnus.common.map;

import net.minestom.server.MinecraftServer;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The map pool is responsible for managing the available maps. It will load all maps data from the given path and store them.
 * It would not load the map itself over a {@link net.minestom.server.instance.anvil.AnvilLoader} instance.
 * This behavior is handled by another class.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 **/
public class MapPool {

    private static final String LOBBY_MAP_NAME = "lobby";
    private static final Logger LOGGER = LoggerFactory.getLogger(MapPool.class);

    private List<MapEntry> referenceList;
    private MapEntry selectedMap;
    private MapEntry lobbyMap;

    /**
     * Creates a new instance of the map pool. It will load all maps from the given path.
     *
     * @param path the path where the maps are stored
     */
    public MapPool(@NotNull Path path) {
        referenceList = loadMapsEntries(path);
        this.peekMap();
    }

    /**
     * Loads all maps from the given path. It will filter all directories and create a new {@link MapEntry} instance.
     *
     * @param path the path where the maps are stored
     * @return a list with all available maps
     */
    private @NotNull List<MapEntry> loadMapsEntries(@NotNull Path path) {
        List<MapEntry> mapEntries = new ArrayList<>();
        try (Stream<Path> stream = Files.list(path)) {
            mapEntries = stream.filter(Files::isDirectory).map(MapEntry::new).filter(MapEntry::hasMapFile).collect(Collectors.toList());
        } catch (IOException exception) {
            MinecraftServer.getExceptionManager().handleException(exception);
            LOGGER.error("Unable to load maps from path {}", path, exception);
        }
        mapEntries.removeIf(mapEntry -> {
            if (mapEntry.path().getFileName().toString().contains(LOBBY_MAP_NAME)) {
                this.lobbyMap = mapEntry;
                return true;
            }
            return false;
        });

        Check.argCondition(this.lobbyMap == null, "The lobby map can't be null");
        return mapEntries;
    }

    /**
     * Peeks a random map from the list. If the list is empty it will throw an exception.
     */
    public void peekMap() {
        Check.argCondition(this.referenceList.isEmpty(), "The map list is empty");
        if (this.referenceList.size() == 1) {
            this.selectedMap = this.referenceList.getFirst();
            return;
        }
        this.selectedMap = this.referenceList.get(ThreadLocalRandom.current().nextInt(referenceList.size()));
    }

    /**
     * Removes the selected map from the list. If the list is empty it will throw an exception.
     */
    public void clear() {
        this.referenceList.clear();
        this.referenceList = null;
    }

    /**
     * Gets the selected map entry.
     *
     * @return the selected map entry
     */
    public @Nullable MapEntry getMapEntry() {
        return this.selectedMap;
    }

    /**
     * Gets the lobby map entry.
     *
     * @return the lobby map entry
     */
    public @NotNull MapEntry getLobbyEntry() {
        return this.lobbyMap;
    }

    /**
     * Gets all available maps from the pool.
     *
     * @return an unmodifiable list with all available maps
     */
    public @NotNull @UnmodifiableView List<MapEntry> getAvailableMaps() {
        return Collections.unmodifiableList(this.referenceList);
    }
}
