package net.onelitefeather.cygnus.map;

import net.minestom.server.MinecraftServer;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.instance.InstanceContainer;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.common.map.filter.MapFilters;
import net.onelitefeather.cygnus.common.util.GsonHelper;
import net.onelitefeather.cygnus.common.util.Helper;
import net.onelitefeather.cygnus.map.event.GameMapLoadedEvent;
import net.theevilreaper.aves.map.BaseMap;
import net.theevilreaper.aves.map.MapEntry;
import net.theevilreaper.aves.map.provider.AbstractMapProvider;

import java.nio.file.Path;

public final class GameMapProvider extends AbstractMapProvider {

    private InstanceContainer gameInstance;
    private GameMap gameMap;

    public GameMapProvider(Path path) {
        super(GsonHelper.FILE_HANDLER, MapFilters::filterMapsForGame);
        this.mapEntries = this.loadMapEntries(path.resolve("maps"));

        if (this.mapEntries.isEmpty()) {
            throw new IllegalStateException("No maps found in the given path");
        }

        this.loadLobbyMap();
    }

    public void loadGameMap() {
        if (this.gameMap != null) return; // idempotent

        MapEntry gameEntry = this.mapEntries.stream()
                .filter(e -> !e.getDirectoryRoot().toString().equalsIgnoreCase("lobby"))
                .findAny()
                .orElseThrow(() -> new IllegalStateException("No game map found"));

        this.gameMap = this.fileHandler.load(gameEntry.getMapFile(), GameMap.class).get();
        this.gameInstance = MinecraftServer.getInstanceManager().createInstanceContainer();
        this.gameInstance.setTime(Helper.NEW_MOON_TIME);
        this.registerInstance(this.gameInstance, gameEntry);

        EventDispatcher.call(new GameMapLoadedEvent(this.gameMap, this.gameInstance));
    }

    public void switchToGameMap() {
        if (this.activeInstance != null) {
            MinecraftServer.getInstanceManager().unregisterInstance(this.activeInstance);
            this.activeInstance = null;
            this.activeMap = null;
        }
        this.activeInstance = this.gameInstance;
        this.activeMap = this.gameMap;
    }

    private BaseMap loadLobbyMap() {
        MapEntry lobbyEntry = this.mapEntries.stream().filter(mapEntry -> mapEntry.getDirectoryRoot().toString().equalsIgnoreCase("lobby")).findAny()
                .orElseThrow(() -> new IllegalStateException("No lobby map found in the given path"));

        if (!lobbyEntry.hasMapFile()) {
            throw new IllegalStateException("Lobby map doesn't contains a map file");
        }

        this.activeMap = this.fileHandler.load(lobbyEntry.getMapFile(), BaseMap.class).get();
        InstanceContainer instanceContainer = MinecraftServer.getInstanceManager().createInstanceContainer();
        this.registerInstance(instanceContainer, lobbyEntry);
        return this.activeMap;
    }

    @Override
    public void saveMap(Path path, BaseMap baseMap) {
        throw new UnsupportedOperationException();
    }

    public BaseMap getActiveMap() {
        return this.activeMap;
    }

    public GameMap getGameMap() {
        return gameMap;
    }

}
