package net.onelitefeather.cygnus.creek.body;

import net.minestom.server.collision.CollisionUtils;
import net.minestom.server.collision.PhysicsResult;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.pathfinding.followers.GroundNodeFollower;
import net.minestom.server.instance.Instance;
import net.minestom.server.utils.position.PositionUtils;
import org.jetbrains.annotations.Nullable;

/**
 * Walks the creek along a path like {@link GroundNodeFollower}, but steps up low blocks the way a
 * vanilla mob does instead of jumping onto them.
 * <p>
 * Three things differ from Minestom's follower:
 * </p>
 * <ul>
 *     <li>Each step is checked a tiny bit above the feet. Standing exactly on top of a slab,
 *     Minestom counts the next slab as a wall and the entity would stop at the block edge.</li>
 *     <li>Blocks up to {@link #STEP_HEIGHT} high, like slabs and dirt paths, are walked up. It
 *     only jumps when that does not get it any farther.</li>
 *     <li>A path point counts as reached when the creek is in its column and less than a block
 *     away. The path puts the floor of a slab one block up, so on a slab the creek
 *     never stood in the same block as the point and kept jumping at it.</li>
 * </ul>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
final class StepFollower extends GroundNodeFollower {

    /** Highest block the creek walks up without jumping, in blocks. Same as a vanilla mob. */
    static final double STEP_HEIGHT = 0.6D;

    /** How far ahead a blocked step is looked for before jumping, in blocks. */
    private static final double LOOK_AHEAD = 0.3D;

    private final Entity entity;

    StepFollower(Entity entity) {
        super(entity);
        this.entity = entity;
    }

    @Override
    public void moveTowards(Point direction, double speed, Point lookAt) {
        Instance instance = this.entity.getInstance();
        if (instance == null) return;
        Pos position = this.entity.getPosition();
        double dx = direction.x() - position.x();
        double dz = direction.z() - position.z();

        // Never step past the point. Minestom slows down by the distance including height, so
        // above a point one block lower it kept overshooting and never dropped into a narrow gap.
        double step = Math.min(speed, Math.hypot(dx, dz));

        Pos moved = this.walk(instance, position, horizontal(dx, dz, step));
        Point look = lookAt.sub(position);
        float yaw = PositionUtils.getLookYaw(look.x(), look.z());
        float pitch = PositionUtils.getLookPitch(look.x(), look.y(), look.z());
        this.entity.refreshPosition(moved.withView(yaw, pitch));
    }

    @Override
    public void jump(@Nullable Point point, @Nullable Point target) {
        Instance instance = this.entity.getInstance();
        if (instance == null || point == null || !this.entity.isOnGround()) return;
        Pos position = this.entity.getPosition();
        Vec ahead = horizontal(point.x() - position.x(), point.z() - position.z(), LOOK_AHEAD);
        // Still room to walk, or a step to walk up: no jump needed.
        if (!this.blocked(instance, position.add(0, Vec.EPSILON, 0), ahead)) return;
        if (!this.blocked(instance, this.lifted(instance, position), ahead)) return;
        super.jump(point, target);
    }

    @Override
    public boolean isAtPoint(Point point) {
        Pos position = this.entity.getPosition();
        return position.blockX() == point.blockX() && position.blockZ() == point.blockZ()
                && Math.abs(position.y() - point.y()) < 1.0D;
    }

    /**
     * Moves by {@code step}, walking up a low block if the flat move runs into it.
     */
    private Pos walk(Instance instance, Pos from, Vec step) {
        PhysicsResult flat = this.sweep(instance, from.add(0, Vec.EPSILON, 0), step);
        Pos flatEnd = flat.newPosition().withY(from.y());
        if ((!flat.collisionX() && !flat.collisionZ()) || !this.entity.isOnGround()) return flatEnd;

        PhysicsResult over = this.sweep(instance, this.lifted(instance, from), step);
        if (horizontalDistance(from, over.newPosition()) <= horizontalDistance(from, flatEnd)) return flatEnd;
        return this.sweep(instance, over.newPosition(), new Vec(0, -STEP_HEIGHT, 0)).newPosition();
    }

    /**
     * Returns the position {@link #STEP_HEIGHT} higher, or lower if a ceiling is in the way.
     */
    private Pos lifted(Instance instance, Pos from) {
        return this.sweep(instance, from.add(0, Vec.EPSILON, 0), new Vec(0, STEP_HEIGHT, 0)).newPosition();
    }

    private boolean blocked(Instance instance, Pos from, Vec step) {
        PhysicsResult result = this.sweep(instance, from, step);
        return result.collisionX() || result.collisionZ();
    }

    /**
     * Moves the creek's bounding box by {@code move} from {@code from} and reports what it hits.
     */
    private PhysicsResult sweep(Instance instance, Pos from, Vec move) {
        return CollisionUtils.handlePhysics(instance, this.entity.getBoundingBox(), from, move, null, false);
    }

    private static Vec horizontal(double dx, double dz, double length) {
        double radians = Math.atan2(dz, dx);
        return new Vec(Math.cos(radians) * length, 0, Math.sin(radians) * length);
    }

    private static double horizontalDistance(Pos from, Pos to) {
        return Math.hypot(to.x() - from.x(), to.z() - from.z());
    }
}
