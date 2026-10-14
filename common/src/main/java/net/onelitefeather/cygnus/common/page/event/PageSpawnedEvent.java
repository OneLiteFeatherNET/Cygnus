package net.onelitefeather.cygnus.common.page.event;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.Event;

import java.util.UUID;

/**
 * Called whenever a page becomes collectible on a spot: when the round places the start pages, and
 * when a found or expired page is moved on.
 * <p>
 * It carries what the page entity cannot say reliably itself: a page that is placed or teleported
 * reports its old position until the move has gone through, so the spot is given explicitly. Together
 * with {@link PageFoundEvent#pageId()} it lets a listener work out where a page stood and for how long.
 * A page that has to wait on its own spot (no other spot is free) is announced when it is hidden, not
 * when it shows up again.
 * </p>
 *
 * @param pageId    the id of the page, the same one {@link PageFoundEvent#pageId()} carries
 * @param position  the spot the page stands on
 * @param relocated {@code false} for the first placement of the round, {@code true} when an existing
 *                  page was moved on
 * @param skipped   how many spots the page passed over because a survivor stood next to them
 * @param watched   whether a survivor stands next to the spot anyway, since every spot it tried was
 * @author TheMeinerLP
 * @version 1.1.0
 * @since 2.15.0
 */
public record PageSpawnedEvent(UUID pageId, Pos position, boolean relocated, int skipped, boolean watched)
        implements Event {

    /**
     * Creates the event for a page whose spot was not checked against the survivors.
     *
     * @param pageId    the id of the page
     * @param position  the spot the page stands on
     * @param relocated whether an existing page was moved on
     */
    public PageSpawnedEvent(UUID pageId, Pos position, boolean relocated) {
        this(pageId, position, relocated, 0, false);
    }
}
