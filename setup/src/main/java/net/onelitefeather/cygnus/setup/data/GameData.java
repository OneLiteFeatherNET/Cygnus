package net.onelitefeather.cygnus.setup.data;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.utils.Direction;
import net.onelitefeather.cygnus.common.config.CreekConfig;
import net.onelitefeather.cygnus.common.creek.CreekRoute;
import net.onelitefeather.cygnus.common.creek.CreekRoutesFile;
import net.onelitefeather.cygnus.common.creek.CreekWaypoint;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.common.map.GameMapBuilder;
import net.onelitefeather.cygnus.common.util.GsonHelper;
import net.onelitefeather.cygnus.setup.creek.CreekRoutePreview;
import net.onelitefeather.cygnus.setup.event.dialog.DialogContext;
import net.onelitefeather.cygnus.setup.event.dialog.DialogRequestEvent;
import net.onelitefeather.cygnus.setup.event.dialog.DialogTarget;
import net.onelitefeather.cygnus.setup.inventory.page.PageHeaderFormatter;
import net.onelitefeather.cygnus.setup.inventory.slot.CreekRouteSlot;
import net.onelitefeather.cygnus.setup.inventory.slot.PageSlot;
import net.onelitefeather.cygnus.setup.inventory.view.InventoryMode;
import net.onelitefeather.cygnus.setup.inventory.view.MapDataOverviewInventory;
import net.onelitefeather.cygnus.setup.inventory.view.SurvivorViewInventory;
import net.onelitefeather.cygnus.setup.item.SetupItemId;
import net.onelitefeather.cygnus.setup.item.SetupItems;
import net.onelitefeather.cygnus.setup.map.MapDataCategory;
import net.onelitefeather.cygnus.common.page.PageFacesFile;
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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GameData extends InstanceSetupData {

    private final MapDataOverviewInventory inventory;
    private final SurvivorViewInventory survivorInventory;
    private final PageableInventory pageInventory;
    private final PageableInventory creekRouteInventory;
    private final List<CreekRouteSlot> creekRouteSlots;
    private final CreekRoutePreview creekRoutePreview;
    private GameMapBuilder gameMapBuilder;
    private boolean pageMode;
    private boolean survivorMode;
    private boolean creekRouteMode;
    private @Nullable String activeCreekRoute;

    /**
     * Constructs a new GameData instance.
     *
     * @param player   who owns the data object
     * @param mapEntry the map entry associated with this game data
     */
    public GameData(Player player, MapEntry mapEntry) {
        super(player.getUuid(), mapEntry, BossBar.Color.RED);
        this.loadData();
        this.creekRouteSlots = new ArrayList<>();
        this.inventory = new MapDataOverviewInventory(player, this.gameMapBuilder, InventoryMode.GAME);
        this.survivorInventory = new SurvivorViewInventory(player, this.gameMapBuilder);
        this.pageInventory = createPageInventory(player);
        this.creekRouteInventory = createCreekRouteInventory(player);
        this.creekRoutePreview = new CreekRoutePreview(player, () -> this.gameMapBuilder.getCreekRoutes(),
                () -> this.activeCreekRoute, CreekConfig.DEFAULT.routeLinkDistance());
        this.refreshCreekRoutes();
    }

    /**
     * Creates the {@link PageableInventory} to display the creek routes of the map.
     *
     * @param player who owns the inventory
     * @return the created inventory
     */
    private PageableInventory createCreekRouteInventory(Player player) {
        InventoryLayout layout = InventoryLayout.fromType(InventoryType.CHEST_6_ROW);
        layout.setItems(LayoutCalculator.fillRow(InventoryType.CHEST_1_ROW), SetupItems.DECORATION_PANE);
        layout.setItems(LayoutCalculator.fillRow(InventoryType.CHEST_6_ROW), SetupItems.DECORATION_PANE);
        return PageableInventory.builder()
                .titleData(TitleData.builder()
                        .title(Component.text("Creek routes - "))
                        .pageMapper(PageHeaderFormatter::format)
                        .showPageNumbers(true)
                        .build())
                .player(player)
                .slotRange(LayoutCalculator.quad(InventoryType.CHEST_1_ROW.getSize(), InventoryType.CHEST_5_ROW.getSize() - 1))
                .layout(layout)
                .values(new ArrayList<>())
                .build();
    }

    /**
     * Rebuilds the route overview after routes or points changed.
     */
    private void refreshCreekRoutes() {
        this.creekRouteSlots.forEach(this.creekRouteInventory::remove);
        this.creekRouteSlots.clear();
        for (CreekRoute route : this.gameMapBuilder.getCreekRoutes()) {
            CreekRouteSlot slot = new CreekRouteSlot(route, route.name().equals(this.activeCreekRoute),
                    this::selectFromOverview, this::deleteFromOverview);
            this.creekRouteSlots.add(slot);
            this.creekRouteInventory.add(slot);
        }
        if (this.hasCreekRouteMode()) {
            this.creekRoutePreview.refreshLabels();
        }
    }

    private void selectFromOverview(Player player, String name) {
        if (this.selectCreekRoute(name)) {
            player.sendMessage(SetupMessages.getCreekRouteSelected(name));
            player.sendMessage(SetupMessages.CREEK_POINT_HINT);
            SetupItems.setCreekRouteEditItems(player);
        }
    }

    private void deleteFromOverview(Player player, String name) {
        if (this.deleteCreekRoute(name)) {
            player.sendMessage(SetupMessages.getCreekRouteDeleted(name));
        }
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
     * @return true if the page was added, false if a page at the position and face already exists
     */
    public boolean addPage(Vec pos, Direction face) {
        PageResource pageResource = new PageResource(pos, face);
        boolean added = this.gameMapBuilder.addPage(pos, face);
        if (added) {
            this.pageInventory.add(new PageSlot(pageResource));
        }
        return added;
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
     * Returns whether the creek route mode is on.
     *
     * @return {@code true} while routes are being edited
     */
    public boolean hasCreekRouteMode() {
        return this.creekRouteMode;
    }

    /**
     * Turns the creek route mode on or off.
     */
    public void swapCreekRouteMode() {
        this.creekRouteMode = !this.creekRouteMode;
    }

    /**
     * Creates a creek route and makes it the active one.
     *
     * @param name the route's name
     * @return {@code false} if the name is empty or taken
     */
    public boolean createCreekRoute(String name) {
        String trimmed = name.trim();
        if (!this.gameMapBuilder.addCreekRoute(trimmed)) return false;
        this.activeCreekRoute = trimmed;
        this.refreshCreekRoutes();
        return true;
    }

    /**
     * Makes a creek route the active one.
     *
     * @param name the route's name
     * @return {@code false} if there is no such route
     */
    public boolean selectCreekRoute(String name) {
        if (!this.gameMapBuilder.hasCreekRoute(name)) return false;
        this.activeCreekRoute = name;
        this.refreshCreekRoutes();
        return true;
    }

    /**
     * Deletes a creek route.
     *
     * @param name the route's name
     * @return {@code false} if there was no such route
     */
    public boolean deleteCreekRoute(String name) {
        if (!this.gameMapBuilder.removeCreekRoute(name)) return false;
        if (name.equals(this.activeCreekRoute)) this.activeCreekRoute = null;
        this.refreshCreekRoutes();
        return true;
    }

    /**
     * Appends a point to the active creek route.
     *
     * @param point where the creek's feet stand
     * @return {@code false} without an active route
     */
    public boolean addCreekPoint(Vec point) {
        String activeRoute = this.activeCreekRoute;
        if (activeRoute == null || !this.gameMapBuilder.addCreekPoint(activeRoute, point)) return false;
        this.refreshCreekRoutes();
        return true;
    }

    /**
     * Removes the point of the active creek route that sits at the given position.
     *
     * @param point the position of the point to remove
     * @return the number of the removed point, counting from 1, or {@code -1} without an active
     * route or without a point there
     */
    public int removeCreekPointAt(Vec point) {
        String activeRoute = this.activeCreekRoute;
        if (activeRoute == null) return -1;
        int removed = this.gameMapBuilder.removeCreekPoint(activeRoute, point);
        if (removed > 0) this.refreshCreekRoutes();
        return removed;
    }

    /**
     * Removes the last point of the active creek route.
     *
     * @return {@code false} without an active route or without points
     */
    public boolean removeLastCreekPoint() {
        String activeRoute = this.activeCreekRoute;
        if (activeRoute == null || !this.gameMapBuilder.removeLastCreekPoint(activeRoute)) return false;
        this.refreshCreekRoutes();
        return true;
    }

    /**
     * Sets how long the creek waits at the start of the active route.
     *
     * @param millis the pause, in milliseconds
     * @return {@code false} without an active route or without a point
     */
    public boolean setCreekStartPause(int millis) {
        return this.setCreekPause(true, millis);
    }

    /**
     * Sets how long the creek waits at the end of the active route.
     *
     * @param millis the pause, in milliseconds
     * @return {@code false} without an active route or with fewer than two points
     */
    public boolean setCreekEndPause(int millis) {
        return this.setCreekPause(false, millis);
    }

    private boolean setCreekPause(boolean atStart, int millis) {
        String activeRoute = this.activeCreekRoute;
        if (activeRoute == null || !this.gameMapBuilder.setCreekEndPause(activeRoute, atStart, millis)) return false;
        this.refreshCreekRoutes();
        return true;
    }

    /**
     * Stops editing the active creek route. The route itself stays.
     *
     * @return {@code false} if no route was being edited
     */
    public boolean finishCreekRoute() {
        if (this.activeCreekRoute == null) return false;
        this.activeCreekRoute = null;
        this.refreshCreekRoutes();
        return true;
    }

    /**
     * Returns the name of the creek route being edited.
     *
     * @return the name, or {@code null} if none is active
     */
    public @Nullable String activeCreekRoute() {
        return this.activeCreekRoute;
    }

    /**
     * Returns how many points the active creek route has.
     *
     * @return the number of points, {@code 0} without an active route
     */
    public int activeCreekPointCount() {
        String activeRoute = this.activeCreekRoute;
        return activeRoute == null ? 0 : this.gameMapBuilder.getCreekRoutePoints(activeRoute).size();
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

        if (SetupItemId.CREEK_ROUTES == tagValue) {
            if (!hasCreekRouteMode() && (hasPageMode() || hasSurvivorMode())) {
                player.sendMessage(SetupMessages.getModeInform(hasPageMode() ? "page" : "survivor"));
                return;
            }
            this.swapCreekRouteMode();
            if (hasCreekRouteMode()) {
                player.sendMessage(SetupMessages.CREEK_MODE_ENABLED);
                player.sendMessage(SetupMessages.getModeInform("creek route"));
                SetupItems.setCreekRouteOverviewItems(player);
                this.creekRoutePreview.show();
            } else {
                this.finishCreekRoute();
                this.creekRoutePreview.hide();
                player.sendMessage(SetupMessages.CREEK_MODE_DISABLED);
                SetupItems.setGameLayout(player);
            }
            return;
        }
        if (SetupItemId.CREEK_LEAVE == tagValue) {
            if (hasCreekRouteMode()) {
                this.swapCreekRouteMode();
            }
            this.finishCreekRoute();
            this.creekRoutePreview.hide();
            player.sendMessage(SetupMessages.CREEK_MODE_DISABLED);
            SetupItems.setGameLayout(player);
            return;
        }
        if (SetupItemId.CREEK_NEW == tagValue) {
            EventDispatcher.call(new DialogRequestEvent(player, DialogTarget.CREEK_ROUTE_NAME));
            return;
        }
        if (SetupItemId.CREEK_UNDO == tagValue) {
            String activeRoute = this.activeCreekRoute;
            if (activeRoute == null) {
                player.sendMessage(SetupMessages.NO_ACTIVE_CREEK_ROUTE);
            } else if (!this.removeLastCreekPoint()) {
                player.sendMessage(SetupMessages.NO_CREEK_POINT_TO_REMOVE);
            } else {
                player.sendMessage(SetupMessages.getCreekPointRemoved(activeRoute, this.activeCreekPointCount()));
            }
            return;
        }
        if (SetupItemId.CREEK_LIST == tagValue) {
            this.creekRouteInventory.open();
            return;
        }
        if (SetupItemId.CREEK_FINISH == tagValue) {
            String activeRoute = this.activeCreekRoute;
            int pointCount = this.activeCreekPointCount();
            if (activeRoute != null && this.finishCreekRoute()) {
                player.sendMessage(SetupMessages.getCreekRouteFinished(activeRoute, pointCount));
                if (pointCount < CreekRoute.MIN_POINTS) {
                    player.sendMessage(SetupMessages.CREEK_ROUTE_NOT_USABLE);
                }
            }
            SetupItems.setCreekRouteOverviewItems(player);
            return;
        }
        if (SetupItemId.CREEK_PAUSE == tagValue) {
            String activeRoute = this.activeCreekRoute;
            if (activeRoute == null) {
                player.sendMessage(SetupMessages.NO_ACTIVE_CREEK_ROUTE);
                return;
            }
            List<CreekWaypoint> points = this.gameMapBuilder.getCreekRoutePoints(activeRoute);
            int startMillis = points.isEmpty() ? 0 : points.getFirst().pauseMillis();
            int endMillis = points.size() < CreekRoute.MIN_POINTS ? 0 : points.getLast().pauseMillis();
            EventDispatcher.call(new DialogRequestEvent(player, DialogTarget.CREEK_ROUTE_PAUSE,
                    new DialogContext.CreekPauseContext(activeRoute, startMillis, endMillis)));
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
            case ATMOSPHERE -> gameMapBuilder.setAtmosphere(null);
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
        GameMap map = this.gameMapBuilder.build();
        PageFacesFile.save(mapEntry.getMapFile(), map.getPageFaces());
        CreekRoutesFile.save(mapEntry.getMapFile(), map.getCreekRoutes());
        GsonHelper.FILE_HANDLER.save(mapEntry.getMapFile(), map);
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
        this.creekRouteInventory.unregister();
        this.creekRoutePreview.hide();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadData() {
        Optional<GameMap> mapData =
                this.mapEntry.hasMapFile()
                        ? GsonHelper.FILE_HANDLER.load(mapEntry.getMapFile(), GameMap.class)
                                .map(map -> PageFacesFile.loadInto(mapEntry.getMapFile(), map))
                                .map(map -> map.withCreekRoutes(CreekRoutesFile.loadUnchecked(mapEntry.getMapFile())))
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
