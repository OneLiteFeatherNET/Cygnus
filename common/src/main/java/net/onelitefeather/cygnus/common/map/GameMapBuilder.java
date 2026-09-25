package net.onelitefeather.cygnus.common.map;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.utils.Direction;
import net.onelitefeather.cygnus.common.creek.CreekRoute;
import net.onelitefeather.cygnus.common.dimension.MapAtmosphere;
import net.onelitefeather.cygnus.common.page.PageResource;
import net.theevilreaper.aves.map.BaseMapBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class GameMapBuilder extends BaseMapBuilder {

    private @Nullable Pos slenderSpawn;
    private @Nullable MapAtmosphere atmosphere;
    private final Set<PageResource> pageFaces;
    private final Set<Pos> survivorSpawns;
    private final Map<String, List<Vec>> creekRoutes;

    /**
     * Creates a new instance of the builder class
     */
    public GameMapBuilder() {
        super();
        this.pageFaces = new HashSet<>();
        this.survivorSpawns = new TreeSet<>(
                Comparator.comparingInt(Pos::blockX)
                        .thenComparingInt(Pos::blockY)
                        .thenComparingInt(Pos::blockZ)
        );
        this.creekRoutes = new LinkedHashMap<>();
    }

    /**
     * Creates a new instance of the builder with the provided {@link GameMap} instance to get the data from it.
     *
     * @param gameMap to get the existing data
     */
    public GameMapBuilder(GameMap gameMap) {
        super(gameMap);
        this.slenderSpawn = gameMap.getSlenderSpawn();
        this.survivorSpawns = new TreeSet<>(
                Comparator.comparingInt(Pos::blockX)
                        .thenComparingInt(Pos::blockY)
                        .thenComparingInt(Pos::blockZ)
        );
        this.survivorSpawns.addAll(gameMap.getSurvivorSpawns());
        this.pageFaces = new HashSet<>(gameMap.getPageFaces());
        this.atmosphere = gameMap.getAtmosphere();
        this.creekRoutes = new LinkedHashMap<>();
        gameMap.getCreekRoutes().forEach(route -> this.creekRoutes.put(route.name(), new ArrayList<>(route.points())));
    }

    /**
     * Adds a new survivor spawn to the map.
     *
     * @param pos the position to add
     * @return true if the position was added
     */
    public boolean addSurvivorSpawn(Pos pos) {
        return this.survivorSpawns.add(pos);
    }

    /**
     * Removes a survivor spawn from the map.
     *
     * @param pos the position to remove
     * @return true if the position was removed
     */
    public boolean removeSurvivorSpawn(Pos pos) {
        return this.survivorSpawns.remove(pos);
    }

    /**
     * Sets the spawn position for the slender.
     *
     * @param slenderSpawn the position to set
     */
    public void setSlenderSpawn(@Nullable Pos slenderSpawn) {
        if (slenderSpawn == null) {
            this.slenderSpawn = null;
            return;
        }
        this.slenderSpawn = slenderSpawn;
    }

    /**
     * Creates a new instance of the {@link GameMap} with the provided data.
     *
     * @return the created instance
     */
    @Override
    public GameMap build() {
        return new GameMap(name, spawn, slenderSpawn, pageFaces, survivorSpawns, builders, atmosphere, getCreekRoutes());
    }

    /**
     * Adds a new page to the map.
     *
     * @param pos  the position to add
     * @param face the face of the page
     * @return true if the page was added, false if a page at the position and face already exists
     */
    public boolean addPage(Vec pos, Direction face) {
        return this.pageFaces.add(new PageResource(pos, face));
    }

    /**
     * Removes a page from the map.
     *
     * @param pageResource the page resource to remove
     * @return true if the page was removed
     */
    public boolean removePage(PageResource pageResource) {
        return this.pageFaces.remove(pageResource);
    }

    /**
     * Returns the {@link Set} of {@link PageResource} which are used for the game.
     *
     * @return the underlying set
     */
    public Set<PageResource> getPageFaces() {
        return pageFaces;
    }

    /**
     * Checks if the map has enough survivor spawns.
     *
     * @return true if the map has enough spawns
     */
    public boolean hasEnoughSurvivorSpawns() {
        return true;
    }

    /**
     * Returns the {@link Pos} where the player spawn which is the Slender for a game.
     *
     * @return the underlying position
     */
    public @Nullable Pos getSlenderSpawn() {
        return slenderSpawn;
    }

    /**
     * Returns the {@link Set} of {@link Pos} where the survivors spawn.
     *
     * @return the underlying set
     */
    public Set<Pos> getSurvivorSpawns() {
        return survivorSpawns;
    }

    /**
     * Sets the atmosphere the map should be rendered with.
     *
     * @param atmosphere the atmosphere to set, or {@code null} to fall back to the overworld look
     */
    public void setAtmosphere(@Nullable MapAtmosphere atmosphere) {
        this.atmosphere = atmosphere;
    }

    /**
     * Returns the atmosphere currently configured for the map.
     *
     * @return the atmosphere, or {@code null} if none is set
     */
    public @Nullable MapAtmosphere getAtmosphere() {
        return atmosphere;
    }

    /**
     * Adds an empty creek route.
     *
     * @param name the route's name
     * @return {@code false} if the name is empty or already taken
     */
    public boolean addCreekRoute(String name) {
        String trimmed = name.trim();
        if (trimmed.isEmpty() || this.creekRoutes.containsKey(trimmed)) return false;
        this.creekRoutes.put(trimmed, new ArrayList<>());
        return true;
    }

    /**
     * Removes a creek route.
     *
     * @param name the route's name
     * @return {@code false} if there was no such route
     */
    public boolean removeCreekRoute(String name) {
        return this.creekRoutes.remove(name) != null;
    }

    /**
     * Returns whether a creek route with this name exists.
     *
     * @param name the route's name
     * @return {@code true} if it exists
     */
    public boolean hasCreekRoute(String name) {
        return this.creekRoutes.containsKey(name);
    }

    /**
     * Appends a point to a creek route.
     *
     * @param name  the route's name
     * @param point the new last point
     * @return {@code false} if there was no such route
     */
    public boolean addCreekPoint(String name, Vec point) {
        List<Vec> points = this.creekRoutes.get(name);
        if (points == null) return false;
        points.add(point);
        return true;
    }

    /**
     * Removes the last point of a creek route.
     *
     * @param name the route's name
     * @return {@code false} if there was no such route or it had no points
     */
    public boolean removeLastCreekPoint(String name) {
        List<Vec> points = this.creekRoutes.get(name);
        if (points == null || points.isEmpty()) return false;
        points.removeLast();
        return true;
    }

    /**
     * Returns the points of a creek route.
     *
     * @param name the route's name
     * @return the points, empty if there is no such route
     */
    public List<Vec> getCreekRoutePoints(String name) {
        return List.copyOf(this.creekRoutes.getOrDefault(name, List.of()));
    }

    /**
     * Returns all creek routes in the order they were added.
     *
     * @return the routes
     */
    public List<CreekRoute> getCreekRoutes() {
        return this.creekRoutes.entrySet().stream()
                .map(entry -> new CreekRoute(entry.getKey(), entry.getValue()))
                .toList();
    }
}
