package net.onelitefeather.cygnus.map;

import de.icevizion.aves.map.BaseMap;
import java.util.HashSet;
import java.util.Set;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.cygnus.config.GameConfig;
import net.onelitefeather.cygnus.game.data.PageResource;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.UnknownNullability;

/**
 * The class contains all data which are required for the game to run.
 * It inherits from the {@link BaseMap} to use the properties from it
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 **/

public final class GameMap extends BaseMap {

    private Pos slenderSpawn;
    private final Set<PageResource> pageFaces;
    private final Set<Pos> survivorSpawns;

    public GameMap() {
        this.slenderSpawn = null;
        this.pageFaces = new HashSet<>();
        this.survivorSpawns = new HashSet<>();
    }

    public GameMap(@NotNull String name, Pos spawn, Pos slenderSpawn, @NotNull Set<PageResource> pageFaces, @NotNull Set<Pos> survivorSpawns, String... builders) {
        super(name, spawn, builders);
        this.slenderSpawn = slenderSpawn;
        this.pageFaces = pageFaces;
        this.survivorSpawns = survivorSpawns;
    }

    public boolean addSurvivorSpawn(@NotNull Pos pos) {
        return this.survivorSpawns.add(pos);
    }

    public void removeSurvivorSpawn(@NotNull Pos pos) {
        this.survivorSpawns.remove(pos);
    }

    public void setSlenderSpawn(@NotNull Pos slenderSpawn) {
        this.slenderSpawn = slenderSpawn;
    }

    public void addPage(@NotNull Vec pos, @NotNull String face) {
        this.pageFaces.add(new PageResource(pos, face));
    }

    public boolean hasEnoughSurvivorSpawns() {
        return this.survivorSpawns.size() < GameConfig.MAX_PLAYERS;
    }

    public @NotNull Set<PageResource> getPageFaces() {
        return pageFaces;
    }

    /**
     * Returns the {@link Pos} where the player spawn which is the Slender for a game.
     * @return the underlying position
     */
    public @UnknownNullability Pos getSlenderSpawn() {
        return slenderSpawn;
    }

    public Set<Pos> getSurvivorSpawns() {
        return survivorSpawns;
    }
}
