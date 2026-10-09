package net.onelitefeather.cygnus.creek;

import net.minestom.server.coordinate.Pos;
import net.onelitefeather.cygnus.common.config.CreekConfig;
import net.onelitefeather.cygnus.creek.state.CreekState;
import net.onelitefeather.cygnus.creek.state.DoneState;
import net.onelitefeather.cygnus.creek.state.StalkState;
import net.onelitefeather.cygnus.creek.state.SurvivorView;
import net.onelitefeather.cygnus.creek.world.SpotFinder;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

/**
 * The creek's variants: a personal creek for each haunted survivor, one that nobody else sees.
 * <p>
 * The survivor with the highest dread gets the next one. It starts by stalking them and is gone
 * once the stalk or the hunt is over. The more survivors there are, the more of them can be
 * haunted at once, see {@link #capacity(int)}. After a variant, the survivor gets a breather before
 * the next one.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
final class CreekVariants {

    /** One variant for every this many survivors. */
    static final int SURVIVORS_PER_VARIANT = 4;

    /** Never more variants than this at once, however big the round. */
    static final int MAX_VARIANTS = 3;

    private final CreekConfig config;
    private final SpotFinder spots;
    private final RandomGenerator random;
    private final long allowedAt;
    private final BiFunction<Pos, CreekState, Creek> spawner;
    private final Map<UUID, Creek> running;
    private final Map<UUID, Long> cooldowns;

    /**
     * Sets up the variants for a round.
     *
     * @param config    the settings
     * @param spots     finds a spot next to a survivor, out of their view
     * @param random    the random source
     * @param allowedAt the earliest time a variant may show up, in milliseconds
     * @param spawner   creates a variant's creek at a spot, starting in the given state
     */
    CreekVariants(CreekConfig config, SpotFinder spots, RandomGenerator random, long allowedAt,
                  BiFunction<Pos, CreekState, Creek> spawner) {
        this.config = config;
        this.spots = spots;
        this.random = random;
        this.allowedAt = allowedAt;
        this.spawner = spawner;
        this.running = new LinkedHashMap<>();
        this.cooldowns = new HashMap<>();
    }

    /**
     * How many survivors can be haunted at once: one per {@link #SURVIVORS_PER_VARIANT}
     * survivors, rounded up, and never more than {@link #MAX_VARIANTS}.
     *
     * @param survivors the number of survivors still alive
     * @return how many variants may run at once
     */
    static int capacity(int survivors) {
        return Math.min(MAX_VARIANTS, (survivors + SURVIVORS_PER_VARIANT - 1) / SURVIVORS_PER_VARIANT);
    }

    /**
     * How long a survivor's breather after a variant lasts: {@code vanishMaxSeconds} for a calm
     * survivor, down to {@code vanishMinSeconds} for one who is scared to death.
     *
     * @param config the settings
     * @param dread  the survivor's dread
     * @return the breather in milliseconds
     */
    static long cooldownMillis(CreekConfig config, double dread) {
        double span = config.vanish().maxSeconds() - config.vanish().minSeconds();
        double seconds = config.vanish().maxSeconds() - span * Math.clamp(dread, 0.0D, 1.0D);
        return Math.round(seconds * 1000.0D);
    }

    /**
     * Picks who to haunt next: the survivor with the highest dread, as long as it reaches the
     * threshold. If two are equally scared, the one who strayed farthest from the others.
     *
     * @param views     every survivor
     * @param threshold the lowest dread that counts
     * @param eligible  whether a survivor may get a variant right now
     * @return the survivor, or empty if nobody fits
     */
    static Optional<SurvivorView> pick(List<SurvivorView> views, double threshold, Predicate<UUID> eligible) {
        return views.stream()
                .filter(view -> view.dread() >= threshold && eligible.test(view.id()))
                .max(Comparator.comparingDouble(SurvivorView::dread)
                        .thenComparingDouble(view -> nearestOther(views, view)));
    }

    /**
     * Runs one step: lets every variant act, clears away the finished ones and starts new ones
     * while there is room.
     *
     * @param survivors the survivors of this step
     * @param now       the current time in milliseconds
     */
    void tick(SurvivorSnapshot survivors, long now) {
        List<SurvivorView> views = survivors.views();
        this.tickRunning(survivors, now);
        if (now < this.allowedAt) return;
        // Someone with no free spot next to them should not hold up everyone behind them.
        Set<UUID> noSpot = new HashSet<>();
        int free = capacity(views.size()) - this.running.size();
        while (free > 0) {
            Optional<SurvivorView> target = pick(views, this.config.stalk().threshold(),
                    id -> this.isFree(id, now) && !noSpot.contains(id));
            if (target.isEmpty()) return;
            if (this.start(target.get(), now)) {
                free--;
            } else {
                noSpot.add(target.get().id());
            }
        }
    }

    /**
     * Tells which survivors are haunted right now, and by which creek.
     *
     * @return a copy, keyed by the haunted survivor
     */
    Map<UUID, Creek> running() {
        return Map.copyOf(this.running);
    }

    /**
     * Removes every variant and forgets every breather.
     */
    void stop() {
        for (Creek variant : this.running.values()) {
            variant.remove();
        }
        this.running.clear();
        this.cooldowns.clear();
    }

    private void tickRunning(SurvivorSnapshot survivors, long now) {
        Iterator<Map.Entry<UUID, Creek>> iterator = this.running.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Creek> entry = iterator.next();
            Creek variant = entry.getValue();
            variant.tick(survivors, now);
            if (!(variant.state() instanceof DoneState)) continue;
            variant.remove();
            iterator.remove();
            long cooldown = cooldownMillis(this.config, dreadOf(survivors.views(), entry.getKey()));
            this.cooldowns.put(entry.getKey(), now + cooldown);
        }
    }

    private boolean isFree(UUID id, long now) {
        return !this.running.containsKey(id) && this.cooldowns.getOrDefault(id, Long.MIN_VALUE) <= now;
    }

    /**
     * Places a new variant next to the survivor, out of their view.
     *
     * @return {@code false} if there was no free spot next to them in this step
     */
    private boolean start(SurvivorView view, long now) {
        Optional<Pos> spot = this.spots.beside(view.position(), config.stalk().minDistance(), config.stalk().maxDistance(),
                config.stalk().minAngle(), config.stalk().maxAngle(), List.of(view.eyes()), config.personalSpace(),
                this.random);
        if (spot.isEmpty()) return false;

        StalkState stalk = StalkState.starting(view.id(), now, this.config, this.random);
        this.running.put(view.id(), this.spawner.apply(spot.get(), stalk));
        return true;
    }

    private static double dreadOf(List<SurvivorView> views, UUID id) {
        for (SurvivorView view : views) {
            if (view.id().equals(id)) return view.dread();
        }
        return 0.0D;
    }

    private static double nearestOther(List<SurvivorView> views, SurvivorView view) {
        double nearest = Double.MAX_VALUE;
        for (SurvivorView other : views) {
            if (other.id().equals(view.id())) continue;
            nearest = Math.min(nearest, other.position().distance(view.position()));
        }
        return nearest;
    }
}
