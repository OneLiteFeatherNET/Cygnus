package net.onelitefeather.cygnus.stats;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.hibernate.SessionFactory;

import javax.sql.DataSource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Builds the repository the configuration asks for: the no-op one without a JDBC URL, otherwise a
 * Hibernate one on a Hikari pool whose schema Flyway has brought up to date.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.18.0
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
        // PostgreSQL's driver maps this to search_path, so unqualified (native) SQL stays in the schema.
        pool.setSchema(config.schema());
        if (!config.username().isEmpty()) {
            pool.setUsername(config.username());
        }
        if (!config.password().isEmpty()) {
            pool.setPassword(config.password());
        }
        HikariDataSource dataSource = new HikariDataSource(pool);
        try {
            migrate(dataSource, config.schema());
            SessionFactory sessionFactory = HibernateStatsRepository.buildSessionFactory(dataSource, config.schema());
            ExecutorService executor = Executors.newSingleThreadExecutor(Thread.ofPlatform().name("cygnus-stats").factory());
            return new HibernateStatsRepository(sessionFactory, executor, metrics, dataSource::close);
        } catch (RuntimeException exception) {
            dataSource.close();
            throw exception;
        }
    }

    /**
     * Brings the given schema up to date. Flyway creates the schema if needed and keeps its history table
     * in it, so the database's other schemas (such as {@code public}) are never read or touched.
     *
     * @param dataSource the database
     * @param schema     the schema that holds the statistics
     */
    static void migrate(DataSource dataSource, String schema) {
        Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .defaultSchema(schema)
                .createSchemas(true)
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }
}
