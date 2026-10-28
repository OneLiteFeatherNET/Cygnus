package net.onelitefeather.cygnus.map;

import java.util.List;

/**
 * Decides which of the found maps a round is played on.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.21.0
 */
@FunctionalInterface
public interface MapSelector {

    /**
     * Picks one of the given maps.
     *
     * @param candidates the maps to choose from, never empty
     * @return one of the given maps
     */
    MapCandidate select(List<MapCandidate> candidates);
}
