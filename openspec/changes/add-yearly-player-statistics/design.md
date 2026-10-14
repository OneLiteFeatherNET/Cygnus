## Context

The game is an asymmetric Slender round: one Slender, the rest Survivors who collect pages. A round runs
`LobbyPhase -> WaitingPhase -> GamePhase -> RestartPhase` and ends with `GameFinishEvent` and one of the reasons
`TIME_OVER`, `ALL_PAGES_FOUND`, `ALL_SURVIVOR_DEAD`, `SLENDER_LEFT`, `SURVIVOR_LEFT`. Survivors win on `TIME_OVER`,
`ALL_PAGES_FOUND` and `SLENDER_LEFT`; the Slender wins on `ALL_SURVIVOR_DEAD` and `SURVIVOR_LEFT`.

There is no persistence layer yet and no DI framework; `Cygnus` builds everything in its constructor.

## Statistics

Counted per round, stored in `PlayerRoundStats`, summed (or maxed / minned) into `PlayerYearStats`.

### General (every participant)

| Statistic | Source | Year aggregate |
|---|---|---|
| Rounds played | participant at `GameStartEvent` | sum |
| Rounds as Slender / as Survivor | team at `GameStartEvent` | sum per role |
| Wins / losses | `GameFinishEvent` reason + role | sum per role |
| Abandoned rounds | quit before `GameFinishEvent` | sum |
| Play time | `GameStartEvent` until finish or quit | sum (seconds) |
| Win by reason | reason of won rounds | sum per reason (`TIME_OVER`, `ALL_PAGES_FOUND`, ...) |

### Survivor

| Statistic | Source | Year aggregate |
|---|---|---|
| Pages found | `PageFoundEvent` | sum, best round (max) |
| Rounds survived (alive at the end) | not dead at `GameFinishEvent` | sum |
| Deaths | `PlayerDeathListener` | sum |
| Time survived | start until death or end | sum, longest |
| Rounds won by finding all pages, with own pages > 0 | `ALL_PAGES_FOUND` | sum |
| Last page found ("finisher") | `PageFoundEvent` with `foundCount == max` | sum |
| Damage taken | `PlayerDamagedEvent` (points) | sum |
| Painkillers used | `RegenerationService` heal item use | sum |
| Creek hunts survived / caught | `CreekWitness` hunt end `TIMEOUT`/`GONE`/`SENT_AWAY` vs `CAUGHT` | sum each |
| Jump scares received | `JumpScareManager` | sum |
| Taken over as Slender | `SlenderReviveEvent` (chosen survivor) | sum |

### Slender

| Statistic | Source | Year aggregate |
|---|---|---|
| Kills | kill credit in `PlayerDeathListener` | sum, best round (max) |
| Flawless wins (all survivors dead) | `ALL_SURVIVOR_DEAD` | sum |
| Fastest win | round time at a Slender win | min (seconds) |
| Damage dealt | `PlayerDamagedEvent` while Slender is the source | sum |
| Pages the survivors found against them | sum of survivor pages in the round | sum |
| Possessions | `PossessionFeature` | sum |

### Derived, not stored

Win rate per role, kills per Slender round, pages per Survivor round, K/D, average time survived. They are computed
from the stored sums when read, so they never drift.

### Deliberately left out

Footprints, stamina state changes, sanity values and blackouts: high frequency, low meaning for a player, and
already covered by OpenTelemetry for balancing. They can be added later as new columns plus a migration.

## Data model

```
player_round_stats                      player_year_stats
-------------------                     -----------------
round_id        UUID   PK(1)            player_uuid  UUID  PK(1)
player_uuid     UUID   PK(2)            year         INT   PK(2)
year            INT    (indexed)        rounds_played, rounds_slender, rounds_survivor,
finished_at     TIMESTAMP (UTC)         wins_slender, wins_survivor, losses_*, abandoned,
role            SLENDER|SURVIVOR        play_time_seconds, pages_found, best_pages_round,
outcome         WIN|LOSS|ABANDONED      deaths, kills, best_kills_round, fastest_win_seconds,
finish_reason   enum                    ... one column per statistic above
pages_found, kills, died, ...           version  BIGINT (optimistic lock)
play_time_seconds, ...
```

