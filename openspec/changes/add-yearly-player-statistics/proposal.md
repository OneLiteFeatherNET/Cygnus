## Why

Cygnus forgets everything about a player when a round ends. `CygnusPlayer` counts pages, kills and the death for the
round summary in `GameFinishListener`, and the next round starts from zero. Players have no way to see how they
played over time, and the team has no data for a yearly recap ("Jahresrückblick"), leaderboards or balancing
questions such as "how often does the Slender win on average".

This change persists one record per player and round and keeps a per-player, per-calendar-year aggregate, both
through Hibernate (JPA). Showing the statistics in game (command, hologram, recap) is not part of this change; it
only builds the data and the storage that later changes read from.

## Commit type and scope

Conventional Commits type and scope: `feat(stats)`. The new dependencies (Hibernate, HikariCP, Flyway, PostgreSQL JDBC driver)
belong to the feature and ship in the same commit. The OpenSpec documents are a separate `docs(openspec)` commit.

## What Changes

- Add a `stats` package in the `game` module with a `RoundStatsCollector` that listens to the existing game events
  (`GameStartEvent`, `PageFoundEvent`, `PlayerDeathListener` kill credit, `PlayerDamagedEvent`, painkiller use,
  `SlenderReviveEvent`, creek hunt ends, quits, `GameFinishEvent`) and builds one in-memory `RoundStats` snapshot per
  participant.
- On `GameFinishEvent` the collector hands the finished snapshots to a `StatsRepository`, which writes them off the
  tick thread in one transaction:
  - one `PlayerRoundStats` row per participant (source of truth, allows recomputation),
  - an upsert of the `PlayerYearStats` row for `(player_uuid, year)` (fast reads for profiles and leaderboards).
- The year is the calendar year of the round end in a configurable time zone (default `Europe/Berlin`), taken from
  an injected `java.time.Clock`.
- Add Hibernate 7 (JPA), HikariCP and Flyway; the schema is created and versioned by Flyway migrations, never by
  `hbm2ddl`.
- Add `stats.*` keys to `config.properties.example`. Without `stats.jdbc-url` the feature is off and the server runs
  as today (a no-op repository).
- Expose the round id that `RoundTracer` already generates so the round rows and the telemetry spans share one id.

## Capabilities

### New Capabilities

- `player-statistics`: collecting per-round player statistics, persisting them and aggregating them per calendar year.

### Modified Capabilities

- None.

## Impact

- `game` module: new `stats` package, wiring in the `Cygnus` constructor, round id exposed from `RoundTracer`.
- `common` module: `GameConfig`/`GameConfigReader` read the `stats.*` keys.
- Build: Hibernate ORM, HikariCP, Flyway and the PostgreSQL JDBC driver in the version catalog in `settings.gradle.kts`; H2 as a
  test dependency for the repository integration tests.
- Operations: one PostgreSQL database per environment, shared by all Cygnus servers of that environment; credentials
  via `config.properties` or environment variables. Data is kept with no automatic pruning.
- Privacy: stored data is the Minecraft UUID plus game counters, no names or IPs. A deletion by UUID is part of the
  repository so a GDPR request can be served.
