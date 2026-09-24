package net.onelitefeather.cygnus.creek.world;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Finds a floor by looking at the blocks of the round's instance.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class InstanceGround implements Ground {

    /** How many blocks above the candidate the search starts. */
    static final int SEARCH_UP = 4;

    /** How many blocks below the candidate the search goes. */
    static final int SEARCH_DOWN = 8;

    /** Free blocks the creek needs above its feet. A creaking is almost three blocks tall. */
    static final int HEADROOM = 3;

    private final Supplier<? extends @Nullable Instance> instance;

    /**
     * Creates the floor search.
     *
     * @param instance supplies the round's instance, or {@code null} while there is none
     */
    public InstanceGround(Supplier<? extends @Nullable Instance> instance) {
        this.instance = instance;
    }

    @Override
    public Optional<Pos> settle(Pos candidate) {
        Instance world = this.instance.get();
        if (world == null || !world.isChunkLoaded(candidate)) return Optional.empty();

        int x = candidate.blockX();
        int z = candidate.blockZ();
        for (int y = candidate.blockY() + SEARCH_UP; y >= candidate.blockY() - SEARCH_DOWN; y--) {
            if (world.getBlock(x, y - 1, z).solid() && hasRoom(world, x, y, z)) {
                return Optional.of(new Pos(x + 0.5D, y, z + 0.5D));
            }
        }
        return Optional.empty();
    }

    private static boolean hasRoom(Instance world, int x, int y, int z) {
        for (int offset = 0; offset < HEADROOM; offset++) {
            if (world.getBlock(x, y + offset, z).solid()) return false;
        }
        return true;
    }
}
