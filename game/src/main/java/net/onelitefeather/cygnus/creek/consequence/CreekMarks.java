package net.onelitefeather.cygnus.creek.consequence;

import net.minestom.server.entity.Player;

/**
 * Decides whether the creek gives a survivor away to the slender outside of a catch.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.21.0
 */
public interface CreekMarks {

    /** Marks that never give anyone away. */
    CreekMarks NONE = new CreekMarks() {
        @Override
        public void stared(Player survivor) {
        }

        @Override
        public void spotted(Player survivor) {
        }
    };

    /**
     * The patrolling creek stared at a survivor until the end.
     *
     * @param survivor the survivor it stared at
     */
    void stared(Player survivor);

    /**
     * A stalked survivor looked at the creek long enough that it moved away.
     *
     * @param survivor the survivor it stalks
     */
    void spotted(Player survivor);

    /**
     * Forgets everything about the round that ended.
     */
    default void cleanUp() {
    }
}
