package net.onelitefeather.cygnus.creek.dread;

import java.util.UUID;

/**
 * Hears what the creek does to the survivors.
 * <p>
 * The counterpart to {@link DreadSource}: the creek reads the dread through one and reports
 * through the other, without knowing who listens.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.16.0
 */
public interface CreekWitness {

    /**
     * Hears nothing. For tests and rounds without anyone listening.
     */
    CreekWitness NONE = new CreekWitness() {
        @Override
        public void sighted(UUID survivor) {
            // Nobody listens.
        }

        @Override
        public void caught(UUID survivor) {
            // Nobody listens.
        }
    };

    /**
     * A survivor has just spotted a creek. Called once when it comes into view, not for every step
     * it stays there.
     *
     * @param survivor the survivor who spotted it
     */
    void sighted(UUID survivor);

    /**
     * A creek has caught a survivor.
     *
     * @param survivor the survivor who was caught
     */
    void caught(UUID survivor);
}
