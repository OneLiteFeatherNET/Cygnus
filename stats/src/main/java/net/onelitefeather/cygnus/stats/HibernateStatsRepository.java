package net.onelitefeather.cygnus.stats;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.OptimisticLockException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StaleStateException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Stores finished rounds through Hibernate.
 * <p>
 * A round is one transaction: the round rows are inserted and every participant's year row is loaded
 * or created and merged. The write runs on the injected executor, so the tick thread only queues it.
 * Several Cygnus servers write to one database, so two rounds can touch the same year row at once. The
 * {@code version} column turns a lost race into an {@link OptimisticLockException}, a year row another
 * server created first into a unique-key violation; both roll the transaction back and it is tried again,
 * up to {@value #MAX_ATTEMPTS} times. The round rows themselves cannot collide, every round id is unique.
 * The last known name of every participant is upserted in the same transaction, and the merged year rows
 * are copied into views before the commit that the future hands out afterwards.
 * </p>
 * <p>
 * A write that still fails is logged with the round id and counted in the failure metric; it never
 * throws into the caller, the returned future completes exceptionally instead. The reads run on the same
 * executor and fail their future without a log line, the caller decides what to tell the player. Hibernate runs with {@code hbm2ddl.auto=validate} against a schema Flyway owns.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.18.0
 */
public final class HibernateStatsRepository implements StatsRepository {

    /** How often a round write is tried before it counts as failed. */
    public static final int MAX_ATTEMPTS = 3;

    private static final Logger LOGGER = LoggerFactory.getLogger(HibernateStatsRepository.class);
    private static final long DRAIN_TIMEOUT_SECONDS = 15L;

    private final SessionFactory sessionFactory;
    private final Executor executor;
    private final StatsMetrics metrics;
    private final Runnable afterClose;
    private final Runnable afterYearRowsLoaded;

    /**
     * Creates the repository.
     *
     * @param sessionFactory the Hibernate session factory, closed with the repository
     * @param executor       runs the writes; an {@link ExecutorService} is drained on {@link #close()}
     * @param metrics        counts failed writes
     * @param afterClose     runs after the session factory closed, to release the connection pool
     */
    public HibernateStatsRepository(SessionFactory sessionFactory, Executor executor, StatsMetrics metrics,
                                    Runnable afterClose) {
        this(sessionFactory, executor, metrics, afterClose, () -> {
        });
    }

    /**
     * Creates the repository with a hook the tests use to play the other server.
     *
     * @param afterYearRowsLoaded runs in every attempt after the year rows were read and before the commit
     */
    HibernateStatsRepository(SessionFactory sessionFactory, Executor executor, StatsMetrics metrics,
                             Runnable afterClose, Runnable afterYearRowsLoaded) {
        this.sessionFactory = sessionFactory;
        this.executor = executor;
        this.metrics = metrics;
        this.afterClose = afterClose;
        this.afterYearRowsLoaded = afterYearRowsLoaded;
    }

    /**
     * Builds the session factory on a data source. The schema is validated, never changed: a missing
     * migration fails here, at startup, and not at the first write.
     *
     * @param dataSource the pool, already migrated by Flyway
     * @return the session factory
     */
    public static SessionFactory buildSessionFactory(DataSource dataSource) {
        return buildSessionFactory(dataSource, null);
    }

    /**
     * Builds the session factory on a data source, validating the tables in the given schema.
     *
     * @param dataSource the pool, already migrated by Flyway
     * @param schema     the schema that holds the tables, {@code null} for the connection's default
     * @return the session factory
     */
    public static SessionFactory buildSessionFactory(DataSource dataSource, @Nullable String schema) {
        StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder();
        if (schema != null) {
            builder.applySetting(AvailableSettings.DEFAULT_SCHEMA, schema);
        }
        StandardServiceRegistry registry = builder
                .applySetting(AvailableSettings.JAKARTA_NON_JTA_DATASOURCE, dataSource)
                .applySetting(AvailableSettings.HBM2DDL_AUTO, "validate")
                .build();
        try {
            return new MetadataSources(registry)
                    .addAnnotatedClass(PlayerRoundStats.class)
                    .addAnnotatedClass(PlayerYearStats.class)
                    .addAnnotatedClass(PlayerName.class)
                    .addAnnotatedClass(SlenderPassBalance.class)
                    .addAnnotatedClass(SlenderPassEntry.class)
                    .buildMetadata()
                    .buildSessionFactory();
        } catch (RuntimeException exception) {
            StandardServiceRegistryBuilder.destroy(registry);
            throw exception;
        }
    }

    /**
     * Queues the write on the executor and returns at once.
     *
     * @param round the finished round
     * @return completes after the commit with the merged year row of every participant; a write that
     * still failed after the retries, or that the executor refused, completes it exceptionally after it
     * was logged and counted
     */
    @Override
    public CompletableFuture<Map<UUID, YearStatsView>> saveRound(FinishedRound round) {
        CompletableFuture<Map<UUID, YearStatsView>> result = new CompletableFuture<>();
        try {
            this.executor.execute(() -> {
                try {
                    result.complete(write(round));
                } catch (RuntimeException exception) {
                    fail(round, exception);
                    result.completeExceptionally(exception);
                }
            });
        } catch (RejectedExecutionException exception) {
            fail(round, exception);
            result.completeExceptionally(exception);
        }
        return result;
    }

    private Map<UUID, YearStatsView> write(FinishedRound round) {
        for (int attempt = 1; ; attempt++) {
            try {
                return this.sessionFactory.fromTransaction(session -> mergeRound(session, round));
            } catch (RuntimeException exception) {
                if (attempt < MAX_ATTEMPTS && conflict(exception)) {
                    LOGGER.debug("Statistics of round {} collided with another write, attempt {}", round.roundId(), attempt);
                    continue;
                }
                throw exception;
            }
        }
    }

    private Map<UUID, YearStatsView> mergeRound(Session session, FinishedRound round) {
        round.players().forEach(stats -> session.persist(new PlayerRoundStats(round, stats)));
        List<PlayerYearStats> years = new ArrayList<>();
        for (RoundStats stats : round.players()) {
            PlayerYearStats.Key key = new PlayerYearStats.Key(stats.playerUuid(), round.year());
            PlayerYearStats year = session.find(PlayerYearStats.class, key);
            if (year == null) {
                year = new PlayerYearStats(key);
                session.persist(year);
            }
            year.merge(stats);
            years.add(year);
        }
        round.names().forEach((uuid, name) -> {
            PlayerName stored = session.find(PlayerName.class, uuid);
            if (stored == null) {
                session.persist(new PlayerName(uuid, name, round.finishedAt()));
            } else {
                stored.rename(name, round.finishedAt());
            }
        });
        this.afterYearRowsLoaded.run();
        Map<UUID, YearStatsView> merged = new HashMap<>();
        years.forEach(year -> {
            YearStatsView view = year.toView();
            merged.put(view.playerUuid(), view);
        });
        return merged;
    }

    /**
     * Reads one year of a player on the executor.
     *
     * @param playerUuid the player
     * @param year       the calendar year
     * @return the year, empty without rounds in it; exceptionally when the read failed
     */
    @Override
    public CompletableFuture<Optional<YearStatsView>> findYear(UUID playerUuid, int year) {
        return read(session -> Optional.ofNullable(session.find(PlayerYearStats.class,
                new PlayerYearStats.Key(playerUuid, year))).map(PlayerYearStats::toView));
    }

    /**
     * Finds the player who was last seen with the name on the executor, ignoring case.
     *
     * @param name the name to look for
     * @return the player, empty for an unknown name; exceptionally when the read failed
     */
    @Override
    public CompletableFuture<Optional<KnownPlayer>> findPlayerByName(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return read(session -> session
                .createSelectionQuery("from PlayerName n where n.nameLower = :name order by n.updatedAt desc, n.playerUuid",
                        PlayerName.class)
                .setParameter("name", lower)
                .setMaxResults(1)
                .getResultList().stream().findFirst().map(PlayerName::toKnownPlayer));
    }

    /**
     * Lists the years of a player on the executor.
     *
     * @param playerUuid the player
     * @return the years, newest first; exceptionally when the read failed
     */
    @Override
    public CompletableFuture<List<Integer>> availableYears(UUID playerUuid) {
        return read(session -> session
                .createSelectionQuery("select y.id.year from PlayerYearStats y where y.id.playerUuid = :player order by y.id.year desc",
                        Integer.class)
                .setParameter("player", playerUuid)
                .getResultList());
    }

    private <T> CompletableFuture<T> read(Function<Session, T> query) {
        CompletableFuture<T> result = new CompletableFuture<>();
        try {
            this.executor.execute(() -> {
                try {
                    result.complete(this.sessionFactory.fromSession(query));
                } catch (RuntimeException exception) {
                    result.completeExceptionally(exception);
                }
            });
        } catch (RejectedExecutionException exception) {
            result.completeExceptionally(exception);
        }
        return result;
    }

    private void fail(FinishedRound round, RuntimeException exception) {
        LOGGER.error("Writing the statistics of round {} failed", round.roundId(), exception);
        this.metrics.writeFailed();
    }

    /**
     * Tells whether another server won the race: an optimistic lock failure or a unique-key violation
     * anywhere in the cause chain.
     */
    static boolean conflict(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof OptimisticLockException || cause instanceof StaleStateException
                    || cause instanceof EntityExistsException || cause instanceof ConstraintViolationException) {
                return true;
            }
        }
        return false;
    }

    /**
     * Deletes the player's round rows, year rows, stored name and slender pass rows in one transaction.
     *
     * @param playerUuid the player
     * @return how many rows were removed in total
     */
    @Override
    public int deleteByPlayer(UUID playerUuid) {
        return this.sessionFactory.fromTransaction(session -> {
            int rounds = session.createMutationQuery("delete from PlayerRoundStats r where r.id.playerUuid = :player")
                    .setParameter("player", playerUuid)
                    .executeUpdate();
            int years = session.createMutationQuery("delete from PlayerYearStats y where y.id.playerUuid = :player")
                    .setParameter("player", playerUuid)
                    .executeUpdate();
            int names = session.createMutationQuery("delete from PlayerName n where n.playerUuid = :player")
                    .setParameter("player", playerUuid)
                    .executeUpdate();
            int passes = session.createMutationQuery("delete from SlenderPassEntry e where e.playerUuid = :player")
                    .setParameter("player", playerUuid)
                    .executeUpdate();
            int balances = session.createMutationQuery("delete from SlenderPassBalance b where b.playerUuid = :player")
                    .setParameter("player", playerUuid)
                    .executeUpdate();
            return rounds + years + names + passes + balances;
        });
    }

    /**
     * Returns the slender pass repository on the same session factory and executor, so a deletion by player
     * covers the passes in the same transaction.
     *
     * @param clock the time of every ledger row
     * @return the Hibernate-backed pass repository
     */
    @Override
    public SlenderPassRepository slenderPasses(Clock clock) {
        return new HibernateSlenderPassRepository(this.sessionFactory, this.executor, clock);
    }

    /**
     * Drains the executor so a queued round is still written, closes the session factory, then releases
     * the pool.
     */
    @Override
    public void close() {
        if (this.executor instanceof ExecutorService service) {
            service.shutdown();
            try {
                if (!service.awaitTermination(DRAIN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    LOGGER.warn("Statistics writes still running after {} seconds, closing anyway", DRAIN_TIMEOUT_SECONDS);
                    service.shutdownNow();
                }
            } catch (InterruptedException exception) {
                service.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        this.sessionFactory.close();
        this.afterClose.run();
    }
}