- Two entities: `PlayerRoundStats` (composite id `round_id + player_uuid`) and `PlayerYearStats` (composite id
  `player_uuid + year`, `@Version` for optimistic locking).
- Wide tables with one column per statistic instead of a key/value table: type-safe, cheap to query for
  leaderboards (`ORDER BY kills DESC WHERE year = ?`), and each new statistic is a conscious Flyway migration.
- `player_round_stats` is the source of truth. If an aggregate is ever wrong, `player_year_stats` can be rebuilt
  with one `INSERT ... SELECT ... GROUP BY player_uuid, year`.

## Write path

1. `RoundStatsCollector` keeps a `Map<UUID, RoundStats.Builder>` for the current round, created at `GameStartEvent`
   for each team member. All event handlers only touch this map on the tick thread; no I/O.
2. A quit before the finish freezes that player's builder with outcome `ABANDONED`.
3. At `GameFinishEvent` the collector resolves win/loss from the reason and role, builds immutable `RoundStats`
   records and clears the map.
4. `StatsRepository.saveRound(roundId, finishedAt, List<RoundStats>)` runs on a dedicated single-thread executor
   (virtual thread is fine). In one transaction it inserts the round rows and merges each year row
   (load-or-create, add, keep max/min). Several Cygnus servers write to the same database, so two rounds can touch
   the same year row at once: an `OptimisticLockException` (another server updated the row) or a unique-key
   violation (another server created the row first) rolls back and retries the whole transaction up to three times.
   The round rows cannot collide because every round id is unique.
5. A failure is logged with the round id, counted in an OpenTelemetry counter `cygnus.stats.write.failures`, and does
   not affect the round end or the restart.
6. On shutdown the executor is drained with a timeout before the `EntityManagerFactory` closes.

Writing only at the round end keeps the database load at one transaction per round and means a crash mid-round
loses that round's numbers, which is acceptable.

## Configuration

```
stats.jdbc-url=            # empty = statistics off (NoOpStatsRepository)
stats.username=
stats.password=            # or env CYGNUS_STATS_PASSWORD
stats.pool-size=4
stats.time-zone=Europe/Berlin
```

## Testing

- Unit (Fast, Independent): `RoundStatsCollector` against an in-memory `StatsRepository` fake and a fixed `Clock`;
  events are fired through a Minestom `Env` with `env.tick()`. Year resolution is tested at the turn of the year
  (`2026-12-31T23:30Z` is already 2027 in `Europe/Berlin`).
- Integration: `HibernateStatsRepository` against H2 in-memory in PostgreSQL compatibility mode
  (`MODE=PostgreSQL`), a fresh database per test, Flyway migrations applied. The migrations use only SQL that both
  PostgreSQL and the H2 mode accept (no PostgreSQL-only types; UUIDs as `UUID`). Covers insert, aggregate merge, max/min handling, the optimistic
  retry and the deletion by UUID.
- No sleeps: the repository executor is injected, tests pass a direct (same-thread) executor.

## Decisions

- **Hibernate with Flyway, no `hbm2ddl.auto=update`**: schema changes are reviewed SQL; Hibernate runs with
  `hbm2ddl.auto=validate` so a missing migration fails at startup instead of at the first write.
- **Year in a configured zone, not UTC**: the yearly recap follows the players' calendar; a round finished on New
  Year's Eve at 00:30 local time counts for the new year.
- **Both round rows and year aggregate** instead of only one: round rows alone make every profile read a
  `GROUP BY`; aggregates alone make mistakes unrecoverable.
- **Feature off without a JDBC URL**: local development and tests do not need a database.

## Resolved questions

- **Database**: PostgreSQL in production. Driver `org.postgresql:postgresql`, Hibernate's PostgreSQL dialect.
- **Several servers**: yes, several Cygnus services share one database. Hence the optimistic lock on
  `player_year_stats`, the retry on unique-key violations and the round id as part of the round row key.
- **Retention**: rows are kept as long as they are needed; there is no automatic pruning of `player_round_stats`.
  Removing data is a manual decision (or a deletion by UUID) and not part of this change.
- **Spectators who joined mid-round**: no row, only players on a team at `GameStartEvent`.
