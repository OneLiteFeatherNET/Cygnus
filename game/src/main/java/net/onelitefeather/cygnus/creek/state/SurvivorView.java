package net.onelitefeather.cygnus.creek.state;

import net.minestom.server.coordinate.Pos;

import java.util.UUID;

/**
 * A snapshot of one survivor, taken once per step.
 *
 * @param id        the survivor's id
 * @param position  the survivor's feet, including the view direction
 * @param dread     how likely the survivor is to be stalked or hunted, from 0 to 1
 * @param seesCreek whether the survivor sees the creek right now
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public record SurvivorView(UUID id, Pos position, double dread, boolean seesCreek) {

    /** Eye height of a standing player, in blocks. */
    public static final double EYE_HEIGHT = 1.62D;

    /**
     * Returns the survivor's eye position.
     *
     * @return the eye position, with the survivor's view direction
     */
    public Pos eyes() {
        return this.position.add(0, EYE_HEIGHT, 0);
    }
}
