package net.onelitefeather.cygnus.creek;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.onelitefeather.cygnus.creek.body.CreekBody;
import net.onelitefeather.cygnus.creek.consequence.CreekScare;
import net.onelitefeather.cygnus.creek.world.CreekSight;
import net.onelitefeather.cygnus.creek.world.SpotFinder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.LongSupplier;

/**
 * The trickster's doppelgangers: creek bodies that never act, each seen by one survivor only.
 * <p>
 * Once its survivor has seen a doppelganger for {@code revealMillis} in a row, with nothing in the
 * way, it crumbles to leaves with the creek's scare sound. It is also gone once its survivor is
 * gone, or after {@link #LIFETIME_MILLIS}. Everything here runs in the creek's step.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.21.0
 */
final class Decoys {

    /** How long a doppelganger stands at the most, in milliseconds. */
    static final long LIFETIME_MILLIS = 20_000L;

    /** How many leaves it crumbles into. */
    static final int LEAF_COUNT = 20;

    private final BiFunction<Instance, Pos, CreekBody> bodies;
    private final CreekSight sight;
    private final LongSupplier revealMillis;
    private final LongSupplier clock;
    private final List<Decoy> running = new ArrayList<>();

    /**
     * Sets up the doppelgangers of a round.
     *
     * @param bodies       puts a creek body into the world
     * @param sight        decides whether a survivor sees a doppelganger
     * @param revealMillis how long a survivor has to look at one before it crumbles, the same as
     *                     for the real creek right now
     * @param clock        supplies the current time in milliseconds
     */
    Decoys(BiFunction<Instance, Pos, CreekBody> bodies, CreekSight sight, LongSupplier revealMillis, LongSupplier clock) {
        this.bodies = bodies;
        this.sight = sight;
        this.revealMillis = revealMillis;
        this.clock = clock;
    }

    /**
     * Puts a doppelganger into the world that only the given survivor sees.
     *
     * @param viewer the survivor who sees it
     * @param where  where it stands
     */
    void spawn(Player viewer, Pos where) {
        Instance instance = viewer.getInstance();
        if (instance == null) return;
        CreekBody body = this.bodies.apply(instance, where);
        body.showTo(Set.of(viewer.getUuid()));
        body.setFrozen(true);
        body.lookAt(viewer.getPosition().add(0, viewer.getEyeHeight(), 0));
        this.running.add(new Decoy(viewer.getUuid(), body, this.clock.getAsLong()));
    }

    /**
     * Lets every doppelganger crumble that was looked at long enough, and clears away the ones
     * whose survivor is gone or whose time is up.
     *
     * @param survivors the survivors of this step
     * @param now       the current time in milliseconds
     */
    void tick(SurvivorSnapshot survivors, long now) {
        Iterator<Decoy> iterator = this.running.iterator();
        while (iterator.hasNext()) {
            Decoy decoy = iterator.next();
            Player viewer = survivors.player(decoy.viewer);
            if (viewer == null || now - decoy.bornAt >= LIFETIME_MILLIS) {
                decoy.body.remove();
                iterator.remove();
                continue;
            }
            if (!this.sight.sees(viewer, decoy.body.entity())) {
                decoy.seenSince = -1L;
                continue;
            }
            if (decoy.seenSince < 0) decoy.seenSince = now;
            if (now - decoy.seenSince < this.revealMillis.getAsLong()) continue;
            crumble(viewer, decoy.body.position());
            decoy.body.remove();
            iterator.remove();
        }
    }

    /**
     * Takes every doppelganger out of the world.
     */
    void clear() {
        for (Decoy decoy : this.running) {
            decoy.body.remove();
        }
        this.running.clear();
    }

    private static void crumble(Player viewer, Pos where) {
        Pos centre = where.add(0, SpotFinder.BODY_CENTRE, 0);
        viewer.sendPacket(new ParticlePacket(Particle.PALE_OAK_LEAVES, centre, new Vec(0.4D, 0.8D, 0.4D), 0.05F, LEAF_COUNT));
        CreekScare.play(viewer);
    }

    private static final class Decoy {

        private final UUID viewer;
        private final CreekBody body;
        private final long bornAt;
        private long seenSince = -1L;

        private Decoy(UUID viewer, CreekBody body, long bornAt) {
            this.viewer = viewer;
            this.body = body;
            this.bornAt = bornAt;
        }
    }
}
