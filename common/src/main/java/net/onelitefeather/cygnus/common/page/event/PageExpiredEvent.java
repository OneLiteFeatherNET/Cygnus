package net.onelitefeather.cygnus.common.page.event;

import net.minestom.server.event.Event;
import net.onelitefeather.cygnus.common.page.PageEntity;

/**
 * The event is called when a page entity's time to live expired without being found by a player.
 *
 * @param entity the page entity whose TTL expired
 */
public record PageExpiredEvent(PageEntity entity) implements Event {
}
