package net.onelitefeather.cygnus.footprint;

import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;

import java.util.Optional;

/**
 * Decides whether a print may lie on a spot: on top of a full block, in an air block.
 * <p>
 * The print is a fake block that replaces whatever is in its block on the client. So that block
 * has to be air: carpets, snow layers, grass and flowers take no print. The block below has to be
 * full, slabs and stairs would be swallowed by the fake block.
 * </p>
 * <p>
 * The search starts at the block the feet are in or on and goes down a little. A recorded point
 * can be taken mid-jump, and the print belongs on the ground below it.
 * </p>
 *
 * @author Joltra
 * @version 1.0.0
 * @since 2.16.0
 */
final class FootprintGround {

    /** How many blocks below the feet the ground may be. */
    static final int SEARCH_DEPTH = 2;

    /** Lifts the feet a little, so a player a hair below the surface still stands on it. */
    static final double FEET_EPSILON = 0.01D;

    /**
     * Finds the block a print goes into.
     *
     * @param instance the instance to look in
     * @param position where the player stood
     * @return the air block above a full block, or empty if no print may lie there
     */
    static Optional<BlockVec> spot(Instance instance, Point position) {
        if (!instance.isChunkLoaded(position)) return Optional.empty();
        int x = position.blockX();
        int z = position.blockZ();
        int start = (int) Math.floor(position.y() + FEET_EPSILON);
        for (int y = start; y >= start - SEARCH_DEPTH; y--) {
            Block below = instance.getBlock(x, y - 1, z);
            if (below.air()) continue;
            // The fake block replaces whatever is here on the client, so it has to be air.
            if (!instance.getBlock(x, y, z).air()) return Optional.empty();
            if (!below.collisionShape().isFaceFull(BlockFace.TOP)) return Optional.empty();
            return Optional.of(new BlockVec(x, y, z));
        }
        return Optional.empty();
    }

    private FootprintGround() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}
