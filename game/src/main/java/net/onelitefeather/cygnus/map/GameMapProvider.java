package net.onelitefeather.cygnus.map;

import net.minestom.server.MinecraftServer;
import net.minestom.server.event.EventDispatcher;
import net.kyori.adventure.key.Key;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.world.DimensionType;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.common.dimension.BlendedAtmosphere;
import net.onelitefeather.cygnus.common.dimension.DimensionAtmosphere;
import net.onelitefeather.cygnus.common.dimension.DimensionFactory;
import net.onelitefeather.cygnus.common.dimension.StaticDimensionPreset;
import net.onelitefeather.cygnus.common.dimension.MapAtmosphere;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.common.map.filter.MapFilters;
import net.onelitefeather.cygnus.common.map.settings.CreekSettings;
import net.onelitefeather.cygnus.common.map.settings.PageSettings;
import net.onelitefeather.cygnus.common.page.PagePool;
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
    private final RegistryKey<DimensionType> lobbyDimension;
    private final PageSettings pageSettings;
    private final CreekSettings creekSettings;
    private @Nullable InstanceContainer gameInstance;
    private @Nullable GameMap gameMap;
    private @Nullable InstanceContainer previousInstance;
    private boolean releasePending;

    /**
     * Creates a provider for the given lobby and game map, with the default share of the game
     * map's atmosphere in the lobby.
     *
     * @param lobbyEntry the entry of the lobby map
     * @param game       the game map the round is played on
     */
    public GameMapProvider(MapEntry lobbyEntry, MapCandidate game) {
        this(lobbyEntry, game, GameConfig.DEFAULT_LOBBY_ATMOSPHERE_SHARE);
    }

    /**
     * Creates a provider for the given lobby and game map.
     *
     * <p>The game map has to be known here already: the lobby's dimension is derived from the game
     * map's atmosphere, and both dimensions have to be registered before the lobby instance is
     * created.</p>
     *
     * @param lobbyEntry           the entry of the lobby map
     * @param game                 the game map the round is played on
     * @param lobbyAtmosphereShare how far the lobby's atmosphere is taken towards the game map's
     *                             own, between 0 and 1
     */
    public GameMapProvider(MapEntry lobbyEntry, MapCandidate game, float lobbyAtmosphereShare) {
        super(GsonHelper.FILE_HANDLER, MapFilters::filterMapsForGame);
        this.chunkLoaders = new ArrayList<>();
        this.gameEntry = game.entry();
        GameMap map = game.map();
        // Read with the map before any service is built, so every one of them gets the map's own values.
        this.pageSettings = map.getPageSettings();
        this.creekSettings = map.getCreekSettings();
        this.gameDimension = registerDimension(map);
        this.lobbyDimension = registerLobbyDimension(map, lobbyAtmosphereShare);
        this.loadLobbyMap(lobbyEntry);
    }

    /**
     * Registers the weakened version of the game map's atmosphere the lobby waits in.
     *
     * <p>A player who walks straight from a vanilla sky into a map that closes in at forty blocks
     * meets the whole atmosphere at once, at the same moment the round starts. Giving the lobby the
     * map's own colours and haze, held at a distance, turns that into a build-up: the start of the
     * round reads as the world tightening rather than as a cut.</p>
     *
     * <p>Registered here for the same reason as the game's own dimension: registry data only
     * reaches a client during its configuration phase, and a dimension registered after a player
     * has logged in is a dimension that player cannot be put into.</p>
     *
     * @param map   the loaded game map
     * @param share how far to take the lobby towards the map's atmosphere
     * @return the key of the registered dimension, or {@link DimensionType#OVERWORLD} if the map
     *         declares no atmosphere or the share is zero
     */
    private RegistryKey<DimensionType> registerLobbyDimension(GameMap map, float share) {
        MapAtmosphere atmosphere = map.getAtmosphere();
        if (atmosphere == null || share <= 0f) {
            return DimensionType.OVERWORLD;
        }

        DimensionAtmosphere weakened = BlendedAtmosphere.between(StaticDimensionPreset.BRIGHT, atmosphere, share);
        Key key = Key.key(DIMENSION_NAMESPACE, "map/" + toKeyValue(map.name()) + "/lobby");
        LOGGER.info(
                "Registered lobby dimension {} at {} of map {}: fog {} from {} to {} blocks",
                key, share, map.name(), weakened.fogColor(),
                weakened.fogStartDistance(), weakened.fogEndDistance()
        );
        return DimensionFactory.create(key, weakened);
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
     * Returns the page values of the game map. Known as soon as the provider exists, long before
     * the map is loaded into an instance.
     *
     * @return the page settings of the game map
     */
    public PageSettings pageSettings() {
        return this.pageSettings;
    }

    /**
     * Returns the creek values of the game map. Known as soon as the provider exists, long before
     * the map is loaded into an instance.
     *
     * @return the creek settings of the game map
     */
    public CreekSettings creekSettings() {
        return this.creekSettings;
    }

    /**
     * Loads the game map into a fresh instance. The page pool of the round is picked here, once, so
     * the map carries only the spots of that pool (see {@link PagePool#forRound(Path, int)}).
     */
    public void loadGameMap() {
        if (this.gameMap != null) return; // idempotent

        GameMap map = MapCatalog.readGameMap(this.gameEntry.getMapFile());
        PagePool pool = PagePool.forRound(this.gameEntry.getMapFile(), map.getPageSettings().minActivePages());
        this.gameMap = map.withPageFaces(pool.pages());
        this.gameInstance = MinecraftServer.getInstanceManager().createInstanceContainer(this.gameDimension);
        this.gameInstance.setTime(Helper.NEW_MOON_TIME);
        this.registerFalcoInstance(this.gameInstance, this.gameEntry);
        EventDispatcher.call(new GameMapLoadedEvent(this.gameMap, this.gameInstance, pool));
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
     * <p>Minestom refuses to unregister an instance that still holds online players, so this waits
     * until the instance is empty rather than assuming the move is done. {@code Player#setInstance}
     * completes only once the target chunks have loaded, and since the game map runs on its own
     * dimension that move also costs a full respawn - far longer than the tick the caller schedules
     * this on. Calling it more than once, or without a previous switch, does nothing.</p>
     */
    public void releasePreviousInstance() {
        InstanceContainer previous = this.previousInstance;
        if (previous == null) return;

        if (previous.getPlayers().isEmpty()) {
            MinecraftServer.getInstanceManager().unregisterInstance(previous);
            this.previousInstance = null;
            return;
        }

        if (this.releasePending) return;
        this.releasePending = true;
        MinecraftServer.getSchedulerManager().submitTask(() -> {
            InstanceContainer pending = this.previousInstance;
            if (pending == null) {
                this.releasePending = false;
                return TaskSchedule.stop();
            }
            if (!pending.getPlayers().isEmpty()) return TaskSchedule.nextTick();

            MinecraftServer.getInstanceManager().unregisterInstance(pending);
            this.previousInstance = null;
            this.releasePending = false;
            return TaskSchedule.stop();
        });
    }

    private BaseMap loadLobbyMap(MapEntry lobbyEntry) {
        if (!lobbyEntry.hasMapFile()) {
            throw new IllegalStateException("Lobby map doesn't contains a map file");
        }

        this.activeMap = this.fileHandler.load(lobbyEntry.getMapFile(), BaseMap.class)
                .orElseThrow(() -> new IllegalStateException("Failed to load LobbyMap from file: " + lobbyEntry.getMapFile()));
        InstanceContainer instanceContainer = MinecraftServer.getInstanceManager().createInstanceContainer(this.lobbyDimension);
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
