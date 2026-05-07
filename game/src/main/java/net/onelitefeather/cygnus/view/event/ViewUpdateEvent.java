package net.onelitefeather.cygnus.view.event;

import net.minestom.server.event.Event;

/**
 * The event is used that the {@link net.onelitefeather.cygnus.view.GameView} needs to be updated.
 *
 * @param ticks value to update the view
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.3.1
 */
public record ViewUpdateEvent(int ticks) implements Event {
}
