package net.onelitefeather.cygnus.possession;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * One look through the creek's eyes: who looks, and until when.
 *
 * @param slender the slender who possesses the creek
 * @param endsAt  when the possession is over at the latest, in milliseconds
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.16.0
 */
record Possession(Player slender, long endsAt) {

    /**
     * Starts a possession.
     *
     * @param slender    the slender
     * @param now        the current time in milliseconds
     * @param maxSeconds how long it may last
     * @return the possession
     */
    static Possession start(Player slender, long now, int maxSeconds) {
        return new Possession(slender, now + maxSeconds * 1000L);
    }

    /**
     * Tells whether the possession has run out of time.
     *
     * @param now the current time in milliseconds
     * @return {@code true} once {@link #endsAt()} is reached
     */
    boolean isOver(long now) {
        return now >= this.endsAt;
    }

    /**
     * Picks the survivors around the creek.
     *
     * @param creek     the creek's entity
     * @param range     how far around it, in blocks
     * @param survivors the survivors of the round
     * @return the survivors in the creek's instance within the range
     */
    static Set<Player> inRange(Entity creek, double range, Collection<Player> survivors) {
        Instance instance = creek.getInstance();
        Pos center = creek.getPosition();
        double rangeSquared = range * range;
        Set<Player> near = new HashSet<>();
        for (Player survivor : survivors) {
            if (survivor.getInstance() != instance) continue;
            if (survivor.getPosition().distanceSquared(center) <= rangeSquared) near.add(survivor);
        }
        return near;
    }
}
