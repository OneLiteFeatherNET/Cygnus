package net.onelitefeather.cygnus.map;

import com.google.gson.Gson;
import de.icevizion.aves.file.GsonFileHandler;
import de.icevizion.aves.file.gson.PositionGsonAdapter;
import de.icevizion.aves.map.BaseMap;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.AnvilLoader;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.utils.validate.Check;
import net.onelitefeather.cygnus.game.adapter.PageResourceAdapter;
import net.onelitefeather.cygnus.page.PageProvider;
import net.onelitefeather.cygnus.setup.MapEntry;
import net.onelitefeather.cygnus.config.GameConfig;
import net.onelitefeather.cygnus.game.data.PageResource;
import net.onelitefeather.cygnus.utils.Helper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since
 **/

public final class MapProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(MapProvider.class);
    private static final String MAP_PATH = "maps";
    private final GsonFileHandler fileHandler;
    private final MapPool mapPool;
    private final Gson gson;
    private InstanceContainer instance;
    private BaseMap activeMap;
    private final boolean debug;

    private GameMap gameMap;
    private InstanceContainer gameInstance;

    private final PageProvider pageProvider;

    public MapProvider(@NotNull Path path, @NotNull InstanceContainer instance, @NotNull PageProvider pageProvider, boolean debug) {
        this.mapPool = new MapPool(path.resolve(MAP_PATH), debug);
        this.debug = debug;
        this.instance = instance;
        this.pageProvider = pageProvider;
        var typeAdapter = new PositionGsonAdapter();
        this.gson = new Gson().newBuilder()
                .registerTypeAdapter(Pos.class, typeAdapter)
                .registerTypeAdapter(Vec.class, typeAdapter)
                .registerTypeAdapter(PageResource.class, new PageResourceAdapter())
                .create();
        this.fileHandler = new GsonFileHandler(this.gson);
        this.loadLobbyMap();
    }

    private void loadLobbyMap() {
        var lobbyData = this.fileHandler.load(this.mapPool.getLobbyEntry().path().resolve(GameConfig.MAP_FILE_NAME), BaseMap.class);
        this.instance.setChunkLoader(new AnvilLoader(mapPool.getLobbyEntry().path()));
        this.activeMap = lobbyData.get();
    }

    public void loadGameMap() {
        var mapEntry = this.mapPool.getMapEntry();
        var gameData = this.fileHandler.load(mapEntry.path().resolve(GameConfig.MAP_FILE_NAME), GameMap.class);
        this.pageProvider.loadPageData(gameData.get().getPageFaces());
        InstanceContainer gameInstance = MinecraftServer.getInstanceManager().createInstanceContainer();
        prepareInstanceData(gameInstance);
        MinecraftServer.getInstanceManager().registerInstance(gameInstance);
        gameInstance.setChunkLoader(new AnvilLoader(mapEntry.path()));

        this.gameMap = gameData.get();
        this.gameInstance = gameInstance;

        this.pageProvider.collectStartPages(gameInstance);
    }

    public void saveMap(@NotNull Path path, @NotNull BaseMap baseMap) {
        Check.argCondition(!debug, "The method can only be used in the setup mode!");
        this.fileHandler.save(path, baseMap instanceof GameMap gameMap ? gameMap : baseMap);
    }

    public void switchToGameMap() {
        this.activeMap = null;
        MinecraftServer.getInstanceManager().unregisterInstance(instance);
        this.instance = null;
    }

    public void prepareInstanceData(@NotNull InstanceContainer instance) {
        instance.setTimeRate(0);
        instance.setTimeUpdate(null);
        instance.setTime(Helper.MIDNIGHT_TIME);
        instance.enableAutoChunkLoad(true);
    }

    public void setInstance(@NotNull InstanceContainer instance) {
        this.instance = instance;
    }

    public void setMidnight() {
        if (this.instance == null) return;
        this.instance.setTime(Helper.MIDNIGHT_TIME);
    }


    public @NotNull Instance getInstance() {
        return this.instance != null ? instance : gameInstance;
    }

    public @NotNull @UnmodifiableView List<MapEntry> getAvailableMaps() {
        return Collections.unmodifiableList(this.mapPool.getAvailableMaps());
    }

    public InstanceContainer getGameInstance() {
        return gameInstance;
    }

    public @NotNull BaseMap getActiveMap() {
        return this.activeMap;
    }

    public GameMap getGameMap() {
        return gameMap;
    }
}
