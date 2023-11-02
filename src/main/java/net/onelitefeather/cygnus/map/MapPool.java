package net.onelitefeather.cygnus.map;

import net.minestom.server.MinecraftServer;
import net.minestom.server.utils.validate.Check;
import net.onelitefeather.cygnus.setup.MapEntry;
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

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since
 **/

public class MapPool {

    private static final String LOBBY_MAP_NAME = "lobby";
    private static final Logger LOGGER = LoggerFactory.getLogger(MapPool.class);

    private List<MapEntry> referenceList;
    private MapEntry selectedMap;
    private MapEntry lobbyMap;

    public MapPool(@NotNull Path path, boolean debug) {
        referenceList = loadMapsEntries(path, debug);
        this.peekMap();
    }

    private @NotNull List<MapEntry> loadMapsEntries(@NotNull Path path, boolean debug) {
        List<MapEntry> mapEntries = new ArrayList<>();
        try {
            mapEntries = Files.list(path).filter(Files::isDirectory).map(MapEntry::new).collect(Collectors.toList());
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

    public void peekMap() {
        Check.argCondition(this.referenceList.isEmpty(), "The map list is empty");
        if (this.referenceList.size() == 1) {
            this.selectedMap = this.referenceList.get(0);
            return;
        }
        this.selectedMap = this.referenceList.get(ThreadLocalRandom.current().nextInt(referenceList.size()));
    }

    public void clear() {
        this.referenceList.clear();
        this.referenceList = null;
    }

    public @Nullable MapEntry getMapEntry() {
        return this.selectedMap;
    }

    public @NotNull MapEntry getLobbyEntry() {
        return this.lobbyMap;
    }

    public @NotNull @UnmodifiableView List<MapEntry> getAvailableMaps() {
        return Collections.unmodifiableList(this.referenceList);
    }
}
