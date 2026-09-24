package net.onelitefeather.cygnus.creek.dread;

import net.minestom.server.coordinate.Pos;

import java.util.List;
import java.util.UUID;

/**
 * Rates how likely a survivor is to be stalked or hunted.
 * <p>
 * The creek only uses this interface. A later sanity system can replace the rating without
 * changing the creek.
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
     * @param position the survivor's position
     * @param others   the positions of all other survivors
     * @return the dread, between {@code 0} and {@code 1}
     */
    double dreadOf(UUID survivor, Pos position, List<Pos> others);
}
