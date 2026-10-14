## 1. Build and configuration

- [x] 1.1 Add Hibernate ORM 7, HikariCP, Flyway and the PostgreSQL JDBC driver to the version catalog in
  `settings.gradle.kts` and to `game/build.gradle.kts`; add H2 as a test dependency
- [x] 1.2 Add the `stats.*` keys to `config.properties.example` and read them in `GameConfigReader` (test first:
  missing URL yields "disabled", password falls back to `CYGNUS_STATS_PASSWORD`)

## 2. Collector (tests first)

- [x] 2.1 Add `RoundStatsCollectorTest` with an in-memory `StatsRepository` fake and a fixed `Clock`: participant
  tracking, page/kill/death/damage/painkiller counting, outcome per finish reason, quit as `ABANDONED`, year at the
  turn of the year; confirm the tests fail
- [x] 2.2 Add `RoundStats` (record + builder), `StatsRepository` interface and `NoOpStatsRepository`
- [x] 2.3 Implement `RoundStatsCollector` and register it on the game event node
- [x] 2.4 Expose the round id from `RoundTracer` and pass it to the collector

## 3. Persistence (tests first)

- [x] 3.1 Add Flyway migration `V1__player_statistics.sql` for `player_round_stats` and `player_year_stats`
- [x] 3.2 Add `HibernateStatsRepositoryIntegrationTest` against H2 (fresh database per test): round insert, aggregate
  sum/max/min, optimistic-lock retry, retry when another server created the year row first, deletion by UUID, `hbm2ddl` validation; confirm the tests fail
- [x] 3.3 Add the `PlayerRoundStats` and `PlayerYearStats` entities and `HibernateStatsRepository` with the injected
  executor, retry and failure metric
- [x] 3.4 Wire repository, executor and collector in the `Cygnus` constructor; drain the executor on shutdown

## 4. Verification

- [x] 4.1 Run `./gradlew :game:test` and `./gradlew build`
- [ ] 4.2 Live test: play one round against a local database and check both tables

## 5. Pull request

- [x] 5.1 Open the pull request titled `feat(stats): persist yearly player statistics with hibernate`
