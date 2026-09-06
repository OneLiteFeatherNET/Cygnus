package net.onelitefeather.cygnus.common.page.event;

import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.trait.PlayerEvent;

/**
 * Called every time a survivor claims a page, with the state of the round attached.
 * <p>
 * {@link PageDiscoveryCompletedEvent} only says that the last page is gone, which is one moment in
 * a round that spends the rest of its time getting worse. This event fires on every find and
 * carries how far along the round is, so an effect can be scaled against it instead of counting
 * finds for itself.
 * </p>
 *
 * @param finder     the survivor who claimed the page
 * @param foundCount how many pages have been claimed including this one, starting at {@code 1}
 * @param maxPages   how many pages the round needs in total
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.14.0
 */
public record PageFoundEvent(Player finder, int foundCount, int maxPages) implements Event, PlayerEvent {

    /**
     * Returns the survivor who claimed the page.
     *
     * @return the survivor
     */
    @Override
    public Player getPlayer() {
        return this.finder;
    }
}
