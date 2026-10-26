package net.onelitefeather.cygnus.pass;

import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.common.config.SlenderPassConfig;
import net.onelitefeather.cygnus.stats.SlenderPassRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * Credits the monthly quota of a player's rank once per calendar month, when the player is in the lobby.
 * <p>
 * The month is the UTC month of the injected clock. The repository decides whether this month is still to be credited,
 * so a second lobby join in the same month credits nothing, and a month that was missed is not paid back. The quota is
 * read on the tick thread from the cached LuckPerms data; the database write runs off it.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.21.0
 */
public final class MonthlyQuota {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonthlyQuota.class);

    private final SlenderPassRepository repository;
    private final Clock clock;
    private final MonthlyQuotaSource source;
    private final SlenderPassConfig config;
    private final Executor tick;
    private final PassFeedback feedback;

    /**
     * Reads the quota of a player. Implementations must not block.
     */
    @FunctionalInterface
    public interface MonthlyQuotaSource {

        /**
         * Reads the quota.
         *
         * @param player the player
         * @return the number of passes per month, {@code 0} for none
         */
        int monthlyQuota(UUID player);
    }

    /**
     * Creates the quota.
     *
     * @param repository where the passes are stored
     * @param clock      the source of the month, read in UTC
     * @param source     reads the quota of a player
     * @param config     the cap
     * @param tick       runs the announcement on the tick thread
     * @param feedback   delivers the announcement
     */
    public MonthlyQuota(SlenderPassRepository repository, Clock clock, MonthlyQuotaSource source, SlenderPassConfig config,
                        Executor tick, PassFeedback feedback) {
        this.repository = repository;
        this.clock = clock;
        this.source = source;
        this.config = config;
        this.tick = tick;
        this.feedback = feedback;
    }

    /**
     * Credits the quota of a player who is in the lobby. Called on the tick thread.
     *
     * @param player the player
     */
    public void onLobbyJoin(UUID player) {
        YearMonth month = YearMonth.from(this.clock.instant().atZone(ZoneOffset.UTC));
        this.repository.grantQuota(player, month, this.source.monthlyQuota(player), this.config.maxBalance())
                .thenAcceptAsync(credit -> {
                    if (credit.delta() > 0) {
                        this.feedback.tell(player, Messages.passEarnedQuota(credit.balance()));
                    }
                }, this.tick)
                .exceptionally(error -> {
                    LOGGER.warn("Crediting the monthly slender pass quota of {} failed", player, error);
                    return null;
                });
    }
}
