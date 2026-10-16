## 1. Build and configuration

- [x] 1.1 Add Hibernate ORM 7, HikariCP, Flyway and the PostgreSQL JDBC driver to the version catalog in
  `settings.gradle.kts` and to `stats/build.gradle.kts`; add H2 as a test dependency
- [x] 1.2 Add the `stats.*` keys to `config.properties.example` and read them in `StatsConfig` (test first:
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

## 4. Module extraction

- [x] 4.1 Move the statistics into the `stats` module (`game -> stats`, `common` untouched): extract the
  game-independent `RoundStatsTracker` and `FinishReason`, let `StatsConfig` read the `stats.*` keys itself, move the
  database dependencies and the migration, keep `RoundStatsCollector` as the event adapter in `game`; tests move with
  their code

## 4a. Data for a later role balancing

- [x] 4a.1 Record `participants` and `taken_over` per round row and `takeovers` per year row, add the index
  `(player_uuid, finished_at)`, edit V1 in place; tests first in the tracker, the repository integration test and the
  collector event test. The balancing itself is a separate later change

## 4b. Player display

- [x] 4b.1 Add `player_names` to V1 (edited in place), the `PlayerName` entity, `KnownPlayer`, `YearStatsView` with the
  derived values, and the names in `FinishedRound` and `RoundStatsTracker`; tests first in the tracker, the view and the
  repository integration test (rename, name reuse, late write, deletion)
- [x] 4b.2 Give `StatsRepository` the reads `findYear`, `findPlayerByName`, `availableYears` and let `saveRound` complete
  with the merged year rows; update `NoOpStatsRepository`, `HibernateStatsRepository` and the callers and tests
- [x] 4b.3 Add `StatsOverview`, `StatsMessages`, `StatsMenu`, `PlayerStatsService` and `StatsCommand` in
  `net.onelitefeather.cygnus.stats.display`; tests first with a `FakeStatsRepository` and a Minestom `Env`
- [x] 4b.4 Add the lobby item (`Items.STATS_ITEM`) and `StatsDisplayFeature`: given in the lobby and waiting phase,
  removed at the game start, locked, not given while statistics are off
- [x] 4b.5 Hand the committed totals from `RoundStatsCollector` to the round-end line and wire everything in `Cygnus`
- [x] 4b.6 Update the OpenSpec documents for the display

## 5. Verification

- [x] 5.1 Run `./gradlew :game:test` and `./gradlew build`
- [ ] 5.2 Live test: play one round against a local database and check both tables

## 6. Pull request

- [x] 6.1 Open the pull request titled `feat(stats): persist yearly player statistics with hibernate`
