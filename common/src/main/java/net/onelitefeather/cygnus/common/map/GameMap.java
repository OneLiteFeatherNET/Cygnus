package net.onelitefeather.cygnus.common.map;

import net.onelitefeather.cygnus.common.creek.CreekRoute;
import net.onelitefeather.cygnus.common.dimension.MapAtmosphere;
import net.onelitefeather.cygnus.common.map.settings.CreekSettings;
import net.onelitefeather.cygnus.common.map.settings.PageSettings;
import net.theevilreaper.aves.map.BaseMap;
import net.minestom.server.coordinate.Pos;
import net.onelitefeather.cygnus.common.page.PageResource;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * The class contains all data that are required for the game to run.
 * It inherits from the {@link BaseMap} to use the properties from it
 *
 * @author theEvilReaper
 * @version 1.2.0
 * @since 1.0.0
 **/
public final class GameMap extends BaseMap {

    private final Pos slenderSpawn;
    // transient: the pages live in the pools of the sibling pages folder (see PagePoolFiles), the game
    // puts the drawn pool in here. GameMapAdapter ignores pages an old map.json still inlines.
    private final transient Set<PageResource> pageFaces;
    private final Set<Pos> survivorSpawns;
    private final @Nullable MapAtmosphere atmosphere;
    // transient like pageFaces: the routes live in a sibling creek.json (see CreekRoutesFile)
    private final transient List<CreekRoute> creekRoutes;
    // transient like pageFaces: the page settings live in pages.json (see PageSettingsFile), the creek settings in creek.json
    private final transient PageSettings pageSettings;
    private final transient CreekSettings creekSettings;

    /**
     * Creates a new instance from the {@link GameMap} with the given values.
     *
     * @param name           the name of the map
     * @param spawn          the spawn position for the players
     * @param slenderSpawn   the spawn position for the slender
     * @param pageFaces      the faces for the pages
     * @param survivorSpawns the spawn positions for the survivors
     * @param builders       the builders for the map
     * @param atmosphere     the fog and sky look of the map, or {@code null} to leave it on the overworld default
     */
    public GameMap(
            String name,
            Pos spawn,
            Pos slenderSpawn,
            Set<PageResource> pageFaces,
            Set<Pos> survivorSpawns,
            List<String> builders,
            @Nullable MapAtmosphere atmosphere
    ) {
        this(name, spawn, slenderSpawn, pageFaces, survivorSpawns, builders, atmosphere, List.of());
    }

    /**
     * Creates a map that also carries creek routes.
     *
     * @param name           the name of the map
     * @param spawn          the spawn position for the players
     * @param slenderSpawn   the spawn position for the slender
     * @param pageFaces      the faces for the pages
     * @param survivorSpawns the spawn positions for the survivors
     * @param builders       the builders for the map
     * @param atmosphere     the fog and sky look of the map, or {@code null} for the overworld default
     * @param creekRoutes    the routes the creek walks along
     */
    public GameMap(
            String name,
            Pos spawn,
            Pos slenderSpawn,
            Set<PageResource> pageFaces,
            Set<Pos> survivorSpawns,
            List<String> builders,
            @Nullable MapAtmosphere atmosphere,
            List<CreekRoute> creekRoutes
    ) {
        this(name, spawn, slenderSpawn, pageFaces, survivorSpawns, builders, atmosphere, creekRoutes,
                PageSettings.DEFAULT, CreekSettings.DEFAULT);
    }

