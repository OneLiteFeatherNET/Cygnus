package net.onelitefeather.cygnus.creek.consequence;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;
import net.onelitefeather.cygnus.creek.state.PatrolState;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

/**
 * What the patrolling creek does to the survivors around it.
 * <p>
 * While it stares at a survivor before picking them out, their view darkens and they hear its heart
 * beat faster. Once it has picked them out, it is a coin toss: either the survivor freezes on the
 * spot with their head turned towards the creek, and everyone standing next to them goes blind for a
 * moment, or the creek flings them away from itself. It is a scare, not a catch, so it never counts towards
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

    /** How long the darkness of a stare lasts at most, in ticks: as long as the stare itself. */
    static final int STARE_DARKNESS_TICKS = (int) (PatrolState.STARE_MILLIS / 50L);

    static final Key TWITCH_SOUND = Key.key("entity.creaking.twitch");
    static final Key HEARTBEAT_SOUND = Key.key("entity.warden.heartbeat");
    static final Key BROKEN_SOUND = Key.key("entity.creaking.deactivate");

    /** How much higher each heartbeat of a stare sounds than the one before. */
    static final float HEARTBEAT_PITCH_STEP = 0.15F;

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
    private final FaceLock faces = new FaceLock();

    /**
     * Sets up the consequence for a round.
     *
     * @param random tosses the coin between freezing and flinging
     */
    public PatrolHelper(RandomGenerator random) {
        this.random = random;
    }

    /**
     * One heartbeat while the creek stares at a survivor. The first one also makes the creek twitch
     * and darkens the survivor's view for as long as the stare lasts.
     *
     * @param target the survivor it stares at
     * @param beat   which beat this is, counting from 0
     * @param creek  where the creek stands
     */
    public void stareBeat(Player target, int beat, Pos creek) {
        if (beat == 0) {
            target.playSound(Sound.sound(TWITCH_SOUND, Sound.Source.HOSTILE, 1.0F, 1.0F), creek.x(), creek.y(), creek.z());
            this.effects.add(target, new Potion(PotionEffect.DARKNESS, 0, STARE_DARKNESS_TICKS));
        }
        float pitch = 1.0F + beat * HEARTBEAT_PITCH_STEP;
        target.playSound(Sound.sound(HEARTBEAT_SOUND, Sound.Source.HOSTILE, 1.0F, pitch));
    }

    /**
     * The survivor got out of the creek's sight during the stare: it lets go and their view clears.
     *
     * @param target the survivor it stared at
     */
    public void stareBroken(Player target) {
        target.playSound(Sound.sound(BROKEN_SOUND, Sound.Source.HOSTILE, 1.0F, 1.0F));
        this.effects.remove(target, PotionEffect.DARKNESS);
    }

    /**
     * Freezes the survivor or flings them away. If there is no room to fling them, they are frozen
     * instead, so a selection never goes unnoticed.
     *
     * @param selected  the survivor the creek picked out
     * @param creekEyes where the creek's eyes are right now, to fling them away from it or turn
     *                  their head towards it
     * @param survivors every survivor of the round, to find the ones standing next to a frozen one
     */
    public void selected(Player selected, Supplier<Pos> creekEyes, Collection<Player> survivors) {
        if (this.random.nextBoolean() || !this.flingAway(selected, creekEyes.get())) {
            this.stun(selected, creekEyes, survivors);
        }
    }

    /**
     * Freezes the survivor for a moment with their head turned towards the creek, and blinds the
     * other survivors standing next to them.
     */
    void stun(Player selected, Supplier<Pos> creekEyes, Collection<Player> survivors) {
        CatchEffects.playScare(selected);
        this.effects.add(selected, new Potion(PotionEffect.SLOWNESS, STUN_AMPLIFIER, STUN_TICKS));
        this.faces.lock(selected, creekEyes, STUN_TICKS);
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
     * Takes back every effect handed out that is still running and lets go of every head still
     * held, so nothing lingers after the round.
     */
    public void cleanUp() {
        this.effects.removeAll();
        this.faces.cleanUp();
    }
}
