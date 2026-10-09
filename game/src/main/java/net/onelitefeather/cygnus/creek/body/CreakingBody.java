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
 * The creek in the world, dressed up as a vanilla creaking.
 * <p>
 * An enderman was out of the question: the slender player already looks like one, and nobody could
 * tell the two apart. Creakings only spawn from a creaking heart, and Cygnus has none, so the
 * resource pack is free to give them a new look.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class CreakingBody implements CreekBody {

    /** How far the goal has to move before the creek works out a new path, in blocks. */
    static final double REPATH_DISTANCE = 1.0D;

    private final EntityCreature entity;
    private final Set<UUID> viewers = ConcurrentHashMap.newKeySet();
    private volatile @Nullable UUID observer;
    private @Nullable Pos goal;

    private CreakingBody(EntityCreature entity) {
        this.entity = entity;
    }

    /**
     * Puts a creaking into the world. Nobody sees it until {@link #showTo(Set)} says who may.
     *
     * @param instance the instance of the round
     * @param position where it appears
     * @return the body
     */
    public static CreakingBody spawn(Instance instance, Pos position) {
        CreakingBody body = new CreakingBody(new EntityCreature(EntityType.CREAKING));
        body.entity.getNavigator().setNodeGenerator(FoliageGroundGenerator::new);
        body.entity.getNavigator().setNodeFollower(() -> new StepFollower(body.entity));
        body.entity.updateViewableRule(player -> body.viewers.contains(player.getUuid())
                || player.getUuid().equals(body.observer));
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
        // A survivor on the move shifts a little between almost every two steps. Working out a
        // new path every time would be expensive, so small shifts are ignored.
        Pos current = this.goal;
        if (current != null && current.distance(goal) <= REPATH_DISTANCE) return;
        // Only remember goals the navigator took on. One it turned down (too close, same block,
        // unloaded chunk) or one already reached must not stand in the way of the next.
        boolean started = this.entity.getNavigator().setPathTo(goal, this.arrivalDistance(), () -> this.forget(goal));
        this.goal = started ? goal : null;
    }

    private void forget(Pos reached) {
        if (reached.equals(this.goal)) this.goal = null;
    }

    /**
     * How close counts as arrived for Minestom's navigator: from the middle of the bounding box to
     * one of its corners.
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
    public void observe(@Nullable UUID observer) {
        this.observer = observer;
        this.entity.updateViewableRule();
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
