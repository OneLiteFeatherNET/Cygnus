package net.onelitefeather.cygnus.map;

import net.onelitefeather.cygnus.common.map.GameMap;
import net.theevilreaper.aves.map.MapEntry;

/**
 * A game map that was found below {@code game/maps} and read without an error, so a round can be
 * played on it.
 *
 * @param entry the directory the map lives in
 * @param map   the map read from its {@code map.json}, with the page and creek settings of its side files
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.21.0
 */
public record MapCandidate(MapEntry entry, GameMap map) {

    /**
     * Returns the name of the map.
     *
     * @return the name the map declares in its {@code map.json}
     */
    public String name() {
        return this.map.name();
    }
}
