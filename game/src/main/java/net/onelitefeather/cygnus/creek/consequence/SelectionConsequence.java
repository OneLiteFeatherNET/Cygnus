package net.onelitefeather.cygnus.creek.consequence;

import net.kyori.adventure.sound.Sound;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;
import net.onelitefeather.cygnus.creek.world.Ground;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

/**
 * What happens when the patrolling creek picks out a survivor who came too close.
 * <p>
 * It is a coin toss: either the survivor freezes on the spot and everyone standing next to them
 * goes blind for a moment, or they suddenly find themselves somewhere else on the map. It is a
 * scare, not a catch, so it never counts towards giving them away to the slender.
 * </p>
 * <p>
 * When the patrolling creek vanishes at the end of its route, the survivors around it go blind
 * the same way.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class SelectionConsequence {

    /** How long the survivor stays frozen, in ticks (2 seconds). */
    static final int STUN_TICKS = 40;

    /** Slowness VII: strong enough that the survivor cannot move at all. */
    static final int STUN_AMPLIFIER = 6;

    /** Everyone this close to the frozen survivor goes blind too, in blocks. */
    static final double BLIND_RADIUS = 8.0D;

    /** How long they stay blind, in ticks (2 seconds). */
    static final int BLIND_TICKS = 40;

    /** A teleported survivor lands at least this far away, in blocks. */
    static final double TELEPORT_MIN_DISTANCE = 20.0D;

    /** A teleported survivor lands at most this far away, so they stay in the same part of the map. */
    static final double TELEPORT_MAX_DISTANCE = 40.0D;

    private final Supplier<List<Pos>> routePoints;
    private final Ground ground;
    private final RandomGenerator random;
    private final TrackedEffects effects = new TrackedEffects();

    /**
     * Sets up the consequence for a round.
     *
     * @param routePoints supplies the points of the map's routes, the places a survivor can be sent to
     * @param ground      finds the floor at such a place
     * @param random      tosses the coin between freezing and teleporting, and picks the place
     */
    public SelectionConsequence(Supplier<List<Pos>> routePoints, Ground ground, RandomGenerator random) {
        this.routePoints = routePoints;
        this.ground = ground;
        this.random = random;
    }

    /**
     * Freezes the survivor or sends them away. If there is nowhere to send them, they are frozen
     * instead, so a selection never goes unnoticed.
     *
     * @param selected  the survivor the creek picked out
     * @param survivors every survivor of the round, to find the ones standing next to a frozen one
     */
    public void apply(Player selected, Collection<Player> survivors) {
        if (this.random.nextBoolean() || !this.teleportAway(selected)) {
            this.stun(selected, survivors);
        }
    }

    /**
     * Freezes the survivor for a moment and blinds the other survivors standing next to them.
     */
    void stun(Player selected, Collection<Player> survivors) {
        scare(selected);
        this.effects.add(selected, new Potion(PotionEffect.SLOWNESS, STUN_AMPLIFIER, STUN_TICKS));
        this.blindAround(selected.getPosition(), survivors, selected);
    }

    /**
     * The creek vanished: every survivor close by hears it and goes blind for a moment.
     *
     * @param where     where the creek vanished
     * @param survivors the survivors it may blind
     */
    public void vanishAt(Pos where, Collection<Player> survivors) {
        for (Player survivor : survivors) {
            if (survivor.getPosition().distance(where) <= BLIND_RADIUS) scare(survivor);
        }
        this.blindAround(where, survivors, null);
    }

    /**
     * Blinds every survivor within {@link #BLIND_RADIUS} of the centre, except the one spared.
     */
    private void blindAround(Pos center, Collection<Player> survivors, @Nullable Player spared) {
        for (Player other : survivors) {
            if (other == spared || other.getPosition().distance(center) > BLIND_RADIUS) continue;
            this.effects.add(other, new Potion(PotionEffect.BLINDNESS, 0, BLIND_TICKS));
        }
    }

    /**
     * Sends the survivor to a random route point 20 to 40 blocks away. Route points are always
     * walkable, so nobody ends up inside a wall.
     *
     * @return {@code false} if none of the route points in that range has a floor
     */
    boolean teleportAway(Player selected) {
        Pos from = selected.getPosition();
        List<Pos> targets = new ArrayList<>();
        for (Pos point : this.routePoints.get()) {
            double distance = point.distance(from);
            if (distance < TELEPORT_MIN_DISTANCE || distance > TELEPORT_MAX_DISTANCE) continue;
            this.ground.settle(point).ifPresent(targets::add);
        }
        if (targets.isEmpty()) return false;
        Pos target = targets.get(this.random.nextInt(targets.size()));
        selected.teleport(target.withView(from.yaw(), from.pitch()));
        scare(selected);
        return true;
    }

    /**
     * Takes back every slowness and blindness handed out that is still running, so nothing lingers
     * after the round.
     */
    public void cleanUp() {
        this.effects.removeAll();
    }

    private static void scare(Player player) {
        player.playSound(Sound.sound(CatchEffects.SCARE_SOUND, Sound.Source.HOSTILE, 1.0F, 0.6F));
    }
}
