package net.onelitefeather.cygnus.creek.consequence;

import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.TrackedWaypointPacket;
import net.minestom.server.network.packet.server.play.TrackedWaypointPacket.Icon;
import net.minestom.server.network.packet.server.play.TrackedWaypointPacket.Operation;
import net.minestom.server.network.packet.server.play.TrackedWaypointPacket.Target;
import net.minestom.server.network.packet.server.play.TrackedWaypointPacket.Waypoint;
import net.minestom.server.utils.Either;

/**
 * Builds the packets that put a survivor on the slender's locator bar.
 * <p>
 * Minestom does not fill the locator bar on its own, so every waypoint is a packet sent to the
 * slender only. The waypoint carries the survivor's UUID, so the client renders it smoothly while
 * the survivor is close, and it needs an {@linkplain #update update} whenever the survivor moves.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.17.0
 */
public final class SurvivorWaypoint {

    /** The look of a survivor on the locator bar. */
    static final Icon ICON = new Icon(Icon.DEFAULT_STYLE, NamedTextColor.RED);

    private SurvivorWaypoint() {
    }

    /**
     * Builds the packet that adds the survivor to the locator bar.
     *
     * @param survivor the survivor to show
     * @return the packet, meant for the slender only
     */
    public static TrackedWaypointPacket track(Player survivor) {
        return packet(Operation.TRACK, survivor, new Target.Vec3i(survivor.getPosition()));
    }

    /**
     * Builds the packet that moves the survivor's waypoint to where he is now.
     *
     * @param survivor the survivor who is shown
     * @return the packet, meant for the slender only
     */
    public static TrackedWaypointPacket update(Player survivor) {
        return packet(Operation.UPDATE, survivor, new Target.Vec3i(survivor.getPosition()));
    }

    /**
     * Builds the packet that takes the survivor off the locator bar.
     *
     * @param survivor the survivor who was shown
     * @return the packet, meant for the slender only
     */
    public static TrackedWaypointPacket untrack(Player survivor) {
        return packet(Operation.UNTRACK, survivor, new Target.Empty());
    }

    private static TrackedWaypointPacket packet(Operation operation, Player survivor, Target target) {
        return new TrackedWaypointPacket(operation, new Waypoint(Either.left(survivor.getUuid()), ICON, target));
    }
}
