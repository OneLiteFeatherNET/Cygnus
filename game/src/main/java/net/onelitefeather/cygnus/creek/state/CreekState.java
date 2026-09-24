package net.onelitefeather.cygnus.creek.state;

/**
 * One phase of the creek's behavior, such as wandering or hunting.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public interface CreekState {

    /**
     * Prepares the body for this state. Called once when the creek switches to it.
     *
     * @param ctx the current step
     */
    void enter(CreekContext ctx);

    /**
     * Runs one step.
     *
     * @param ctx the current step
     * @return {@code this} to stay, or the next state
     */
    CreekState tick(CreekContext ctx);
}
