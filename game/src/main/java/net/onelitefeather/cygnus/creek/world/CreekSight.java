package net.onelitefeather.cygnus.creek.world;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.onelitefeather.cygnus.gaze.SlenderGaze;

/**
 * Checks whether a survivor sees the creek.
 * <p>
 * It uses the same view cone as {@link SlenderGaze}: within a range and an angle of the
 * survivor's view direction. On top of that it checks the line of sight, because the creek is
 * usually far away and often behind trees.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class CreekSight {

    private final SlenderGaze cone;

    /**
     * Creates a sight check with the given cone.
     *
     * @param range     how far away the creek can be noticed, in blocks
     * @param viewAngle the maximum angle from the view center, in degrees
     */
    public CreekSight(int range, int viewAngle) {
        // SlenderGaze needs a close distance below its range. Only "inside the cone or not"
        // matters here, and zero keeps the full range.
        this.cone = new SlenderGaze(range, 0, viewAngle);
    }

    /**
     * Checks whether a point is inside an observer's view cone. Blocks in between are ignored.
     *
     * @param observer the observer's eye position, including yaw and pitch
     * @param target   the point to check
     * @return {@code true} if the point is inside the cone
     */
    public boolean inView(Pos observer, Point target) {
        return this.cone.levelOf(observer, target.asPos()) != SlenderGaze.NONE;
    }

    /**
     * Checks whether a player sees the creek: it is inside the view cone and no block is in
     * the way.
     *
     * @param observer the player
     * @param creek    the creek's entity
     * @return {@code true} if the player sees the creek
     */
    public boolean sees(Player observer, Entity creek) {
        Instance instance = observer.getInstance();
        if (instance == null || !instance.equals(creek.getInstance())) return false;

        Pos eyes = observer.getPosition().add(0, observer.getEyeHeight(), 0);
        Pos body = creek.getPosition().add(0, creek.getEyeHeight(), 0);
        return this.inView(eyes, body) && observer.hasLineOfSight(creek);
    }
}
