package net.onelitefeather.cygnus.common.map;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.utils.Direction;
import net.onelitefeather.cygnus.common.page.PageResource;
import net.theevilreaper.aves.map.BaseMapBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public final class GameMapBuilder extends BaseMapBuilder {

    private @Nullable Pos slenderSpawn;
    private final Set<PageResource> pageFaces;
    private final Set<Pos> survivorSpawns;

    /**
     * Creates a new instance of the builder class
     */
    public GameMapBuilder() {
        super();
        this.pageFaces = new HashSet<>();
        this.survivorSpawns = new HashSet<>();
    }

    /**
     * Creates a new instance of the builder with the provided {@link GameMap} instance to get the data from it.
     *
     * @param gameMap to get the existing data
     */
    public GameMapBuilder(GameMap gameMap) {
        super(gameMap);
        this.slenderSpawn = gameMap.getSlenderSpawn();
        this.survivorSpawns = gameMap.getSurvivorSpawns();
        this.pageFaces = gameMap.getPageFaces();
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
        return new GameMap(name, spawn, slenderSpawn, pageFaces, survivorSpawns, builders.toArray(new String[0]));
    }

    /**
     * Adds a new page to the map.
     *
     * @param pos  the position to add
     * @param face the face of the page
     */
    public void addPage(Vec pos, Direction face) {
        this.pageFaces.add(new PageResource(pos, face));
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
}
