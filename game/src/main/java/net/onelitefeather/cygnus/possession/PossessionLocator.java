package net.onelitefeather.cygnus.possession;

import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.creek.consequence.SurvivorWaypoint;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Puts the survivors around the possessed creek on the slender's locator bar.
 * <p>
 * The waypoints go to the slender only, see {@link SurvivorWaypoint}. Every update adds the
 * survivors who came into range, moves the ones still in range and removes the ones who left.
 * </p>
 *
 * @author theEvilReaper
 * @version 2.0.0
 * @since 2.16.0
 */
final class PossessionLocator {

    private final Set<Player> tracked = new HashSet<>();

    /**
     * Shows exactly these survivors on the slender's locator bar.
     *
     * @param slender the slender who sees the waypoints
     * @param inRange the survivors around the creek right now
     */
    void update(Player slender, Set<Player> inRange) {
        for (Player survivor : List.copyOf(this.tracked)) {
            if (inRange.contains(survivor)) continue;
            this.tracked.remove(survivor);
            slender.sendPacket(SurvivorWaypoint.untrack(survivor));
        }
        for (Player survivor : inRange) {
            boolean added = this.tracked.add(survivor);
            slender.sendPacket(added ? SurvivorWaypoint.track(survivor) : SurvivorWaypoint.update(survivor));
        }
    }

    /**
     * Takes every survivor off the slender's locator bar.
     *
     * @param slender the slender who saw the waypoints
     */
    void clear(Player slender) {
        for (Player survivor : this.tracked) {
            slender.sendPacket(SurvivorWaypoint.untrack(survivor));
        }
        this.tracked.clear();
    }

    /**
     * Forgets every waypoint without sending anything, for a slender who is gone.
     */
    void forget() {
        this.tracked.clear();
    }

    /**
     * Returns the survivors on the locator bar right now.
     *
     * @return a copy of the set
     */
    Set<Player> tracked() {
        return Set.copyOf(this.tracked);
    }
}
