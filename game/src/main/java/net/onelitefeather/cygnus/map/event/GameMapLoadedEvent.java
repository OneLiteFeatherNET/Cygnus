package net.onelitefeather.cygnus.map.event;

import net.minestom.server.event.Event;
import net.minestom.server.instance.InstanceContainer;
import net.onelitefeather.cygnus.common.map.GameMap;

/**
 * Called when the map for the game is loaded. This is called after the map is loaded, but before the game starts.
 *
 * @param gameMap           which was loaded
 * @param instanceContainer the instance container of the map
 */
public record GameMapLoadedEvent(GameMap gameMap, InstanceContainer instanceContainer) implements Event {
}
