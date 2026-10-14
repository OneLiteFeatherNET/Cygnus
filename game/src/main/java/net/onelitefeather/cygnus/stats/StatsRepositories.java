package net.onelitefeather.cygnus.stats;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.onelitefeather.cygnus.common.config.StatsConfig;
import org.flywaydb.core.Flyway;
import org.hibernate.SessionFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Builds the repository the configuration asks for: the no-op one without a JDBC URL, otherwise a
 * Hibernate one on a Hikari pool whose schema Flyway has brought up to date.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.16.0
 */
public final class StatsRepositories {

    /** Gives up on an unreachable database after this long instead of Hikari's 30 seconds, so a start is not held up. */
    private static final long CONNECTION_TIMEOUT_MILLIS = 5_000L;

    private StatsRepositories() {
    }

    /**
     * Opens the repository. Connects, migrates and validates when statistics are enabled.
     *
     * @param config  the statistics settings
     * @param metrics counts failed writes
     * @return the repository, never {@code null}
     * @throws RuntimeException if the database is unreachable or its schema does not validate; the caller decides
     *                          whether the game runs without statistics then
     */
    public static StatsRepository create(StatsConfig config, StatsMetrics metrics) {
        if (!config.enabled()) {
            return new NoOpStatsRepository();
        }
        HikariConfig pool = new HikariConfig();
        pool.setPoolName("cygnus-stats");
        pool.setJdbcUrl(config.jdbcUrl());
        pool.setMaximumPoolSize(config.poolSize());
        pool.setConnectionTimeout(CONNECTION_TIMEOUT_MILLIS);
        if (!config.username().isEmpty()) {
            pool.setUsername(config.username());
        }
        if (!config.password().isEmpty()) {
            pool.setPassword(config.password());
        }
        HikariDataSource dataSource = new HikariDataSource(pool);
        try {
            Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .load()
                    .migrate();
            SessionFactory sessionFactory = HibernateStatsRepository.buildSessionFactory(dataSource);
            ExecutorService executor = Executors.newSingleThreadExecutor(Thread.ofPlatform().name("cygnus-stats").factory());
            return new HibernateStatsRepository(sessionFactory, executor, metrics, dataSource::close);
        } catch (RuntimeException exception) {
            dataSource.close();
            throw exception;
        }
    }
}
