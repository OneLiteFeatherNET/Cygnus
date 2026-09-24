package net.onelitefeather.cygnus.creek.body;

import net.minestom.server.collision.BoundingBox;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.metadata.monster.CreakingMeta;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shows the creek as a vanilla creaking.
 * <p>
 * An enderman would not work: the slender player is already shown as one, so both would look
 * the same. Creakings only spawn from a creaking heart, and Cygnus has none, so the resource
 * pack can retexture them freely.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class CreakingBody implements CreekBody {

    /** How far the goal has to move before a new path is calculated, in blocks. */
    static final double REPATH_DISTANCE = 1.0D;

    private final EntityCreature entity;
    private final Set<UUID> viewers = ConcurrentHashMap.newKeySet();
    private @Nullable Pos goal;

    private CreakingBody(EntityCreature entity) {
        this.entity = entity;
    }

    /**
     * Spawns a creaking. Nobody can see it until {@link #showTo(Set)} is called.
     *
     * @param instance the instance of the round
     * @param position the spawn position
     * @return the body
     */
    public static CreakingBody spawn(Instance instance, Pos position) {
        CreakingBody body = new CreakingBody(new EntityCreature(EntityType.CREAKING));
        body.entity.updateViewableRule(player -> body.viewers.contains(player.getUuid()));
        body.entity.setInstance(instance, position);
        return body;
    }

    @Override
    public Pos position() {
        return this.entity.getPosition();
    }

    @Override
    public void moveTo(Pos goal, double speed) {
        this.entity.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(speed);
        // A walking survivor moves a little between almost every two steps. Recalculating the
        // path each time would be expensive, so small moves are ignored.
        Pos current = this.goal;
        if (current != null && current.distance(goal) <= REPATH_DISTANCE) return;
        // Only remember goals the navigator accepted. A rejected goal (too close, same block,
        // unloaded chunk) or a reached one must not block the next goal.
        boolean started = this.entity.getNavigator().setPathTo(goal, this.arrivalDistance(), () -> this.forget(goal));
        this.goal = started ? goal : null;
    }

    private void forget(Pos reached) {
        if (reached.equals(this.goal)) this.goal = null;
    }

    /**
     * The navigator's default arrival distance: from the center of the bounding box to a corner.
     */
    private double arrivalDistance() {
        BoundingBox box = this.entity.getBoundingBox();
        return Math.sqrt(box.width() * box.width() + box.depth() * box.depth()) / 2.0D;
    }

    @Override
    public void stop() {
        this.goal = null;
        this.entity.getNavigator().reset();
    }

    @Override
    public void teleport(Pos position) {
        this.stop();
        this.entity.teleport(position);
    }

    @Override
    public void lookAt(Pos point) {
        this.entity.lookAt(point);
    }

    @Override
    public void showTo(Set<UUID> viewers) {
        this.viewers.clear();
        this.viewers.addAll(viewers);
        this.entity.updateViewableRule();
    }

    @Override
    public boolean isVisibleTo(UUID viewer) {
        return this.viewers.contains(viewer);
    }

    @Override
    public void setAggressive(boolean aggressive) {
        this.meta().setActive(aggressive);
    }

    @Override
    public void setFrozen(boolean frozen) {
        this.meta().setCanMove(!frozen);
    }

    @Override
    public Entity entity() {
        return this.entity;
    }

    @Override
    public void remove() {
        this.entity.remove();
    }

    private CreakingMeta meta() {
        return (CreakingMeta) this.entity.getEntityMeta();
    }
}
