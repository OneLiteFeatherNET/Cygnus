package net.onelitefeather.cygnus.map;

import net.minestom.server.MinecraftServer;
import net.minestom.server.event.EventDispatcher;
import net.kyori.adventure.key.Key;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.world.DimensionType;
import net.onelitefeather.cygnus.common.dimension.DimensionFactory;
import net.onelitefeather.cygnus.common.dimension.MapAtmosphere;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.common.map.filter.MapFilters;
import net.onelitefeather.cygnus.common.util.GsonHelper;
import net.onelitefeather.cygnus.common.util.Helper;
import net.onelitefeather.cygnus.map.event.GameMapLoadedEvent;
import net.onelitefeather.falco.anvil.FalcoAnvilLoader;
import net.theevilreaper.aves.map.BaseMap;
import net.theevilreaper.aves.map.MapEntry;
import net.theevilreaper.aves.map.provider.AbstractMapProvider;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class GameMapProvider extends AbstractMapProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(GameMapProvider.class);

    /** Namespace every dimension this provider registers lives in. */
    private static final String DIMENSION_NAMESPACE = "cygnus";

    private final List<FalcoAnvilLoader> chunkLoaders;
    private final MapEntry gameEntry;
    private final RegistryKey<DimensionType> gameDimension;
    private @Nullable InstanceContainer gameInstance;
    private @Nullable GameMap gameMap;
    private @Nullable InstanceContainer previousInstance;

    public GameMapProvider(Path path) {
        super(GsonHelper.FILE_HANDLER, MapFilters::filterMapsForGame);
        this.loadMapEntries(path.resolve("game").resolve("maps"));
        this.chunkLoaders = new ArrayList<>();
        if (this.mapEntries.isEmpty()) {
            throw new IllegalStateException("No maps found in the given path");
        }

        this.loadLobbyMap();
        this.gameEntry = this.mapEntries.stream()
                .filter(entry -> !entry.getDirectoryRoot().toString().equalsIgnoreCase("lobby"))
                .findAny()
                .orElseThrow(() -> new IllegalStateException("No game map found"));
        this.gameDimension = registerDimension(readGameMap());
    }

    /**
     * Registers the dimension the game map asks for, if it asks for one.
     *
     * <p>This happens here, in the constructor, rather than in {@link #loadGameMap()} on purpose:
     * registry data only reaches a client during its configuration phase, and the provider is built
     * before the server starts accepting connections. Registering later would leave every player
     * already online without the dimension they are about to be moved into.</p>
     *
     * @param map the loaded game map
     * @return the key of the registered dimension, or {@link DimensionType#OVERWORLD} if the map
     *         declares no atmosphere
     */
    private RegistryKey<DimensionType> registerDimension(GameMap map) {
        MapAtmosphere atmosphere = map.getAtmosphere();
        if (atmosphere == null) {
            LOGGER.info("Map {} declares no atmosphere, running it on {}", map.name(), DimensionType.OVERWORLD.key());
            return DimensionType.OVERWORLD;
        }

        Key key = Key.key(DIMENSION_NAMESPACE, "map/" + toKeyValue(map.name()));
        LOGGER.info(
                "Registered dimension {} for map {}: fog {} from {} to {} blocks",
                key, map.name(), atmosphere.fogColor(), atmosphere.fogStartDistance(), atmosphere.fogEndDistance()
        );
        return DimensionFactory.create(key, atmosphere);
    }

    /**
     * Reduces a map name to the characters a {@link Key} accepts, so any name a builder types still
     * yields a registrable dimension key.
     *
     * @param name the map name
     * @return the name in lower case with every unsupported character replaced by an underscore
     */
    private static String toKeyValue(String name) {
        String sanitized = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
        return sanitized.isBlank() ? "unnamed" : sanitized;
    }

    /**
     * Reads the game map from its map file.
     *
     * @return the loaded game map
     * @throws IllegalStateException if the file cannot be read
     */
    private GameMap readGameMap() {
        return this.fileHandler.load(this.gameEntry.getMapFile(), GameMap.class)
                .orElseThrow(() -> new IllegalStateException("Failed to load GameMap from file: " + this.gameEntry.getMapFile()));
    }

    public void loadGameMap() {
        if (this.gameMap != null) return; // idempotent

        this.gameMap = readGameMap();
        this.gameInstance = MinecraftServer.getInstanceManager().createInstanceContainer(this.gameDimension);
        this.gameInstance.setTime(Helper.NEW_MOON_TIME);
        this.registerFalcoInstance(this.gameInstance, this.gameEntry);
        EventDispatcher.call(new GameMapLoadedEvent(this.gameMap, this.gameInstance));
    }

    /**
     * Returns the dimension the game instance runs on.
     *
     * @return the registered per-map dimension, or {@link DimensionType#OVERWORLD} if the map
     *         declares no atmosphere
     */
    public RegistryKey<DimensionType> getGameDimension() {
        return gameDimension;
    }

    /**
     * Switches the provider over to the game map.
     *
     * <p>This only moves the active references; the lobby instance stays registered so the players
     * can still be moved out of it. Call {@link #releasePreviousInstance()} once they are gone.</p>
     *
     * @throws IllegalStateException if the game map has not been loaded yet
     */
    public void switchToGameMap() {
        if (this.gameInstance == null || this.gameMap == null) {
            throw new IllegalStateException("The game map has not been loaded yet");
        }
        this.previousInstance = this.activeInstance;
        this.activeInstance = this.gameInstance;
        this.activeMap = this.gameMap;
    }

    /**
     * Unregisters the instance the provider was on before the last switch.
     *
     * <p>Minestom refuses to unregister an instance that still holds online players, so this must
     * run after every player has been moved into the new instance. Calling it more than once, or
     * without a previous switch, does nothing.</p>
     */
    public void releasePreviousInstance() {
        if (this.previousInstance == null) return;
        MinecraftServer.getInstanceManager().unregisterInstance(this.previousInstance);
        this.previousInstance = null;
    }

    private BaseMap loadLobbyMap() {
        MapEntry lobbyEntry = this.mapEntries.stream().filter(mapEntry -> mapEntry.getDirectoryRoot().toString().contains("lobby")).findAny()
                .orElseThrow(() -> new IllegalStateException("No lobby map found in the given path"));

        if (!lobbyEntry.hasMapFile()) {
            throw new IllegalStateException("Lobby map doesn't contains a map file");
        }

        this.mapEntries.remove(lobbyEntry);

        this.activeMap = this.fileHandler.load(lobbyEntry.getMapFile(), BaseMap.class)
                .orElseThrow(() -> new IllegalStateException("Failed to load LobbyMap from file: " + lobbyEntry.getMapFile()));
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveMap(Path path, BaseMap baseMap) {
        throw new UnsupportedOperationException();
    }

    public @Nullable GameMap getGameMap() {
        return this.gameMap;
    }
}
