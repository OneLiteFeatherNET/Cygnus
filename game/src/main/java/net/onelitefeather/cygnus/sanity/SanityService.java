package net.onelitefeather.cygnus.sanity;

import net.minestom.server.coordinate.Pos;
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
 * Fear jumps when a survivor finds a page, spots the creek, is caught by it, or someone dies close
 * by. It wears off again over time, but never below a floor that grows with the pages found, so
 * the round still gets tenser towards the end. The creek reads it as its {@link DreadSource} and
 * reports catches and sightings back as its {@link CreekWitness}. The survivors' ambient sounds read
 * it too, to get more frequent the more scared a survivor is.
 * </p>
 * <p>
 * Like {@code SlenderGazeService}, it listens for the round's start and end on its own and drops a
 * survivor the moment they die or leave.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.16.0
 */
public final class SanityService implements GameFeature, DreadSource, CreekWitness {

    private final EventNode<Event> node = EventNode.all("sanity");

    private final SanityConfig config;
    private final DoubleSupplier pageProgress;
    private final LongSupplier clock;
    private final Supplier<Set<Player>> roundSurvivors;
    private final PlayerState<Tracked> survivors;

    /**
     * Sets up the service.
     *
     * @param config         the settings
     * @param pageProgress   supplies how much of all pages has been found, between {@code 0} and {@code 1}
     * @param clock          supplies the current time in milliseconds
     * @param roundSurvivors supplies the survivors of the starting round
     */
    public SanityService(SanityConfig config, DoubleSupplier pageProgress, LongSupplier clock,
                         Supplier<Set<Player>> roundSurvivors) {
        this.config = config;
        this.pageProgress = pageProgress;
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
        this.node.addListener(GameFinishEvent.class, _ -> this.survivors.clear());
    }

    /**
     * Starts tracking a survivor, calm.
     *
     * @param survivor the survivor to track
     */
    public void track(Player survivor) {
        this.survivors.put(survivor, new Tracked(survivor, new Fear(this.config.decayPerSecond(), this.clock.getAsLong())));
    }

    /**
     * A survivor has found a page. Only they are scared by it; everyone else feels it through the
     * floor.
     */
    private void pageFound(Player finder) {
        this.scare(this.survivors.get(finder), this.config.pageFoundGain());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double dreadOf(UUID survivor) {
        double floor = this.floor();
        Tracked tracked = this.survivors.get(survivor);
        if (tracked == null) return floor;
        return tracked.fear().read(this.clock.getAsLong(), floor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sighted(UUID survivor) {
        Tracked tracked = this.survivors.get(survivor);
        if (tracked == null) return;
        long now = this.clock.getAsLong();
        if (tracked.fear().trySighting(now, this.config.sightingCooldownSeconds() * 1000L)) {
            tracked.fear().add(this.config.sightingGain(), now, this.floor());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void caught(UUID survivor) {
        this.scare(this.survivors.get(survivor), this.config.caughtGain());
    }

    /**
     * Scares everyone close to a survivor who died, then stops tracking them.
     */
    private void died(Player dead) {
        Tracked gone = this.survivors.remove(dead);
        if (gone == null) return;
        Pos where = dead.getPosition();
        double radius = this.config.deathRadius();
        for (Tracked tracked : this.survivors.values()) {
            Player survivor = tracked.player();
            if (survivor.getInstance() != dead.getInstance()) continue;
            if (survivor.getPosition().distance(where) > radius) continue;
            this.scare(tracked, this.config.deathGain());
        }
    }

    private void scare(@Nullable Tracked tracked, double gain) {
        if (tracked == null) return;
        tracked.fear().add(gain, this.clock.getAsLong(), this.floor());
    }

    private double floor() {
        return this.config.pageFloorWeight() * this.pageProgress.getAsDouble();
    }

    /**
     * A tracked survivor and their fear. The player is kept to know where they are when someone
     * dies close by.
     */
    private record Tracked(Player player, Fear fear) {
    }
}
