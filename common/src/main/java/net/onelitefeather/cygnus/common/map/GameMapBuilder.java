package net.onelitefeather.cygnus.common.map;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.utils.Direction;
import net.onelitefeather.cygnus.common.creek.CreekRoute;
import net.onelitefeather.cygnus.common.creek.CreekWaypoint;
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
    private final Map<String, List<CreekWaypoint>> creekRoutes;

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
     * Appends a point to a creek route. Once the route has an end, its pause moves to the new end.
     *
     * @param name  the route's name
     * @param point the new last point
     * @return {@code false} if there was no such route
     */
    public boolean addCreekPoint(String name, Vec point) {
        List<CreekWaypoint> points = this.creekRoutes.get(name);
        if (points == null) return false;
        CreekWaypoint added = CreekWaypoint.of(point);
        if (points.size() >= CreekRoute.MIN_POINTS) {
            CreekWaypoint oldEnd = points.getLast();
            points.set(points.size() - 1, oldEnd.withPause(0));
            added = added.withPause(oldEnd.pauseMillis());
        }
        points.add(added);
        return true;
    }

    /**
     * Removes the last point of a creek route. If the route still has an end, the pause moves to it.
     *
     * @param name the route's name
     * @return {@code false} if there was no such route or it had no points
     */
    public boolean removeLastCreekPoint(String name) {
        List<CreekWaypoint> points = this.creekRoutes.get(name);
        if (points == null || points.isEmpty()) return false;
        CreekWaypoint removed = points.removeLast();
        if (points.size() >= CreekRoute.MIN_POINTS) {
            int lastIndex = points.size() - 1;
            points.set(lastIndex, points.get(lastIndex).withPause(removed.pauseMillis()));
        }
        return true;
    }

    /**
     * Removes the point of a creek route that sits at the given position. If several do, the one
     * added last goes. The pauses of the start and the end stay with the new start and end.
     *
     * @param name  the route's name
     * @param point the position of the point to remove
     * @return the number of the removed point, counting from 1, or {@code -1} if there was none
     */
    public int removeCreekPoint(String name, Vec point) {
        List<CreekWaypoint> points = this.creekRoutes.get(name);
        if (points == null) return -1;
        int index = -1;
        for (int i = points.size() - 1; i >= 0; i--) {
            if (points.get(i).position().equals(point)) {
                index = i;
                break;
            }
        }
        if (index < 0) return -1;
        if (index == points.size() - 1) {
            this.removeLastCreekPoint(name);
            return index + 1;
        }
        CreekWaypoint removed = points.remove(index);
        if (index == 0) {
            points.set(0, points.getFirst().withPause(removed.pauseMillis()));
        }
        return index + 1;
    }

    /**
     * Sets how long the creek waits at the start or the end of a route.
     *
     * @param name    the route's name
     * @param atStart {@code true} for the first point, {@code false} for the last
     * @param millis  the pause, in milliseconds
     * @return {@code false} without such a route, with a negative pause, without a point at the
     * start, or with fewer than two points for the end
     */
    public boolean setCreekEndPause(String name, boolean atStart, int millis) {
        List<CreekWaypoint> points = this.creekRoutes.get(name);
        if (points == null || millis < 0) return false;
        if (atStart ? points.isEmpty() : points.size() < CreekRoute.MIN_POINTS) return false;
        int index = atStart ? 0 : points.size() - 1;
        points.set(index, points.get(index).withPause(millis));
        return true;
    }

    /**
     * Returns the points of a creek route.
     *
     * @param name the route's name
     * @return the points, empty if there is no such route
     */
    public List<CreekWaypoint> getCreekRoutePoints(String name) {
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
