package net.onelitefeather.cygnus.creek.body;

import net.minestom.server.collision.BoundingBox;
import net.minestom.server.collision.CollisionUtils;
import net.minestom.server.collision.PhysicsResult;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.pathfinding.generators.GroundNodeGenerator;
import net.minestom.server.instance.block.Block;

/**
 * Plans paths like {@link GroundNodeGenerator}, but also through grass, ferns and leaf litter.
 * <p>
 * Minestom only lets an entity walk into a block that is air. Any plant on the ground made that
 * spot unwalkable, so the creek took detours, jumped or did not move at all. Here only solid
 * blocks and liquids stop a step.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
final class FoliageGroundGenerator extends GroundNodeGenerator {

    @Override
    public boolean canMoveTowards(Block.Getter getter, Point start, Point end, BoundingBox boundingBox) {
        Block target = getter.getBlock(end);
        if (target.solid() || target.liquid()) return false;
        PhysicsResult result = sweep(getter, boundingBox, start, end);
        return !result.collisionX() && !result.collisionY() && !result.collisionZ();
    }

    /**
     * Moves the creek's bounding box from {@code start} to {@code end} and reports what it hits.
     *
     * @param getter      the blocks of the world
     * @param boundingBox the creek's bounding box
     * @param start       where the move starts
     * @param end         where the move ends
     * @return the result of the move
     */
    private static PhysicsResult sweep(Block.Getter getter, BoundingBox boundingBox, Point start, Point end) {
        return CollisionUtils.handlePhysics(getter, boundingBox, start.asPos(), end.sub(start).asVec(), null, false);
    }
}
