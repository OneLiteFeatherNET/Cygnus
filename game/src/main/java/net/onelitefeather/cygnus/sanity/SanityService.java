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
import net.onelitefeather.cygnus.utils.RepeatingTask;
import org.jetbrains.annotations.Nullable;

import java.time.temporal.ChronoUnit;
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
 * Each survivor sees their own sanity as their level, from 100 when calm down to 0 when terrified.
 * The bar below it stays the sprint bar.
 * </p>
 * <p>
 * Like {@code SlenderGazeService}, it listens for the round's start and end on its own and drops a
 * survivor the moment they die or leave.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.2.0
 * @since 2.16.0
 */
public final class SanityService implements GameFeature, DreadSource, CreekWitness {

    private static final long NOT_STARTED = Long.MIN_VALUE;

    /** How often the survivors' sanity is put on their level display. */
    static final long DISPLAY_MILLIS = 250L;

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
    private final RepeatingTask display = new RepeatingTask(this::showSanity);

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
            this.display.start(DISPLAY_MILLIS, ChronoUnit.MILLIS);
        });
        this.node.addListener(PageFoundEvent.class, event -> this.pageFound(event.finder()));
        this.node.addListener(PlayerDeathEvent.class, event -> this.died(event.getPlayer()));
        this.node.addListener(PlayerDisconnectEvent.class, event -> this.survivors.remove(event.getPlayer()));
        // A survivor who takes over as the slender is no longer one of the scared.
        this.node.addListener(SlenderReviveEvent.class, event -> this.forget(event.getPlayer()));
        this.node.addListener(GameFinishEvent.class, _ -> {
            this.display.stop();
            for (Player survivor : this.roundSurvivors.get()) {
                this.forget(survivor);
            }
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
        return this.dreadOf(survivor, this.clock.getAsLong());
    }

    /**
     * How scared a survivor is at the given time. The fear and the floor are both read at that
     * time, so one call never mixes two moments.
     */
    private double dreadOf(UUID survivor, long now) {
        double floor = this.floor(now);
        Fear fear = this.survivors.get(survivor);
        if (fear == null) return Math.min(this.config.floorCap(), floor);
        return fear.read(now, floor);
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
        double floor = this.floor(now);
        double before = this.observed ? fear.read(now, floor) : 0.0D;
        fear.stalked(this.config.stalkGainPerSecond(), now, floor);
        if (this.observed) {
            this.observer.jumped(survivor, SanityObserver.SOURCE_STALK, before, fear.read(now, floor));
        }
    }

    /**
     * Scares a survivor whom a dead teammate's figure has just struck.
     *
     * @param survivor the survivor who was lured
     * @param gain     how much the fear jumps, between 0 and 1
     * @since 2.21.0
     */
    public void lured(UUID survivor, double gain) {
        this.scare(survivor, this.survivors.get(survivor), gain, SanityObserver.SOURCE_LURE);
    }

    /**
     * Scares every other survivor when one of them dies, then stops tracking the dead one. Nobody
     * has to be close: on a big map a radius would leave most deaths unnoticed.
     */
    private void died(Player dead) {
        if (!this.forget(dead)) return;
        this.survivors.forEach((id, fear) -> this.scare(id, fear, this.config.deathGain(), SanityObserver.SOURCE_DEATH));
    }

    private void scare(UUID id, @Nullable Fear fear, double gain, String source) {
        if (fear == null) return;
        long now = this.clock.getAsLong();
        double floor = this.floor(now);
        // Read around the jump only when somebody listens: the add settles the value first anyway, so
        // the extra read changes nothing about the fear itself.
        double before = this.observed ? fear.read(now, floor) : 0.0D;
        fear.add(gain, now, floor);
        if (this.observed) {
            this.observer.jumped(id, source, before, fear.read(now, floor));
        }
    }

    /**
     * Puts every tracked survivor's sanity on their level display. Only a changed value is sent, so
     * the client is not told the same number four times a second.
     */
    void showSanity() {
        long now = this.clock.getAsLong();
        for (Player survivor : this.roundSurvivors.get()) {
            if (this.survivors.get(survivor) == null) continue;
            int level = sanityLevel(this.dreadOf(survivor.getUuid(), now));
            if (survivor.getLevel() != level) {
                survivor.setLevel(level);
            }
        }
    }

    /**
     * Turns a dread into the sanity a survivor sees: 100 when calm, 0 when terrified.
     *
     * @param dread how scared the survivor is, between {@code 0} and {@code 1}
     * @return the sanity, between {@code 0} and {@code 100}
     */
    static int sanityLevel(double dread) {
        return (int) Math.round(100.0D * (1.0D - Math.clamp(dread, 0.0D, 1.0D)));
    }

    /**
     * Stops following a survivor and takes the sanity off their level display.
     *
     * @param player the survivor
     * @return {@code true} if the survivor was followed until now
     */
    private boolean forget(Player player) {
        if (this.survivors.remove(player) == null) return false;
        player.setLevel(0);
        return true;
    }

    /**
     * The floor everyone shares, before the cap: it grows with the pages found and the time played.
     */
    private double floor(long now) {
        return this.config.pageFloorWeight() * this.pageProgress.getAsDouble()
                + this.config.timeFloorWeight() * this.roundProgress(now);
    }

    /**
     * How much of the round's time is up: {@code 0} before the round starts, {@code 1} at its end.
     */
    private double roundProgress(long now) {
        long started = this.startedAt;
        if (started == NOT_STARTED || this.roundMillis <= 0) return 0.0D;
        return Math.clamp((double) (now - started) / this.roundMillis, 0.0D, 1.0D);
    }
}
