package net.onelitefeather.cygnus.creek.dread;

import net.minestom.server.coordinate.Pos;

import java.util.List;
import java.util.UUID;

/**
 * Rates how likely a survivor is to be haunted.
 * <p>
 * The creek only ever asks this interface. In a round it is answered by the survivors' fear,
 * {@code SanityService}.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
@FunctionalInterface
public interface DreadSource {

    /**
     * Rates one survivor.
     *
     * @param survivor the survivor to rate
     * @param position where the survivor is
     * @param others   where all the other survivors are
     * @return the dread, between {@code 0} and {@code 1}
     */
    double dreadOf(UUID survivor, Pos position, List<Pos> others);
}
