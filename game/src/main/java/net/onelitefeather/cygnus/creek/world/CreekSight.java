package net.onelitefeather.cygnus.creek.world;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.onelitefeather.cygnus.gaze.SlenderGaze;

/**
 * Works out whether a survivor can see the creek.
 * <p>
 * It uses the same view cone as {@link SlenderGaze}: close enough, and not too far off to the side
 * of where the survivor is looking. On top of that it checks the line of sight, because the creek
 * is usually far away and often behind trees.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class CreekSight {

    private final SlenderGaze cone;

    /**
     * Sets up the check with the given cone.
     *
     * @param range     how far away the creek can still be noticed, in blocks
     * @param viewAngle how far off to the side it can still be noticed, in degrees from the view centre
     */
    public CreekSight(int range, int viewAngle) {
        // SlenderGaze wants a close distance below its range. All that matters here is "inside
        // the cone or not", and zero keeps the whole range.
        this.cone = new SlenderGaze(range, 0, viewAngle);
    }

    /**
     * Checks whether a point is inside someone's view cone. Blocks in between do not matter here.
     *
     * @param observer where the observer's eyes are, including where they are looking
     * @param target   the point to check
     * @return {@code true} if the point is inside the cone
     */
    public boolean inView(Pos observer, Point target) {
        return this.cone.levelOf(observer, target.asPos()) != SlenderGaze.NONE;
    }

    /**
     * Checks whether a player can see the creek: it is inside their view cone and no block is in the
     * way.
     *
     * @param observer the player
     * @param creek    the creek's entity
     * @return {@code true} if the player can see the creek
     */
    public boolean sees(Player observer, Entity creek) {
        Instance instance = observer.getInstance();
        if (instance == null || !instance.equals(creek.getInstance())) return false;

        Pos eyes = observer.getPosition().add(0, observer.getEyeHeight(), 0);
        Pos body = creek.getPosition().add(0, creek.getEyeHeight(), 0);
        return this.inView(eyes, body) && observer.hasLineOfSight(creek);
    }
}
