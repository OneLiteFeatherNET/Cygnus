package net.onelitefeather.cygnus.creek.consequence;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;

import java.util.Collection;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * What the patrolling creek does to the survivors around it.
 * <p>
 * When it picks out a survivor who came too close, it is a coin toss: either the survivor freezes
 * on the spot and everyone standing next to them goes blind for a moment, or the creek flings them
 * away from itself. It is a scare, not a catch, so it never counts towards
 * giving them away to the slender. When it vanishes at the end of its route, everyone close by
 * hears it and goes blind the same way.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class PatrolHelper {

    /** How long the survivor stays frozen, in ticks (2 seconds). */
    static final int STUN_TICKS = 40;

    /** Slowness VII: strong enough that the survivor cannot move at all. */
    static final int STUN_AMPLIFIER = 6;

    /** Everyone this close to the frozen survivor goes blind too, in blocks. */
    static final double BLIND_RADIUS = 8.0D;

    /** How long they stay blind, in ticks (2 seconds). */
    static final int BLIND_TICKS = 40;

    /** A fling needs at least this much room, in blocks. With less, it tries another way. */
    static final double FLING_MIN_DISTANCE = 4.0D;

    /** A fling goes at most this far, however much room there is, in blocks. */
    static final double FLING_MAX_DISTANCE = 10.0D;

    /**
     * How fast to send a survivor for every block they should fly, in blocks per second. The client
     * slows them down on its own, so this is worked out by hand: tune it if flings fall short or
     * overshoot.
     */
    static final double FLING_SPEED_PER_BLOCK = 2.7D;

    /** How fast a fling lifts the survivor off the ground, in blocks per second. */
    static final double FLING_LIFT = 8.0D;

    /** The ways a fling tries, in degrees off straight away from the creek, in this order. */
    private static final double[] FLING_TURNS = {0.0D, 45.0D, -45.0D, 90.0D, -90.0D};

    /** How finely the room in front of a survivor is measured, in blocks. */
    private static final double ROOM_STEP = 0.5D;

    /** The heights above the feet at which the room is measured, so the whole body fits through. */
    private static final double[] BODY_HEIGHTS = {0.5D, 1.5D};

    private final RandomGenerator random;
    private final TrackedEffects effects = new TrackedEffects();

    /**
     * Sets up the consequence for a round.
     *
     * @param random tosses the coin between freezing and flinging
     */
    public PatrolHelper(RandomGenerator random) {
        this.random = random;
    }

    /**
     * Freezes the survivor or flings them away. If there is no room to fling them, they are frozen
     * instead, so a selection never goes unnoticed.
     *
     * @param selected  the survivor the creek picked out
     * @param creek     where the creek stands, to fling them away from it
     * @param survivors every survivor of the round, to find the ones standing next to a frozen one
     */
    public void selected(Player selected, Pos creek, Collection<Player> survivors) {
        if (this.random.nextBoolean() || !this.flingAway(selected, creek)) {
            this.stun(selected, survivors);
        }
    }

    /**
     * Freezes the survivor for a moment and blinds the other survivors standing next to them.
     */
    void stun(Player selected, Collection<Player> survivors) {
        CatchEffects.playScare(selected);
        this.effects.add(selected, new Potion(PotionEffect.SLOWNESS, STUN_AMPLIFIER, STUN_TICKS));
        for (Player other : near(selected.getPosition(), survivors)) {
            if (other != selected) this.blind(other);
        }
    }

    /**
     * The creek vanished: every survivor close by hears it and goes blind for a moment.
     *
     * @param where     where the creek vanished
     * @param survivors the survivors it may blind
     */
    public void vanished(Pos where, Collection<Player> survivors) {
        for (Player survivor : near(where, survivors)) {
            CatchEffects.playScare(survivor);
            this.blind(survivor);
        }
    }

    private void blind(Player player) {
        this.effects.add(player, new Potion(PotionEffect.BLINDNESS, 0, BLIND_TICKS));
    }

    /**
     * The survivors within {@link #BLIND_RADIUS} of a point.
     */
    private static List<Player> near(Pos center, Collection<Player> survivors) {
        return survivors.stream().filter(survivor -> survivor.getPosition().distance(center) <= BLIND_RADIUS).toList();
    }

    /**
     * Flings the survivor away from the creek, as far as there is room but no further than
     * {@link #FLING_MAX_DISTANCE}. Straight away comes first; if something stands in the way within
     * {@link #FLING_MIN_DISTANCE}, it tries to the sides.
     *
     * @return {@code false} if there is not enough room in any of those ways
     */
    boolean flingAway(Player selected, Pos creek) {
        Instance instance = selected.getInstance();
        if (instance == null) return false;
        Pos from = selected.getPosition();
        double away = Math.atan2(from.z() - creek.z(), from.x() - creek.x());
        for (double turn : FLING_TURNS) {
            double angle = away + Math.toRadians(turn);
            Vec direction = new Vec(Math.cos(angle), 0.0D, Math.sin(angle));
            double room = room(instance, from, direction);
            if (room < FLING_MIN_DISTANCE) continue;
            Vec push = direction.mul(room * FLING_SPEED_PER_BLOCK);
            selected.setVelocity(new Vec(push.x(), FLING_LIFT, push.z()));
            CatchEffects.playScare(selected);
            return true;
        }
        return false;
    }

    /**
     * How far the survivor can fly in a direction before hitting something, up to
     * {@link #FLING_MAX_DISTANCE}. An unloaded chunk counts as a wall.
     */
    private static double room(Instance instance, Pos from, Vec direction) {
        for (double distance = ROOM_STEP; distance <= FLING_MAX_DISTANCE; distance += ROOM_STEP) {
            Pos ahead = from.add(direction.mul(distance));
            if (!instance.isChunkLoaded(ahead)) return distance - ROOM_STEP;
            for (double height : BODY_HEIGHTS) {
                if (instance.getBlock(ahead.add(0.0D, height, 0.0D)).solid()) return distance - ROOM_STEP;
            }
        }
        return FLING_MAX_DISTANCE;
    }

    /**
     * Takes back every slowness and blindness handed out that is still running, so nothing lingers
     * after the round.
     */
    public void cleanUp() {
        this.effects.removeAll();
    }
}
