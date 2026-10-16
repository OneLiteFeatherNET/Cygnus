## Why

Cygnus forgets everything about a player when a round ends. `CygnusPlayer` counts pages, kills and the death for the
round summary in `GameFinishListener`, and the next round starts from zero. Players have no way to see how they
played over time, and the team has no data for a yearly recap ("Jahresrückblick"), leaderboards or balancing
questions such as "how often does the Slender win on average".

This change persists one record per player and round and keeps a per-player, per-calendar-year aggregate, both
through Hibernate (JPA). Players can look at the statistics in game: a chat command, an inventory menu and a lobby
item, and the round summary shows the updated year totals. A hologram or a yearly recap is not part of this change.

## Commit type and scope

Conventional Commits type and scope: `feat(stats)`. The new dependencies (Hibernate, HikariCP, Flyway, PostgreSQL JDBC driver)
belong to the feature and ship in the same commit. The OpenSpec documents are a separate `docs(openspec)` commit.

## What Changes

- Add a Gradle module `stats` (package `net.onelitefeather.cygnus.stats`) that owns the domain, the per-round
  bookkeeping (`RoundStatsTracker`), the persistence and the `stats.*` config. It does not depend on `game`.
- Add a thin adapter `RoundStatsCollector` in the `game` module that listens to the existing game events
  (`GameStartEvent`, `PageFoundEvent`, `PlayerDeathListener` kill credit, `PlayerDamagedEvent`, painkiller use,
  `SlenderReviveEvent`, creek hunt ends, quits, `GameFinishEvent`) and feeds the tracker, which builds one in-memory
  `RoundStats` snapshot per participant.
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
- Show the statistics to players (package `net.onelitefeather.cygnus.stats.display` in `game`):
  - `/stats [player] [year]` prints a compact overview in chat (general, Survivor and Slender, with win rate, K/D and
    averages computed on read); a click on it opens the menu of the same player and year.
  - A read-only chest menu with one item per area.
  - A hotbar item in the lobby and waiting phase; a right click opens the player's own current-year menu. It is gone
    when the round starts, cannot be dropped or moved, and is not given while statistics are off.
  - The round summary gets one extra line per player with the updated year totals (rounds, wins, pages, kills).
  - Everyone may look at everyone's statistics, offline players who played before included.
- Store the last known name of every player (`player_names`, written in the round's transaction) so offline players
  can be found by name.
- `StatsRepository` gets asynchronous reads (`findYear`, `findPlayerByName`, `availableYears`) that return immutable
  `YearStatsView` records, and `saveRound` completes with the merged year rows after the commit.

## Capabilities

### New Capabilities

- `player-statistics`: collecting per-round player statistics, persisting them and aggregating them per calendar year.

### Modified Capabilities

- None.

## Impact

- New `stats` module: domain, tracker, repository, entities, Flyway migration and `StatsConfig`. All database
  dependencies live here; the module depends on neither `game` nor `common`.
- `game` module: depends on `stats`, holds the event adapter `RoundStatsCollector`, the display code in
  `net.onelitefeather.cygnus.stats.display` (command, menu, lobby item, cache, messages), the statistics item in
  `Items`, the wiring in the `Cygnus` constructor and the round id exposed from `RoundTracer`. All player-facing text is
  English.
- `common` module: untouched, it knows nothing about the statistics.
- Build: `stats` is included in `settings.gradle.kts`; Hibernate ORM, HikariCP, Flyway and the PostgreSQL JDBC driver
  are in the version catalog and used by `stats` only (bundled into the game shadow jar through `game -> stats`); H2 is
  a test dependency of `stats`.
- Operations: one PostgreSQL database per environment, shared by all Cygnus servers of that environment; credentials
  via `config.properties` or environment variables. Data is kept with no automatic pruning.
- Privacy: stored data is the Minecraft UUID plus game counters and the last known Minecraft name. Minecraft names are
  public; the name is kept only so a player can be found by name, and it is deleted together with the rest of the
  UUID's statistics. No IPs are stored. A deletion by UUID is part of the repository so a GDPR request can be served.
  Statistics are visible to every player by design.
