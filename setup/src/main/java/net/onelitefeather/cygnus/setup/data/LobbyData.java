package net.onelitefeather.cygnus.setup.data;

import net.kyori.adventure.bossbar.BossBar;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.anvil.AnvilLoader;
import net.onelitefeather.cygnus.common.util.GsonHelper;
import net.onelitefeather.cygnus.setup.inventory.view.InventoryMode;
import net.onelitefeather.cygnus.setup.inventory.view.MapDataOverviewInventory;
import net.onelitefeather.cygnus.setup.map.MapDataCategory;
import net.theevilreaper.aves.inventory.PersonalInventoryBuilder;
import net.theevilreaper.aves.map.BaseMap;
import net.theevilreaper.aves.map.BaseMapBuilder;
import net.theevilreaper.aves.map.MapEntry;

import java.nio.file.Files;
import java.util.Optional;
import java.util.UUID;

public final class LobbyData extends InstanceSetupData {

    private final PersonalInventoryBuilder viewInventory;
    private BaseMapBuilder mapBuilder;

    public LobbyData(UUID uuid, MapEntry mapEntry) {
        super(uuid, mapEntry, BossBar.Color.GREEN);
        this.loadData();
        Player player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(uuid);

        if (player == null) {
            throw new IllegalArgumentException("Player with UUID " + uuid + " is not online.");
        }

        this.viewInventory = new MapDataOverviewInventory(player, this.mapBuilder, InventoryMode.LOBBY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPosition(MapDataCategory category, Player player) {
        if (category == MapDataCategory.SPAWN) {
            getMapBuilder().spawn(player.getPosition());
            triggerUpdate(InventoryTarget.GENERAL);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void openInventory(InventoryTarget target) {
        this.viewInventory.open();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void triggerUpdate(InventoryTarget target) {
        this.viewInventory.invalidateDataLayout();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save() {
        if (!Files.exists(mapEntry.getMapFile())) {
            this.mapEntry.createFile();
        }
        GsonHelper.FILE_HANDLER.save(mapEntry.getMapFile(), BaseMap.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void teleport(Player player) {
        super.teleport(player);
        Pos spawnPoint = this.mapBuilder.getSpawn() == null
                ? SPAWN_POINT
                : this.mapBuilder.getSpawn();
        player.setInstance(this.instance, spawnPoint);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        super.reset();
        this.viewInventory.unregister();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadData() {
        if (this.mapEntry == null) {
            this.mapBuilder = BaseMap.builder();
        } else {
            Optional<BaseMap> mapData = GsonHelper.FILE_HANDLER.load(mapEntry.getMapFile(), BaseMap.class);

            mapData.ifPresentOrElse(baseMap -> {
                this.mapBuilder = BaseMap.builder(baseMap);
            }, () -> this.mapBuilder = BaseMap.builder());
        }

        this.instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        AnvilLoader anvilLoader = new AnvilLoader(this.mapEntry.getDirectoryRoot());
        this.instance.setChunkLoader(anvilLoader);
        this.updateTitle();
        MinecraftServer.getInstanceManager().registerInstance(this.instance);
    }

    @Override
    public BaseMapBuilder getMapBuilder() {
        return mapBuilder;
    }
}
