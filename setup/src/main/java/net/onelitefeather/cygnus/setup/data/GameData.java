package net.onelitefeather.cygnus.setup.data;

import net.kyori.adventure.bossbar.BossBar;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.anvil.AnvilLoader;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.common.map.GameMapBuilder;
import net.onelitefeather.cygnus.common.util.GsonHelper;
import net.onelitefeather.cygnus.setup.inventory.view.MapDataOveriewInventory;
import net.onelitefeather.cygnus.setup.inventory.view.SurvivorViewInventory;
import net.onelitefeather.cygnus.setup.item.SetupItems;
import net.onelitefeather.cygnus.setup.util.SetupMessages;
import net.theevilreaper.aves.map.BaseMapBuilder;
import net.theevilreaper.aves.map.MapEntry;

import java.nio.file.Files;
import java.util.Optional;
import java.util.UUID;

public class GameData extends InstanceSetupData {

    private final MapDataOveriewInventory inventory;
    private final SurvivorViewInventory survivorInventory;
    private GameMapBuilder gameMapBuilder;
    private boolean pageMode;

    /**
     * Constructs a new GameData instance.
     *
     * @param uuid       the UUID of the player
     * @param mapEntry   the map entry associated with this game data
     */
    public GameData(UUID uuid, MapEntry mapEntry) {
        super(uuid, mapEntry, BossBar.Color.RED);
        Player player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(uuid);
        this.loadData();
        if (player == null) {
            throw new IllegalArgumentException("Player with UUID " + uuid + " is not online.");
        }

        System.out.println("After loadData: " + System.identityHashCode(this.gameMapBuilder));
        this.inventory = new MapDataOveriewInventory(player, this.gameMapBuilder);
        this.survivorInventory = new SurvivorViewInventory(player, this.gameMapBuilder);
    }

    /**
     * Swaps between area mode and normal mode.
     */
    public void swapPageMode() {
        this.pageMode = !this.pageMode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void openInventory(InventoryTarget target) {
        switch (target) {
            case GENERAL -> this.inventory.open();
            case SURVIVOR -> this.survivorInventory.open();
            case PAGE -> {

            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void triggerUpdate(InventoryTarget target) {
        switch (target) {
            case GENERAL -> this.inventory.invalidateDataLayout();
            case SURVIVOR -> this.survivorInventory.invalidateDataLayout();
            case PAGE -> {
                // Nothing to do here
            }
        }
    }

    @Override
    public void handleItemInteraction(Player player, byte tagValue) {
        if (3 == tagValue) {
            swapPageMode();
            if (hasPageMode()) {
                player.sendMessage(SetupMessages.PAGE_MODE_ENABLED);
                player.sendMessage(SetupMessages.PAGE_MODE_INFORM);
            }
            SetupItems.setPageItems(player);
            return;
        }
        if (4 == tagValue) {
            swapPageMode();
            player.sendMessage(SetupMessages.PAGE_MODE_DISABLED);
            SetupItems.setGameLayout(player);
            return;
        }
        super.handleItemInteraction(player, tagValue);
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
        this.survivorInventory.unregister();
        this.inventory.unregister();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadData() {
        if (!this.mapEntry.hasMapFile()) {
            this.gameMapBuilder = new GameMapBuilder();
        } else {
            Optional<GameMap> mapData = GsonHelper.FILE_HANDLER.load(mapEntry.getMapFile(), GameMap.class);
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
     * Returns an indication if the page mode is active or not.
     *
     * @return true if page mode is active, false otherwise
     */
    public boolean hasPageMode() {
        return pageMode;
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
