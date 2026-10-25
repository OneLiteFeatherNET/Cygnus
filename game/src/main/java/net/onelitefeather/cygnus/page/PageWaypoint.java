package net.onelitefeather.cygnus.page;

import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Point;
import net.minestom.server.network.packet.server.play.TrackedWaypointPacket;
import net.minestom.server.network.packet.server.play.TrackedWaypointPacket.Icon;
import net.minestom.server.network.packet.server.play.TrackedWaypointPacket.Operation;
import net.minestom.server.network.packet.server.play.TrackedWaypointPacket.Target;
import net.minestom.server.network.packet.server.play.TrackedWaypointPacket.Waypoint;
import net.minestom.server.utils.Either;

import java.util.UUID;

/**
 * Builds the packets that put a page on a survivor's locator bar.
 * <p>
 * The waypoint never carries the page's own id. The page is an entity, and the client would draw
 * a waypoint with its id right on it once it is close, which gives the exact spot away. Each shown
 * page gets an id of its own instead, and its target is a fixed block.
 * </p>
 *
 * @author Joltra
 * @version 1.0.0
 * @since 2.22.0
 */
final class PageWaypoint {

    /** The look of a page on the locator bar, apart from the red survivors the slender sees. */
    static final Icon ICON = new Icon(Icon.DEFAULT_STYLE, NamedTextColor.WHITE);

    private PageWaypoint() {
    }

    /**
     * Builds the packet that adds the page to the locator bar.
     *
     * @param id    the waypoint's own id
     * @param where the block the waypoint points at
     * @return the packet, meant for one survivor only
     */
    static TrackedWaypointPacket track(UUID id, Point where) {
        return packet(Operation.TRACK, id, new Target.Vec3i(where));
    }

    /**
     * Builds the packet that moves the waypoint, after the page moved.
     *
     * @param id    the waypoint's own id
     * @param where the block the waypoint points at now
     * @return the packet, meant for one survivor only
     */
    static TrackedWaypointPacket update(UUID id, Point where) {
        return packet(Operation.UPDATE, id, new Target.Vec3i(where));
    }

    /**
     * Builds the packet that takes the page off the locator bar.
     *
     * @param id the waypoint's own id
     * @return the packet, meant for one survivor only
     */
    static TrackedWaypointPacket untrack(UUID id) {
        return packet(Operation.UNTRACK, id, new Target.Empty());
    }

    private static TrackedWaypointPacket packet(Operation operation, UUID id, Target target) {
        return new TrackedWaypointPacket(operation, new Waypoint(Either.left(id), ICON, target));
    }
}
