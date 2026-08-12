package net.onelitefeather.cygnus.view.event;

import net.minestom.server.event.Event;

/**
 * The event is used to signal that the round's HUD components (e.g. {@link
 * net.onelitefeather.cygnus.hud.PageTimerHudComponent} and {@link
 * net.onelitefeather.cygnus.hud.PageCountHudComponent}) need to be updated.
 *
 * @param ticks value to update the view
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.3.1
 */
public record ViewUpdateEvent(int ticks) implements Event {
}
