package net.onelitefeather.cygnus.creek.state;

/**
 * One phase of the creek's behaviour, such as patrolling, stalking or hunting.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public interface CreekState {

    /**
     * Gets the body ready for this state. Called once, when the creek switches to it.
     *
     * @param ctx the current step
     */
    void enter(CreekContext ctx);

    /**
     * Runs one step.
     *
     * @param ctx the current step
     * @return {@code this} to carry on, or the state to switch to
     */
    CreekState tick(CreekContext ctx);
}
