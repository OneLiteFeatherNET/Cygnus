package net.onelitefeather.cygnus.creek.dread;


import java.util.UUID;

/**
 * Rates how likely a survivor is to be haunted.
 * <p>
 * The creek asks it whom to haunt, and the slender's swing asks it how much a survivor is spared.
 * In a round it is answered by the survivors' fear, {@code SanityService}.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.1.0
 * @since 2.15.0
 */
@FunctionalInterface
public interface DreadSource {

    /**
     * Rates one survivor.
     *
     * @param survivor the survivor to rate
     * @param now      the current time in milliseconds, so a caller's step rates everyone at the same moment
     * @return the dread, between {@code 0} and {@code 1}
     */
    double dreadOf(UUID survivor, long now);
}
