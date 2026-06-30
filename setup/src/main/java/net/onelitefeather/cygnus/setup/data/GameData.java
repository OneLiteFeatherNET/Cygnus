package net.onelitefeather.cygnus.setup.data;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.anvil.AnvilLoader;
import net.minestom.server.inventory.InventoryType;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.common.map.GameMapBuilder;
import net.onelitefeather.cygnus.common.util.GsonHelper;
import net.onelitefeather.cygnus.setup.inventory.page.PageHeaderFormatter;
import net.onelitefeather.cygnus.setup.inventory.slot.PositionSlot;
import net.onelitefeather.cygnus.setup.inventory.view.InventoryMode;
import net.onelitefeather.cygnus.setup.inventory.view.MapDataOverviewInventory;
import net.onelitefeather.cygnus.setup.inventory.view.SurvivorViewInventory;
import net.onelitefeather.cygnus.setup.item.SetupItemId;
import net.onelitefeather.cygnus.setup.item.SetupItems;
import net.onelitefeather.cygnus.setup.map.MapDataCategory;
import net.onelitefeather.cygnus.setup.util.SetupMessages;
import net.theevilreaper.aves.inventory.InventoryLayout;
import net.theevilreaper.aves.inventory.pageable.PageableInventory;
import net.theevilreaper.aves.inventory.pageable.TitleData;
import net.theevilreaper.aves.inventory.slot.ISlot;
import net.theevilreaper.aves.inventory.util.LayoutCalculator;
import net.theevilreaper.aves.map.BaseMapBuilder;
import net.theevilreaper.aves.map.MapEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GameData extends InstanceSetupData {

    private final MapDataOverviewInventory inventory;
    private final SurvivorViewInventory survivorInventory;
    private final PageableInventory pageInventory;
    private GameMapBuilder gameMapBuilder;
    private boolean pageMode;
    private boolean survivorMode;

    /**
     * Constructs a new GameData instance.
     *
     * @param uuid     the UUID of the player
     * @param mapEntry the map entry associated with this game data
     */
    public GameData(UUID uuid, MapEntry mapEntry) {
        super(uuid, mapEntry, BossBar.Color.RED);
        Player player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(uuid);
        this.loadData();
        if (player == null) {
            throw new IllegalArgumentException("Player with UUID " + uuid + " is not online.");
        }

        this.inventory = new MapDataOverviewInventory(player, this.gameMapBuilder, InventoryMode.GAME);
        this.survivorInventory = new SurvivorViewInventory(player, this.gameMapBuilder);
        this.pageInventory = PageableInventory
                .builder()
                .titleData(
                        TitleData
                                .builder()
                                .pageMapper(PageHeaderFormatter::format)
                                .showPageNumbers(true)
                                .build()
                )
                .player(player)
                .type(InventoryType.CHEST_6_ROW)
                .slotRange(LayoutCalculator.quad(InventoryType.CHEST_1_ROW.getSize(), InventoryType.CHEST_5_ROW.getSize() - 1))
                .layout(InventoryLayout.fromType(InventoryType.CHEST_6_ROW))
                .values(getPageSlots())
                .build();
    }

    private List<ISlot> getPageSlots() {
        if (this.gameMapBuilder.getPageFaces().isEmpty()) return List.of();
        List<ISlot> pageSlots = new ArrayList<>(this.gameMapBuilder.getPageFaces().size());
        this.gameMapBuilder.getPageFaces().forEach(pageFace -> {
           pageSlots.add(new PositionSlot(MapDataCategory.PAGE, ((Pos) pageFace.position())));
        });
        return pageSlots;
    }

    /**
     * Swaps between area mode and normal mode.
     */
    public void swapPageMode() {
        this.pageMode = !this.pageMode;
    }

    public void swapSurvivorMode() {
        this.survivorMode = !this.survivorMode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void openInventory(InventoryTarget target) {
        switch (target) {
            case GENERAL -> this.inventory.open();
            case SURVIVOR -> this.survivorInventory.open();
            case PAGE -> this.pageInventory.open();
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
            case PAGE -> this.pageInventory.open();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPosition(MapDataCategory category, Player player) {
        Pos pos = player.getPosition();
        switch (category) {
            case SPAWN -> {
                getMapBuilder().spawn(pos);
                triggerUpdate(InventoryTarget.GENERAL);
            }
            case SLENDER -> {
                ((GameMapBuilder) getMapBuilder()).setSlenderSpawn(pos);
                triggerUpdate(InventoryTarget.GENERAL);
            }
            case SURVIVOR -> {
                Pos spawnPos = new Pos(
                        pos.blockX(),
                        pos.blockY() + 1,
                        pos.blockZ(),
                        player.getPosition().yaw(),
                        0f
                );
                this.gameMapBuilder.addSurvivorSpawn(spawnPos);
                triggerUpdate(InventoryTarget.SURVIVOR);
            }
            default -> {
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleItemInteraction(Player player, byte tagValue) {
        if (SetupItemId.PAGE == tagValue) {
            swapPageMode();
            if (hasPageMode()) {
                player.sendMessage(SetupMessages.PAGE_MODE_ENABLED);
                player.sendMessage(SetupMessages.PAGE_MODE_INFORM);
            }
            SetupItems.setPageItems(player);
            return;
        }
        if (SetupItemId.LEAVE_PAGE == tagValue) {
            swapPageMode();
            player.sendMessage(SetupMessages.PAGE_MODE_DISABLED);
            SetupItems.setGameLayout(player);
            return;
        }

        if (SetupItemId.SURVIVOR == tagValue) {
            this.swapSurvivorMode();
            SetupItems.setSurvivorSpawn(player);
            return;
        }

        if (SetupItemId.SPAWNS == tagValue) {
            this.openInventory(InventoryTarget.SURVIVOR);
            return;
        }

        if (SetupItemId.LEAVE_MODE == tagValue) {
            this.swapSurvivorMode();
            SetupItems.setGameLayout(player);
            return;
        }

        super.handleItemInteraction(player, tagValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleDataDelete(MapDataCategory category) {
        switch (category) {
            case SPAWN -> gameMapBuilder.spawn(null);
            case NAME -> gameMapBuilder.name(null);
            case AUTHOR -> gameMapBuilder.builders("");
            case SLENDER -> gameMapBuilder.setSlenderSpawn(null);
            default -> throw new IllegalArgumentException("Unknown inventory category: " + category);
        }
        this.triggerUpdate(InventoryTarget.GENERAL);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleDataContextDelete(MapDataCategory category, Point point) {
        if (point instanceof Pos pos) {
            this.gameMapBuilder.removeSurvivorSpawn(pos);
            this.triggerUpdate(InventoryTarget.SURVIVOR);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save() {
        if (!this.mapEntry.hasMapFile()) {
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
        this.pageInventory.unregister();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadData() {
        Optional<GameMap> mapData =
                this.mapEntry.hasMapFile()
                        ? GsonHelper.FILE_HANDLER.load(mapEntry.getMapFile(), GameMap.class)
                        : Optional.empty();

        this.gameMapBuilder = mapData
                .map(GameMapBuilder::new)
                .orElseGet(GameMapBuilder::new);

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

    public boolean hasSurvivorMode() {
        return this.survivorMode;
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