    /**
     * Creates a map that also carries the values it sets for its pages and its creek.
     *
     * @param name           the name of the map
     * @param spawn          the spawn position for the players
     * @param slenderSpawn   the spawn position for the slender
     * @param pageFaces      the faces for the pages
     * @param survivorSpawns the spawn positions for the survivors
     * @param builders       the builders for the map
     * @param atmosphere     the fog and sky look of the map, or {@code null} for the overworld default
     * @param creekRoutes    the routes the creek walks along
     * @param pageSettings   the page values the map sets for itself
     * @param creekSettings  the creek values the map sets for itself
     */
    public GameMap(
            String name,
            Pos spawn,
            Pos slenderSpawn,
            Set<PageResource> pageFaces,
            Set<Pos> survivorSpawns,
            List<String> builders,
            @Nullable MapAtmosphere atmosphere,
            List<CreekRoute> creekRoutes,
            PageSettings pageSettings,
            CreekSettings creekSettings
    ) {
        super(name, spawn, builders);
        this.slenderSpawn = slenderSpawn;
        this.pageFaces = Set.copyOf(pageFaces);
        this.survivorSpawns = Set.copyOf(survivorSpawns);
        this.atmosphere = atmosphere;
        this.creekRoutes = List.copyOf(creekRoutes);
        this.pageSettings = pageSettings;
        this.creekSettings = creekSettings;
    }

    /**
     * Returns the {@link Pos} where the player spawn which is the Slender for a game.
     *
     * @return the underlying position
     */
    public Pos getSlenderSpawn() {
        return slenderSpawn;
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
     * Returns the {@link Set} of {@link Pos} where the survivors spawn.
     *
     * @return the underlying set
     */
    public Set<Pos> getSurvivorSpawns() {
        return survivorSpawns;
    }

    /**
     * Returns the {@link MapAtmosphere} that gives this map its fog and sky.
     *
     * <p>A map without an atmosphere runs on {@link net.minestom.server.world.DimensionType#OVERWORLD},
     * which is what every map did before atmospheres existed.</p>
     *
     * @return the atmosphere, or {@code null} if the map file declares none
     */
    public @Nullable MapAtmosphere getAtmosphere() {
        return atmosphere;
    }

    /**
     * Returns the routes the creek walks along.
     *
     * @return the routes, empty if the map has none
     */
    public List<CreekRoute> getCreekRoutes() {
        return creekRoutes;
    }

    /**
     * Returns a copy of this map with other page faces.
     *
     * @param pageFaces the new page faces
     * @return the copy
     */
    public GameMap withPageFaces(Set<PageResource> pageFaces) {
        return new GameMap(name(), spawn(), slenderSpawn, pageFaces, survivorSpawns, builders(), atmosphere, creekRoutes,
                pageSettings, creekSettings);
    }

    /**
     * Returns a copy of this map with other creek routes.
     *
     * @param creekRoutes the new routes
     * @return the copy
     */
    public GameMap withCreekRoutes(List<CreekRoute> creekRoutes) {
        return new GameMap(name(), spawn(), slenderSpawn, pageFaces, survivorSpawns, builders(), atmosphere, creekRoutes,
                pageSettings, creekSettings);
    }

    /**
     * Returns the page values this map sets for itself.
     *
     * @return the settings, {@link PageSettings#DEFAULT} if the map sets none
     */
    public PageSettings getPageSettings() {
        return pageSettings;
    }

    /**
     * Returns the creek values this map sets for itself.
     *
     * @return the settings, {@link CreekSettings#DEFAULT} if the map sets none
     */
    public CreekSettings getCreekSettings() {
        return creekSettings;
    }

    /**
     * Returns a copy of this map with other page settings.
     *
     * @param pageSettings the new settings
     * @return the copy
     */
    public GameMap withPageSettings(PageSettings pageSettings) {
        return new GameMap(name(), spawn(), slenderSpawn, pageFaces, survivorSpawns, builders(), atmosphere,
                creekRoutes, pageSettings, creekSettings);
    }

    /**
     * Returns a copy of this map with other creek settings.
     *
     * @param creekSettings the new settings
     * @return the copy
     */
    public GameMap withCreekSettings(CreekSettings creekSettings) {
        return new GameMap(name(), spawn(), slenderSpawn, pageFaces, survivorSpawns, builders(), atmosphere,
                creekRoutes, pageSettings, creekSettings);
    }
}
