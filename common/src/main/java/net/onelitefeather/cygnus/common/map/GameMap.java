package net.onelitefeather.cygnus.common.map;

import net.onelitefeather.cygnus.common.dimension.MapAtmosphere;
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
    private final Set<PageResource> pageFaces;
    private final Set<Pos> survivorSpawns;
    private final @Nullable MapAtmosphere atmosphere;

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
        super(name, spawn, builders);
        this.slenderSpawn = slenderSpawn;
        this.pageFaces = Set.copyOf(pageFaces);
        this.survivorSpawns = Set.copyOf(survivorSpawns);
        this.atmosphere = atmosphere;
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
}
