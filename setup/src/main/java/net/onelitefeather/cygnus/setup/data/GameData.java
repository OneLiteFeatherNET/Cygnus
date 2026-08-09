package net.onelitefeather.cygnus.setup.data;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.utils.Direction;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.common.map.GameMapBuilder;
import net.onelitefeather.cygnus.common.util.GsonHelper;
import net.onelitefeather.cygnus.setup.inventory.page.PageHeaderFormatter;
import net.onelitefeather.cygnus.setup.inventory.slot.PageSlot;
import net.onelitefeather.cygnus.setup.inventory.view.InventoryMode;
import net.onelitefeather.cygnus.setup.inventory.view.MapDataOverviewInventory;
import net.onelitefeather.cygnus.setup.inventory.view.SurvivorViewInventory;
import net.onelitefeather.cygnus.setup.item.SetupItemId;
import net.onelitefeather.cygnus.setup.item.SetupItems;
import net.onelitefeather.cygnus.setup.map.MapDataCategory;
import net.onelitefeather.cygnus.common.page.PageResource;
import net.onelitefeather.cygnus.setup.player.SetupPlayer;
import net.onelitefeather.cygnus.setup.util.SetupMessages;
import net.theevilreaper.aves.inventory.layout.InventoryLayout;
import net.theevilreaper.aves.inventory.pageable.PageableInventory;
import net.theevilreaper.aves.inventory.pageable.TitleData;
import net.theevilreaper.aves.inventory.slot.ISlot;
import net.theevilreaper.aves.inventory.util.LayoutCalculator;
import net.theevilreaper.aves.map.BaseMapBuilder;
import net.theevilreaper.aves.map.MapEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
     * @param player   who owns the data object
     * @param mapEntry the map entry associated with this game data
     */
    public GameData(Player player, MapEntry mapEntry) {
        super(player.getUuid(), mapEntry, BossBar.Color.RED);
        this.loadData();

        this.inventory = new MapDataOverviewInventory(player, this.gameMapBuilder, InventoryMode.GAME);
        this.survivorInventory = new SurvivorViewInventory(player, this.gameMapBuilder);
        this.pageInventory = createPageInventory(player);
    }

    /**
     * Creates the [{@link PageableInventory} to display the given {@link PageResource}s.
     *
     * @param player who owns the inventory
     * @return the created inventory
     */
    private PageableInventory createPageInventory(Player player) {
        InventoryLayout layout = InventoryLayout.fromType(InventoryType.CHEST_6_ROW);
        layout.setItems(LayoutCalculator.fillRow(InventoryType.CHEST_1_ROW), SetupItems.DECORATION_PANE);
        layout.setItems(LayoutCalculator.fillRow(InventoryType.CHEST_6_ROW), SetupItems.DECORATION_PANE);

        return PageableInventory
                .builder()
                .titleData(
                        TitleData
                                .builder()
                                .title(Component.text("Page positions - "))
                                .pageMapper(PageHeaderFormatter::format)
                                .showPageNumbers(true)
                                .build()
                )
                .player(player)
                .slotRange(LayoutCalculator.quad(InventoryType.CHEST_1_ROW.getSize(), InventoryType.CHEST_5_ROW.getSize() - 1))
                .layout(layout)
                .values(getPageSlots())
                .build();
    }

    /**
     * Adds a new {@link PageResource} to the builder and inventory
     *
     * @param pos  of the resource
     * @param face of the resource
     */
    public void addPage(Vec pos, Direction face) {
        PageResource pageResource = new PageResource(pos, face);
        this.gameMapBuilder.addPage(pos, face);
        this.pageInventory.add(new PageSlot(pageResource));
    }

    /**
     * Returns the list of slots for the given {@link PageResource}s
     *
     * @return the created list
     */
    private List<ISlot> getPageSlots() {
        if (this.gameMapBuilder.getPageFaces().isEmpty()) return new ArrayList<>();
        List<ISlot> pageSlots = new ArrayList<>(this.gameMapBuilder.getPageFaces().size());
        this.gameMapBuilder.getPageFaces().forEach(pageFace -> pageSlots.add(new PageSlot(pageFace)));
        return pageSlots;
    }

    /**
     * Swaps between area mode and normal mode.
     */
    public void swapPageMode() {
        this.pageMode = !this.pageMode;
    }

    /**
     * Swaps to the survivor mode.
     */
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
            case GENERAL -> {
                this.inventory.invalidateDataLayout();
                this.inventory.invalidateLayout();
            }
            case SURVIVOR -> {
                this.survivorInventory.invalidateDataLayout();
                this.survivorInventory.invalidateLayout();
            }
            case PAGE -> {
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateTitle() {
        if (getMapBuilder().getName().equalsIgnoreCase("Map")) {
            this.title = null;
            super.updateTitle();
            return;
        }
        this.title = Component.text("Map: ").append(Component.text(getMapBuilder().getName(), MapDataCategory.NAME.getColor()));
        super.updateTitle();
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
                Pos spawnPos = new Pos(pos.x(), pos.y(), pos.z(), pos.yaw(), 0f);
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
                player.sendMessage(SetupMessages.getModeInform("page"));
                SetupItems.setPageItems(player);
            } else {
                player.sendMessage(SetupMessages.PAGE_MODE_DISABLED);
                SetupItems.setGameLayout(player);
            }
            return;
        }
        if (SetupItemId.LEAVE_PAGE == tagValue) {
            if (hasPageMode()) {
                swapPageMode();
            }
            player.sendMessage(SetupMessages.PAGE_MODE_DISABLED);
            SetupItems.setGameLayout(player);
            return;
        }

        if (SetupItemId.SURVIVOR == tagValue) {
            this.swapSurvivorMode();
            if (hasSurvivorMode()) {
                player.sendMessage(SetupMessages.SURVIVOR_MODE_ENABLED);
                player.sendMessage(SetupMessages.getModeInform("survivor"));
                SetupItems.setSurvivorSpawn(player);
            } else {
                player.sendMessage(SetupMessages.SURVIVOR_MODE_DISABLED);
                SetupItems.setGameLayout(player);
            }
            return;
        }

        if (SetupItemId.SPAWNS == tagValue) {
            this.openInventory(InventoryTarget.SURVIVOR);
            return;
        }

        if (SetupItemId.LEAVE_MODE == tagValue) {
            if (hasSurvivorMode()) {
                this.swapSurvivorMode();
            }
            player.sendMessage(SetupMessages.SURVIVOR_MODE_DISABLED);
            SetupItems.setGameLayout(player);
            return;
        }

        if (SetupItemId.PAGES == tagValue) {
            this.openInventory(InventoryTarget.PAGE);
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
            case NAME -> {
                gameMapBuilder.name("Map");
                this.updateTitle();
            }
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
        if (category == MapDataCategory.SURVIVOR) {
            Pos pos = point instanceof Pos givenPos ? givenPos : new Pos(point.x(), point.y(), point.z());
            this.gameMapBuilder.removeSurvivorSpawn(pos);
            this.triggerUpdate(InventoryTarget.SURVIVOR);
        } else if (category == MapDataCategory.PAGE) {
            Player player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(this.uuid);
            PageResource pageResource = null;
            if (player instanceof SetupPlayer setupPlayer) {
                pageResource = setupPlayer.getPageResource();
            }
            if (pageResource != null) {
                this.gameMapBuilder.removePage(pageResource);
                this.pageInventory.remove(new PageSlot(pageResource));
            }
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

        this.createInstance();

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
     * Returns an indication if the survivor mode is active or not.
     *
     * @return true for yes otherwise false
     */
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
