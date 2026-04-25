package net.onelitefeather.cygnus.setup.map;

import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.InstanceContainer;
import net.onelitefeather.cygnus.common.map.filter.MapFilters;
import net.onelitefeather.cygnus.common.util.GsonHelper;
import net.theevilreaper.aves.map.BaseMap;
import net.theevilreaper.aves.map.MapEntry;
import net.theevilreaper.aves.map.provider.AbstractMapProvider;

import java.nio.file.Path;
import java.util.Optional;

public final class SetupMapProvider extends AbstractMapProvider {

    public SetupMapProvider(Path path) {
        super(GsonHelper.FILE_HANDLER, MapFilters::filterMapsForSetup);
        this.mapEntries = loadMapEntries(path.resolve("maps"));

        Optional<MapEntry> lobbyOptional = this.mapEntries.stream().filter(MapEntry::hasMapFile)
                .filter(mapEntry -> mapEntry.getDirectoryRoot().equals("lobby"))
                .findAny();

        if (lobbyOptional.isEmpty()) {
            throw new IllegalStateException("Lobby map not found in the maps directory.");
        }

        MapEntry lobbyEntry = lobbyOptional.get();
        this.mapEntries.remove(lobbyEntry);

        Optional<BaseMap> loadedMap = this.fileHandler.load(lobbyEntry.getMapFile(), BaseMap.class);

        if (loadedMap.isEmpty()) {
            throw new IllegalStateException("Unable to load lobby map");
        }

        this.activeMap = loadedMap.get();
        InstanceContainer instanceContainer = MinecraftServer.getInstanceManager().createInstanceContainer();
        this.registerInstance(instanceContainer, lobbyEntry);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveMap(Path path, BaseMap baseMap) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
