package net.onelitefeather.cygnus.creek.dread;

/**
 * How a creek's hunt of a survivor ended, as told to {@link CreekWitness#huntEnded(java.util.UUID, HuntEnd)}.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
public enum HuntEnd {

    /** The creek got close enough with nothing in between. */
    CAUGHT,
    /** The hunt ran out of time. */
    TIMEOUT,
    /** The survivor was no longer among the hunted, because they died, left or were turned. */
    GONE,
    /** The creek was sent away for good while it hunted. */
    SENT_AWAY,
    /** The creek was taken out of the world, which is what the end of a round does. */
    REMOVED
}
