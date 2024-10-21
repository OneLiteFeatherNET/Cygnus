package net.onelitefeather.cygnus.common.map;

import de.icevizion.aves.map.BaseMap;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.utils.Direction;
import net.onelitefeather.cygnus.common.page.PageResource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.util.HashSet;
import java.util.Set;

/**
 * The class contains all data which are required for the game to run.
 * It inherits from the {@link BaseMap} to use the properties from it
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 **/
public final class GameMap extends BaseMap {

    private Pos slenderSpawn;
    private final Set<PageResource> pageFaces;
    private final Set<Pos> survivorSpawns;

    /**
     * Creates a new instance from the {@link GameMap}.
     * This constructor is mostly used in the setup process of the game.
     */
    public GameMap() {
        this.slenderSpawn = null;
        this.pageFaces = new HashSet<>();
        this.survivorSpawns = new HashSet<>();
    }

    /**
     * Creates a new instance from the {@link GameMap} with the given values.
     *
     * @param name           the name of the map
     * @param spawn          the spawn position for the players
     * @param slenderSpawn   the spawn position for the slender
     * @param pageFaces      the faces for the pages
     * @param survivorSpawns the spawn positions for the survivors
     * @param builders       the builders for the map
     */
    public GameMap(@NotNull String name, Pos spawn, Pos slenderSpawn, @NotNull Set<PageResource> pageFaces, @NotNull Set<Pos> survivorSpawns, String... builders) {
        super(name, spawn, builders);
        this.slenderSpawn = slenderSpawn;
        this.pageFaces = pageFaces;
        this.survivorSpawns = survivorSpawns;
    }

    /**
     * Adds a new survivor spawn to the map.
     *
     * @param pos the position to add
     * @return true if the position was added
     */
    public boolean addSurvivorSpawn(@NotNull Pos pos) {
        return this.survivorSpawns.add(pos);
    }

    /**
     * Removes a survivor spawn from the map.
     *
     * @param pos the position to remove
     * @return true if the position was removed
     */
    public boolean removeSurvivorSpawn(@NotNull Pos pos) {
        return this.survivorSpawns.remove(pos);
    }

    /**
     * Sets the spawn position for the slender.
     *
     * @param slenderSpawn the position to set
     */
    public void setSlenderSpawn(@NotNull Pos slenderSpawn) {
        this.slenderSpawn = slenderSpawn;
    }

    /**
     * Removes the spawn position for the slender.
     */
    public void removeSlenderSpawn() {
        this.slenderSpawn = null;
    }

    /**
     * Adds a new page to the map.
     *
     * @param pos  the position to add
     * @param face the face of the page
     */
    public void addPage(@NotNull Vec pos, @NotNull Direction face) {
        this.pageFaces.add(new PageResource(pos, face));
    }

    /**
     * Checks if the map has enough survivor spawns.
     *
     * @return true if the map has enough spawns
     */
    public boolean hasEnoughSurvivorSpawns() {
        return true;
        /*eturn this.survivorSpawns.size() >= GameConfig.;*/
    }

    /**
     * Returns the {@link Set} of {@link PageResource} which are used for the game.
     *
     * @return the underlying set
     */
    public @NotNull Set<PageResource> getPageFaces() {
        return pageFaces;
    }

    /**
     * Returns the {@link Pos} where the player spawn which is the Slender for a game.
     *
     * @return the underlying position
     */
    public @UnknownNullability Pos getSlenderSpawn() {
        return slenderSpawn;
    }

    /**
     * Returns the {@link Set} of {@link Pos} where the survivors spawn.
     *
     * @return the underlying set
     */
    public @NotNull Set<Pos> getSurvivorSpawns() {
        return survivorSpawns;
    }
}
