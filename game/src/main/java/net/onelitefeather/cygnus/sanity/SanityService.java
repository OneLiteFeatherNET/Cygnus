package net.onelitefeather.cygnus.sanity;

import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.onelitefeather.cygnus.GameFeature;
import net.onelitefeather.cygnus.common.config.SanityConfig;
import net.onelitefeather.cygnus.common.page.event.PageFoundEvent;
import net.onelitefeather.cygnus.creek.dread.CreekWitness;
import net.onelitefeather.cygnus.creek.dread.DreadSource;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.event.GameStartEvent;
import net.onelitefeather.cygnus.event.SlenderReviveEvent;
import net.onelitefeather.cygnus.utils.PlayerState;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;
import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Keeps track of how scared every survivor is.
 * <p>
 * Sanity is tracked as its inverse, fear: {@code 0} is calm, {@code 1} is terrified. A higher
 * value means less sanity, which is the direction the creek's thresholds read it in.
 * </p>
 * <p>
 * Fear jumps when a survivor finds a page, spots the creek, is picked out or caught by it, or
 * another survivor dies, and it grows steadily while a creek stalks them. It wears off again over time, but never
 * below a floor that grows with the pages found and the time played, so the round still gets
 * tenser towards the end. Every jump also leaves a share that never wears off. The floor and that
 * share together stay below {@link SanityConfig#floorCap()}, so only a fresh scare starts a hunt.
 * The creek reads it as its {@link DreadSource} and reports catches, pick-outs, sightings and
 * stalks back as its {@link CreekWitness}. The survivors' ambient sounds read
 * it too, to get more frequent the more scared a survivor is.
 * </p>
 * <p>
 * Like {@code SlenderGazeService}, it listens for the round's start and end on its own and drops a
 * survivor the moment they die or leave.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.1.0
 * @since 2.16.0
 */
public final class SanityService implements GameFeature, DreadSource, CreekWitness {

    private static final long NOT_STARTED = Long.MIN_VALUE;

    private final EventNode<Event> node = EventNode.all("sanity");

    private final SanityConfig config;
    private final DoubleSupplier pageProgress;
    private final long roundMillis;
    private final LongSupplier clock;
    private final Supplier<Set<Player>> roundSurvivors;
    private final PlayerState<Fear> survivors;
    private final SanityObserver observer;
    private final boolean observed;
    private volatile long startedAt = NOT_STARTED;

    /**
     * Sets up the service.
     *
     * @param config         the settings
     * @param pageProgress   supplies how much of all pages has been found, between {@code 0} and {@code 1}
     * @param roundMillis    how long a round lasts, in milliseconds
     * @param clock          supplies the current time in milliseconds
     * @param roundSurvivors supplies the survivors of the starting round
     */
    public SanityService(SanityConfig config, DoubleSupplier pageProgress, long roundMillis, LongSupplier clock,
                         Supplier<Set<Player>> roundSurvivors) {
        this(config, pageProgress, roundMillis, clock, roundSurvivors, SanityObserver.NONE);
    }

    /**
     * Creates the service with an observer for the jumps in fear.
     *
     * @param config         the settings
     * @param pageProgress   how far the pages are found, between 0 and 1
     * @param roundMillis    the length of a round in milliseconds
     * @param clock          the clock in milliseconds
     * @param roundSurvivors supplies the survivors of the starting round
     * @param observer       hears about every jump in fear
     * @since 2.15.0
     */
    public SanityService(SanityConfig config, DoubleSupplier pageProgress, long roundMillis, LongSupplier clock,
                         Supplier<Set<Player>> roundSurvivors, SanityObserver observer) {
        this.observer = observer;
        this.observed = observer != SanityObserver.NONE;
        this.config = config;
        this.pageProgress = pageProgress;
        this.roundMillis = roundMillis;
        this.clock = clock;
        this.roundSurvivors = roundSurvivors;
        this.survivors = new PlayerState<>();
        this.registerListeners();
    }

    @Override
    public EventNode<Event> node() {
        return this.node;
    }

    /**
     * Hooks the service into the round's lifecycle.
     */
    private void registerListeners() {
        this.node.addListener(GameStartEvent.class, _ -> {
            this.startedAt = this.clock.getAsLong();
            this.survivors.clear();
            for (Player survivor : this.roundSurvivors.get()) {
                this.track(survivor);
            }
        });
        this.node.addListener(PageFoundEvent.class, event -> this.pageFound(event.finder()));
        this.node.addListener(PlayerDeathEvent.class, event -> this.died(event.getPlayer()));
        this.node.addListener(PlayerDisconnectEvent.class, event -> this.survivors.remove(event.getPlayer()));
        // A survivor who takes over as the slender is no longer one of the scared.
        this.node.addListener(SlenderReviveEvent.class, event -> this.survivors.remove(event.getPlayer()));
        this.node.addListener(GameFinishEvent.class, _ -> {
            this.survivors.clear();
            this.startedAt = NOT_STARTED;
        });
    }

    /**
     * Starts tracking a survivor, calm.
     *
     * @param survivor the survivor to track
     */
    public void track(Player survivor) {
        Fear fear = new Fear(this.config.decayPerSecond(), this.config.residualShare(), this.config.floorCap(),
                this.clock.getAsLong());
        this.survivors.put(survivor, fear);
    }

    /**
     * A survivor has found a page. Only they are scared by it; everyone else feels it through the
     * floor.
     */
    private void pageFound(Player finder) {
        this.scare(finder.getUuid(), this.survivors.get(finder), this.config.pageFoundGain(), SanityObserver.SOURCE_PAGE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double dreadOf(UUID survivor) {
        double floor = this.floor();
        Fear fear = this.survivors.get(survivor);
        if (fear == null) return Math.min(this.config.floorCap(), floor);
        return fear.read(this.clock.getAsLong(), floor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sighted(UUID survivor) {
        Fear fear = this.survivors.get(survivor);
        if (fear == null) return;
        long now = this.clock.getAsLong();
        if (fear.trySighting(now, this.config.sightingCooldownSeconds() * 1000L)) {
            this.scare(survivor, fear, this.config.sightingGain(), SanityObserver.SOURCE_SIGHTING);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void caught(UUID survivor) {
        this.scare(survivor, this.survivors.get(survivor), this.config.caughtGain(), SanityObserver.SOURCE_CAUGHT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void selected(UUID survivor) {
        this.scare(survivor, this.survivors.get(survivor), this.config.selectedGain(), SanityObserver.SOURCE_SELECTED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stalked(UUID survivor) {
        Fear fear = this.survivors.get(survivor);
        if (fear == null) return;
        long now = this.clock.getAsLong();
        double floor = this.floor();
        double before = this.observed ? fear.read(now, floor) : 0.0D;
        fear.stalked(this.config.stalkGainPerSecond(), now, floor);
        if (this.observed) {
            this.observer.jumped(survivor, SanityObserver.SOURCE_STALK, before, fear.read(now, floor));
        }
    }

    /**
     * Scares every other survivor when one of them dies, then stops tracking the dead one. Nobody
     * has to be close: on a big map a radius would leave most deaths unnoticed.
     */
    private void died(Player dead) {
        if (this.survivors.remove(dead) == null) return;
        this.survivors.forEach((id, fear) -> this.scare(id, fear, this.config.deathGain(), SanityObserver.SOURCE_DEATH));
    }

    private void scare(UUID id, @Nullable Fear fear, double gain, String source) {
        if (fear == null) return;
        long now = this.clock.getAsLong();
        double floor = this.floor();
        // Read around the jump only when somebody listens: the add settles the value first anyway, so
        // the extra read changes nothing about the fear itself.
        double before = this.observed ? fear.read(now, floor) : 0.0D;
        fear.add(gain, now, floor);
        if (this.observed) {
            this.observer.jumped(id, source, before, fear.read(now, floor));
        }
    }

    /**
     * The floor everyone shares, before the cap: it grows with the pages found and the time played.
     */
    private double floor() {
        return this.config.pageFloorWeight() * this.pageProgress.getAsDouble()
                + this.config.timeFloorWeight() * this.roundProgress();
    }

    /**
     * How much of the round's time is up: {@code 0} before the round starts, {@code 1} at its end.
     */
    private double roundProgress() {
        long started = this.startedAt;
        if (started == NOT_STARTED || this.roundMillis <= 0) return 0.0D;
        return Math.clamp((double) (this.clock.getAsLong() - started) / this.roundMillis, 0.0D, 1.0D);
    }
}
