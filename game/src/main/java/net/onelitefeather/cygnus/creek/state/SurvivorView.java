package net.onelitefeather.cygnus.creek.state;

import net.minestom.server.coordinate.Pos;

import java.util.UUID;

/**
 * A snapshot of one survivor, taken once per step.
 *
 * @param id        the survivor's id
 * @param position  where the survivor's feet are, including where they are looking
 * @param dread     how likely the survivor is to be haunted, from 0 to 1
 * @param seesCreek whether the survivor can see the creek right now
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public record SurvivorView(UUID id, Pos position, double dread, boolean seesCreek) {

    /** How high a standing player's eyes are, in blocks. */
    public static final double EYE_HEIGHT = 1.62D;

    /**
     * Where the survivor's eyes are.
     *
     * @return the eye position, looking where the survivor looks
     */
    public Pos eyes() {
        return this.position.add(0, EYE_HEIGHT, 0);
    }

    /**
     * The same snapshot, with the given answer to whether the survivor sees the creek.
     *
     * @param sees whether the survivor sees the creek
     * @return this view if nothing changes, otherwise a copy
     */
    public SurvivorView withSeesCreek(boolean sees) {
        return sees == this.seesCreek ? this : new SurvivorView(this.id, this.position, this.dread, sees);
    }
}
