package net.onelitefeather.cygnus.setup.data;

import net.kyori.adventure.bossbar.BossBar;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.anvil.AnvilLoader;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.common.map.GameMapBuilder;
import net.onelitefeather.cygnus.common.util.GsonHelper;
import net.theevilreaper.aves.file.FileHandler;
import net.theevilreaper.aves.map.BaseMapBuilder;
import net.theevilreaper.aves.map.MapEntry;

import java.nio.file.Files;
import java.util.Optional;
import java.util.UUID;

public class GameData extends InstanceSetupData {

    private final FileHandler fileHandler;
    //TODO: Implement inventory
    private Object inventory;
    private GameMapBuilder gameMapBuilder;
    private boolean areaMode;

    /**
     * Constructs a new GameData instance.
     *
     * @param uuid       the UUID of the player
     * @param mapEntry   the map entry associated with this game data
     * @param fileHandler the file handler for saving and loading game data
     */
    public GameData(UUID uuid, MapEntry mapEntry, FileHandler fileHandler) {
        super(uuid, mapEntry, BossBar.Color.RED);
        this.fileHandler = fileHandler;
        Player player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(uuid);
        this.loadData();
        if (player == null) {
            throw new IllegalArgumentException("Player with UUID " + uuid + " is not online.");
        }
    }

    /**
     * Swaps between area mode and normal mode.
     */
    public void swapAreaMode() {
        this.areaMode = !this.areaMode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void openInventory(Player player) {
        //player.openInventory(this.inventory.getInventory());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void triggerUpdate() {
     //  this.inventory.invalidateDataLayout();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save() {
        if (!Files.exists(mapEntry.getMapFile())) {
            this.mapEntry.createFile();
        }
        GsonHelper.FILE_HANDLER.save(mapEntry.getMapFile(), this.gameMapBuilder.build());
    }

    @Override
    public void teleport(Player player) {
        super.teleport(player);
        Pos spawnPoint = this.gameMapBuilder.getSpawn() == null
                ? SPAWN_POINT
                : this.gameMapBuilder.getSpawn();
        player.setInstance(this.instance, spawnPoint);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        super.reset();
       // this.inventory.unregister();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadData() {
        if (!this.mapEntry.hasMapFile()) {
            this.gameMapBuilder = new GameMapBuilder();
        } else {
            Optional<GameMap> mapData = fileHandler.load(mapEntry.getMapFile(), GameMap.class);
            mapData.ifPresentOrElse(gameMap ->
                            this.gameMapBuilder = new GameMapBuilder(gameMap),
                    () -> this.gameMapBuilder = new GameMapBuilder()
            );
        }
       // this.inventory = new LobbyViewInventory(this.gameMapBuilder);
        this.instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        AnvilLoader anvilLoader = new AnvilLoader(this.mapEntry.getDirectoryRoot());
        this.instance.setChunkLoader(anvilLoader);
        this.updateTitle();
        MinecraftServer.getInstanceManager().registerInstance(this.instance);
    }

    /**
     * Returns an indication if the area mode is active or not.
     *
     * @return true if area mode is active, false otherwise
     */
    public boolean hasAreaMode() {
        return areaMode;
    }

    /**
     * Returns the GameMapBuilder instance used for building the game map.
     *
     * @return the builder instance
     */
    @Override
    public BaseMapBuilder getMapBuilder() {
        return this.gameMapBuilder;
    }
}
