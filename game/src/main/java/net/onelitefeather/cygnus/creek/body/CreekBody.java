package net.onelitefeather.cygnus.creek.body;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

/**
 * What the creek looks like in the world: the entity that gets drawn and moved around.
 * <p>
 * The states only ever talk to this interface. That way the vanilla creaking can later make way
 * for a custom model without the behaviour changing at all.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public interface CreekBody {

    /**
     * Where the creek's feet are.
     *
     * @return the position
     */
    Pos position();

    /**
     * Walks towards a goal.
     *
     * @param goal  where to go
     * @param speed how fast, in blocks per tick
     */
    void moveTo(Pos goal, double speed);

    /**
     * Stops walking.
     */
    void stop();

    /**
     * Moves the creek somewhere else in an instant.
     *
     * @param position where it ends up
     */
    void teleport(Pos position);

    /**
     * Turns the creek's head towards a point.
     *
     * @param point what to look at
     */
    void lookAt(Pos point);

    /**
     * Lets exactly these players see the creek.
     *
     * @param viewers who may see it; an empty set hides it from everyone
     */
    void showTo(Set<UUID> viewers);

    /**
     * Tells whether a player can see the creek right now.
     *
     * @param viewer the player's id
     * @return {@code true} if the creek is visible to them
     */
    boolean isVisibleTo(UUID viewer);

    /**
     * Lets one extra player see the creek, whoever {@link #showTo(Set)} picks. The slender needs this
     * while he looks through the creek's eyes, since his client has to know the entity. He does not
     * count as someone who sees the creek, so {@link #isVisibleTo(UUID)} leaves him out.
     *
     * @param observer the player, or {@code null} for nobody
     */
    void observe(@Nullable UUID observer);

    /**
     * Switches the hunting look on or off. For the creaking, that means glowing eyes.
     *
     * @param aggressive {@code true} for the hunting look
     */
    void setAggressive(boolean aggressive);

    /**
     * Switches the frozen look on or off.
     *
     * @param frozen {@code true} while the hunted survivor is looking at the creek
     */
    void setFrozen(boolean frozen);

    /**
     * The entity behind the body, needed to check who can see it.
     *
     * @return the entity
     */
    Entity entity();

    /**
     * Takes the creek out of the world.
     */
    void remove();
}
