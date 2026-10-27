package net.onelitefeather.cygnus.pass;

import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.common.config.SlenderPassConfig;
import net.onelitefeather.cygnus.stats.FinishedRound;
import net.onelitefeather.cygnus.stats.RoundStats;
import net.onelitefeather.cygnus.stats.RoundStats.Outcome;
import net.onelitefeather.cygnus.stats.RoundStats.Role;
import net.onelitefeather.cygnus.stats.SlenderPassRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * Counts the Survivor rounds of every player and credits a pass when a player reaches the pity threshold.
 * <p>
 * The input is the snapshot the statistics build when a round ends, so "a Survivor who played to the end" means the
 * same here as in the statistics. A player who was the Slender in the round, by any path, or who was taken over, starts
 * the count again. A player who quit before the end is not counted at all. The repository counts in one transaction;
 * the credits are announced on the tick thread.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.21.0
 */
public final class PityCounter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PityCounter.class);

    private final SlenderPassRepository repository;
    private final SlenderPassConfig config;
    private final Executor tick;
    private final PassFeedback feedback;

    /**
     * Creates the counter.
     *
     * @param repository where the counters and the passes are stored
     * @param config     the threshold and the cap
     * @param tick       runs the announcements on the tick thread
     * @param feedback   delivers the announcements
     */
    public PityCounter(SlenderPassRepository repository, SlenderPassConfig config, Executor tick, PassFeedback feedback) {
        this.repository = repository;
        this.config = config;
        this.tick = tick;
        this.feedback = feedback;
    }

    /**
     * Counts a finished round. Called on the tick thread; the database write runs off it.
     *
     * @param round the round the statistics have just snapshotted
     */
    public void roundFinished(FinishedRound round) {
        List<UUID> survivors = new ArrayList<>();
        List<UUID> slenders = new ArrayList<>();
        for (RoundStats stats : round.players()) {
            if (stats.role() == Role.SLENDER || stats.takenOver()) {
                slenders.add(stats.playerUuid());
            } else if (stats.outcome() != Outcome.ABANDONED) {
                survivors.add(stats.playerUuid());
            }
        }
        this.repository.recordRounds(survivors, slenders, this.config.pityRounds(), this.config.maxBalance())
                .thenAcceptAsync(credits -> credits.forEach((player, credit) ->
                        this.feedback.tell(player, Messages.passEarnedPity(credit.balance()))), this.tick)
                .exceptionally(error -> {
                    LOGGER.warn("Counting the Survivor rounds of round {} failed", round.roundId(), error);
                    return null;
                });
    }
}
