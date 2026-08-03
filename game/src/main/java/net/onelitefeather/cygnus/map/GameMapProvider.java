package net.onelitefeather.cygnus.map;

import net.minestom.server.MinecraftServer;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.world.DimensionType;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.common.map.filter.MapFilters;
import net.onelitefeather.cygnus.common.util.GsonHelper;
import net.onelitefeather.cygnus.common.util.Helper;
import net.onelitefeather.cygnus.map.event.GameMapLoadedEvent;
import net.onelitefeather.falco.anvil.FalcoAnvilLoader;
import net.theevilreaper.aves.map.BaseMap;
import net.theevilreaper.aves.map.MapEntry;
import net.theevilreaper.aves.map.provider.AbstractMapProvider;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class GameMapProvider extends AbstractMapProvider {

    private final List<FalcoAnvilLoader> chunkLoaders = new ArrayList<>();
    private InstanceContainer gameInstance;
    private GameMap gameMap;

    public GameMapProvider(Path path) {
        super(GsonHelper.FILE_HANDLER, MapFilters::filterMapsForGame);
        this.loadMapEntries(path.resolve("game").resolve("maps"));

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
        this.registerFalcoInstance(this.gameInstance, gameEntry);
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
        MapEntry lobbyEntry = this.mapEntries.stream().filter(mapEntry -> mapEntry.getDirectoryRoot().toString().contains("lobby")).findAny()
                .orElseThrow(() -> new IllegalStateException("No lobby map found in the given path"));

        if (!lobbyEntry.hasMapFile()) {
            throw new IllegalStateException("Lobby map doesn't contains a map file");
        }

        this.mapEntries.remove(lobbyEntry);

        this.activeMap = this.fileHandler.load(lobbyEntry.getMapFile(), BaseMap.class).get();
        InstanceContainer instanceContainer = MinecraftServer.getInstanceManager().createInstanceContainer();
        this.registerFalcoInstance(instanceContainer, lobbyEntry);
        this.activeInstance = instanceContainer;
        return this.activeMap;
    }

    /**
     * Registers the given instance with a {@link FalcoAnvilLoader} attached to it.
     *
     * <p>This mirrors {@link AbstractMapProvider#registerInstance(InstanceContainer, MapEntry)} but
     * swaps the chunk loader: the Aves method hard-wires Minestom's own {@code AnvilLoader}, while
     * Falco reads region files in parallel and fails loudly on a broken chunk instead of reporting
     * it as absent. The loader is kept so it can be closed in {@link #close()}.</p>
     *
     * @param instance the instance the map is loaded into
     * @param mapEntry the map entry whose directory root is the world root of the loader
     */
    private void registerFalcoInstance(InstanceContainer instance, MapEntry mapEntry) {
        FalcoAnvilLoader chunkLoader =
                new FalcoAnvilLoader(mapEntry.getDirectoryRoot(), DimensionType.OVERWORLD.key());
        this.chunkLoaders.add(chunkLoader);

        instance.setChunkLoader(chunkLoader);
        instance.enableAutoChunkLoad(true);

        var defaultClock = instance.defaultClock();
        if (defaultClock != null) {
            defaultClock.rate(0f);
        }
        MinecraftServer.getInstanceManager().registerInstance(instance);
    }

    /**
     * Closes every chunk loader this provider opened.
     *
     * <p>Unlike Minestom's {@code AnvilLoader}, a {@link FalcoAnvilLoader} holds its region files
     * open, so it has to be closed once the server shuts down. Calling this more than once is
     * harmless.</p>
     */
    public void close() {
        for (FalcoAnvilLoader chunkLoader : this.chunkLoaders) {
            try {
                chunkLoader.close();
            } catch (IOException exception) {
                MinecraftServer.getExceptionManager().handleException(exception);
            }
        }
        this.chunkLoaders.clear();
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
