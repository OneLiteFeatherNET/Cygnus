package net.onelitefeather.cygnus.creek.body;

import net.minestom.server.collision.CollisionUtils;
import net.minestom.server.collision.PhysicsResult;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.pathfinding.followers.GroundNodeFollower;
import net.minestom.server.instance.Instance;
import net.minestom.server.utils.position.PositionUtils;
import org.jetbrains.annotations.Nullable;

/**
 * Moves the creek along its path like {@link GroundNodeFollower}, but steps up low blocks the
 * way a vanilla mob does instead of hopping onto them.
 * <p>
 * It differs from Minestom's follower in three ways:
 * </p>
 * <ul>
 *     <li>Each step is checked a hair above the feet. Standing right on top of a slab, Minestom
 *     treats the next slab as a wall, and the creek would get stuck at the edge.</li>
 *     <li>Anything up to its step height attribute high it simply walks up. A creaking's own is
 *     1.0625, so like in vanilla it takes whole blocks without hopping, as survivors do in a round.
 *     It only jumps when stepping up gets it nowhere.</li>
 *     <li>A path point counts as reached once the creek stands in its column and less than a block
 *     away in height. The path puts the floor of a slab a block too high, so on slabs the creek
 *     never quite arrived and kept jumping at the point.</li>
 * </ul>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
final class StepFollower extends GroundNodeFollower {

    /** How far ahead it looks for a blocked step before deciding to jump, in blocks. */
    private static final double LOOK_AHEAD = 0.3D;

    private final LivingEntity entity;

    StepFollower(LivingEntity entity) {
        super(entity);
        this.entity = entity;
    }

    /**
     * The highest step the creek takes without jumping, from its step height attribute.
     *
     * @return the step height, in blocks
     */
    double stepHeight() {
        return this.entity.getAttributeValue(Attribute.STEP_HEIGHT);
    }

    @Override
    public void moveTowards(Point direction, double speed, Point lookAt) {
        Instance instance = this.entity.getInstance();
        if (instance == null) return;
        Pos position = this.entity.getPosition();
        double dx = direction.x() - position.x();
        double dz = direction.z() - position.z();

        // Never overshoot the point. Minestom slows down using the distance including height, so
        // above a point one block lower the creek kept swinging past it and never dropped into a
        // narrow gap.
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
        // Still room to walk, or a step it can walk up: no need to jump.
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
        return this.sweep(instance, over.newPosition(), new Vec(0, -this.stepHeight(), 0)).newPosition();
    }

    /**
     * Returns the position {@link #stepHeight()} higher, or lower if a ceiling is in the way.
     */
    private Pos lifted(Instance instance, Pos from) {
        return this.sweep(instance, from.add(0, Vec.EPSILON, 0), new Vec(0, this.stepHeight(), 0)).newPosition();
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
