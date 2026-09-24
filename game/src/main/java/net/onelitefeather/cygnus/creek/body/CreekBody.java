package net.onelitefeather.cygnus.creek.body;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;

import java.util.Set;
import java.util.UUID;

/**
 * The creek's appearance in the world: the entity that is drawn and moved.
 * <p>
 * The states only use this interface. That way the vanilla creaking can later be replaced by a
 * custom model without changing the behavior.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public interface CreekBody {

    /**
     * Returns the position of the creek's feet.
     *
     * @return the position
     */
    Pos position();

    /**
     * Walks towards a goal.
     *
     * @param goal  where to go
     * @param speed the movement speed, in blocks per tick
     */
    void moveTo(Pos goal, double speed);

    /**
     * Stops walking.
     */
    void stop();

    /**
     * Moves the creek to a position instantly.
     *
     * @param position the new position
     */
    void teleport(Pos position);

    /**
     * Turns the creek's head towards a point.
     *
     * @param point what to look at
     */
    void lookAt(Pos point);

    /**
     * Makes the creek visible to exactly these players.
     *
     * @param viewers the players who can see the creek; an empty set hides it from everyone
     */
    void showTo(Set<UUID> viewers);

    /**
     * Returns whether a player can currently see the creek.
     *
     * @param viewer the player's id
     * @return {@code true} if the creek is visible to that player
     */
    boolean isVisibleTo(UUID viewer);

    /**
     * Switches the hunting look on or off (for the creaking: glowing eyes).
     *
     * @param aggressive {@code true} for the hunting look
     */
    void setAggressive(boolean aggressive);

    /**
     * Switches the frozen look on or off.
     *
     * @param frozen {@code true} while a survivor looks at the creek during a hunt
     */
    void setFrozen(boolean frozen);

    /**
     * Returns the entity behind the body. Used for line-of-sight checks.
     *
     * @return the entity
     */
    Entity entity();

    /**
     * Removes the creek from the world.
     */
    void remove();
}
