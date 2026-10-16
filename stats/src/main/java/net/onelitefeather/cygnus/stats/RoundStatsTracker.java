package net.onelitefeather.cygnus.stats;

import net.onelitefeather.cygnus.stats.RoundStats.Role;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Keeps the per-round bookkeeping of the statistics: who takes part, what each participant did, who
 * quit, and how the round resolved for everyone when it ended.
 * <p>
 * The tracker knows nothing about the game or its events, it only works on plain values, and it only
 * touches an in-memory map, so feeding it never does I/O. Only players handed to
 * {@link #roundStarted(Map)} are tracked; anyone else gets no row.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.16.0
 */
public final class RoundStatsTracker {

    private final Clock clock;
    private final ZoneId zone;
    private final Map<UUID, RoundStats.Builder> participants = new HashMap<>();

    /**
     * Creates the tracker.
     *
     * @param clock the source of every timestamp
     * @param zone  the zone whose calendar decides the year of a round
     */
    public RoundStatsTracker(Clock clock, ZoneId zone) {
        this.clock = clock;
        this.zone = zone;
    }

    /**
     * Starts tracking the given players. A round still tracked from before is dropped.
     *
     * @param players every participant with the role they start in
     */
    public synchronized void roundStarted(Map<UUID, Role> players) {
        this.participants.clear();
        Instant started = this.clock.instant();
        players.forEach((uuid, role) -> this.participants.put(uuid, new RoundStats.Builder(uuid, role, started)));
    }

    /**
     * Counts a found page for its finder.
     *
     * @param finder the survivor who found it
     * @param found  how many pages were found including this one
     * @param max    how many pages the round needs
     */
    public synchronized void pageFound(UUID finder, int found, int max) {
        RoundStats.Builder builder = active(finder);
        if (builder != null) {
            builder.pageFound(found >= max);
        }
    }

    /**
     * Records that a survivor became the Slender because the original one left. The round row keeps
     * the start role and only gets the takeover flag.
     *
     * @param player the survivor who took over
     */
    public synchronized void slenderTakenOver(UUID player) {
        RoundStats.Builder builder = active(player);
        if (builder != null) {
            builder.takenOver();
        }
    }

    /**
     * Records a death and credits the kill.
     *
     * @param victim the player who died
     * @param killer the Slender who gets the credit, or {@code null} for nobody
     */
    public synchronized void playerDied(UUID victim, @Nullable UUID killer) {
        RoundStats.Builder dead = active(victim);
        if (dead == null || dead.role() != Role.SURVIVOR) {
            return;
        }
        dead.died(this.clock.instant());
        if (killer != null) {
            RoundStats.Builder credited = active(killer);
            if (credited != null) {
                credited.kill();
            }
        }
    }

    /**
     * Adds damage a participant took.
     *
     * @param player the player who was hit
     * @param points the vitality points lost
     */
    public synchronized void damageTaken(UUID player, double points) {
        RoundStats.Builder builder = active(player);
        if (builder != null) {
            builder.damage(points);
        }
    }

    /**
     * Counts a painkiller a participant took.
     *
     * @param player the player who took it
     */
    public synchronized void painkillerUsed(UUID player) {
        RoundStats.Builder builder = active(player);
        if (builder != null) {
            builder.painkiller();
        }
    }

    /**
     * Freezes a participant who quit before the round ended: their outcome is
     * {@link RoundStats.Outcome#ABANDONED} and their play time ends now.
     *
     * @param player the player who left
     */
    public synchronized void playerLeft(UUID player) {
        RoundStats.Builder builder = active(player);
        if (builder != null) {
            builder.left(this.clock.instant());
        }
    }

    /**
     * Ends the round: resolves every outcome and clears the state.
     *
     * @param reason  why the round ended
     * @param roundId reads the id the finished round is stored under, only asked when a round was tracked
     * @return the finished round, or empty when no round was being tracked
     */
    public synchronized Optional<FinishedRound> finish(FinishReason reason, Supplier<UUID> roundId) {
        if (this.participants.isEmpty()) {
            return Optional.empty();
        }
        Instant finished = this.clock.instant();
        int pagesAgainst = this.participants.values().stream()
                .filter(builder -> builder.role() == Role.SURVIVOR)
                .mapToInt(RoundStats.Builder::pagesFound)
                .sum();
        int lobbySize = this.participants.size();
        List<RoundStats> stats = new ArrayList<>(lobbySize);
        this.participants.values().forEach(builder -> stats.add(builder.build(reason, finished, pagesAgainst, lobbySize)));
        this.participants.clear();
        return Optional.of(new FinishedRound(roundId.get(), finished, finished.atZone(this.zone).getYear(), stats));
    }

    /**
     * Reads a participant who is still counting: one who is not tracked or who quit counts nothing.
     */
    private RoundStats.@Nullable Builder active(UUID player) {
        RoundStats.Builder builder = this.participants.get(player);
        return builder == null || builder.frozen() ? null : builder;
    }
}
