## Context

`add-slender-pass` introduces the balance, the ledger (`slender_pass_transaction`, reasons `ADMIN_*` and `REDEEM`) and
the repository with its optimistic-lock retry. The persistence lives in the Gradle module `:stats`, package
`net.onelitefeather.cygnus.stats`: `SlenderPassRepository`, `HibernateSlenderPassRepository`,
`InMemorySlenderPassRepository`, `NoOpSlenderPassRepository`, the entities `SlenderPassBalance` and
`SlenderPassEntry`, the enum `LedgerReason` and the migrations `V1__player_statistics.sql` and
`V2__slender_pass.sql`. This change adds to that module and adds `V3__slender_pass_earning.sql` after `V2`.

Round facts used here, all taken from the statistics:

- `RoundStatsCollector` (module `:game`, package `net.onelitefeather.cygnus.stats`) is the one place that sees the
  round. Its event sources (`GameStartEvent`, `GameFinishEvent`, `PlayerDisconnectEvent`, `PlayerDeathEvent`,
  `SlenderReviveEvent` for takeovers) feed `RoundStatsTracker`, and `gameFinished` produces a `FinishedRound`: one
  `RoundStats` per participant with the role the participant started in, whether a takeover happened
  (`takenOver`), and the outcome (`ABANDONED` for a player who quit).
- The Slender is chosen in `TeamHelper.prepareTeamAllocation` at round start (through the reservation of
  `add-slender-pass`, or randomly). `SlenderTakeover` can turn a survivor into the Slender during a round.
- Ranks are LuckPerms groups; `common/.../permission/LuckPermsSupport` and `PermissionAwarePlayer` already read
  LuckPerms data through the cached user data.

## Goals / Non-Goals

**Goals**

- A player who keeps playing as Survivor receives a pass after a fixed number of rounds.
- A rank grants a fixed number of passes per calendar month, paid once per month, never back-paid.
- Earned credit never lifts a balance above the configured cap.
- Every credit is in the ledger and is announced to the player.

**Non-Goals**

- Changes to the redemption or the round-start rules of `add-slender-pass`.
- Earning from the administrator commands (they keep their uncapped behaviour).
- Rewards by achievements or by pages found.
- A UI to see the counter; the counter is internal.
- A second detection of who played which role. The counter reads the snapshot the statistics already build.

## Data changes

```
player_slender_pass (added columns)
-----------------------------------
rounds_since_slender  INT        NOT NULL DEFAULT 0  CHECK (rounds_since_slender >= 0)
last_quota_month      VARCHAR(7) NULL                -- 'YYYY-MM', UTC
```

- `last_quota_month` is text, not a date type: `YYYY-MM` sorts correctly as a string, the comparison is one SQL
  predicate, and it stays portable to H2 in PostgreSQL mode. The Java side maps it to `YearMonth` with an
  `AttributeConverter`.
- New ledger reasons: `EARNED_PITY` and `RANK_QUOTA`. The `reason` check of `slender_pass_transaction` is replaced.
  `V2` gives that check the name `ck_slender_pass_transaction_reason`, so `V3` drops it by that name and adds a
  check with the six reasons under the same name. The table, its rows and their ids stay as they are, and the
  index from `V2` is not touched.
- `V3` adds the two columns with `ALTER TABLE ... ADD COLUMN`, which both databases accept on a populated table.
- `V1` and `V2` are not changed.

## Pity counter

The counter is updated from the `FinishedRound` that `RoundStatsCollector.gameFinished` hands out, before the
statistics are written. The classification uses the same fields as the statistics. It lives in `PityCounter.roundFinished`:

| Participant in `FinishedRound`                              | Effect on `rounds_since_slender`                       |
|-------------------------------------------------------------|--------------------------------------------------------|
| role `SLENDER` at start (any path: pass, random, takeover)  | reset to 0                                             |
| role `SURVIVOR` with `takenOver = true`                     | reset to 0                                             |
| role `SURVIVOR`, outcome `ABANDONED` (quit before the end)  | unchanged                                              |
| role `SURVIVOR`, not taken over, not abandoned              | +1; at `pass.pity-rounds` the counter goes to 0 and a pass is credited |

- The classification reads `FinishedRound.players()` only. It does not look at the team tags or the events again.
- The reset for the Slender is applied with the round snapshot, not at the moment of selection. A Slender who quits
  early is still in the snapshot with outcome `ABANDONED`, so the reset still happens. The only difference to a
  reset at selection: a server that stops in the middle of a round keeps the old counter for that round. The round
  itself is not recorded in that case either.
- The increments, the resets and the threshold credit for one round are one transaction in the repository, with the
  optimistic lock of the balance rows and the retry of `HibernateStatsRepository.MAX_ATTEMPTS`. Two servers that
  finish rounds for the same player therefore cannot skip the threshold.
- The threshold credit reuses the cap rule below. The counter goes to 0 when it reaches the threshold, even when the
  cap discards the pass (see Decisions).

## Monthly quota

- The meta `cygnus.slender-pass.monthly-quota` is read through a small interface, `MonthlyQuota.MonthlyQuotaSource`,
  with one method `int monthlyQuota(UUID player)`. Its LuckPerms implementation is `LuckPermsSupport.monthlyQuota` in
  `common`: it reads the cached user data (no I/O), returns 0 without LuckPerms, without user data, without the meta,
  or for a value that is not an integer, and logs the last case. Tests give a fake.
- The hook is the lobby join of `PlayerSpawnListener` (the first spawn in the lobby), next to
  `SlenderPassService.onLobbyJoin`, on the tick thread. The grant is idempotent per month, so a repeated lobby join in
  the same month credits nothing and costs one conditional statement.
- The grant is one repository call. Inside one transaction it creates the balance row if needed, runs
  `UPDATE player_slender_pass SET last_quota_month = :month, version = version + 1 WHERE player_uuid = :uuid AND (last_quota_month IS NULL OR last_quota_month < :month)`,
  and credits the quota only when the update matched. The `version` bump is explicit because a bulk update does not
  bump a `@Version` column by itself. Only the server whose update matches pays, so two servers that both see the
  join cannot both pay.
- A quota of 0 still sets `last_quota_month` and writes no ledger row and no message.
- No back pay: after a gap of several months the player gets one quota, once.

## Cap

Earned credit is `delta = min(amount, max(0, max-balance - balance))`. The ledger records that delta. A delta of 0
writes no ledger row. Administrator changes do not use the cap.

A balance already above the cap (from an administrator) is never reduced by earning; earning simply credits nothing
until the balance drops below the cap.

## Messages

Every credit with a delta above 0 sends one message to the player, with the reason (pity or quota) and the new
balance. The repository completes with the credit; the game hands the message to the tick thread. A credit that is
reduced to 0 by the cap sends nothing (see open questions). New texts are constants in `common/.../Messages.java`.

## Configuration

`GameConfig` gets one more section record, `SlenderPassConfig`, read from the keys `pass.*` in the same way as the
other sections (`root.section("pass.")`, `orDefault`, `ConfigChecks`):

```
pass.pity-rounds=10     # rounds as Survivor until one pass is earned; must be >= 1
pass.max-balance=5      # cap for earned credit; must be >= 0
```

Invalid values stop the server at start with a clear message, the same way other required values are checked by
`ConfigChecks.atLeast` and `ConfigChecks.notNegative`. The keys are listed in `config.properties.example`.

## Testing

- Unit (Fast, Independent) in `:game`: `PityCounterTest` and `MonthlyQuotaTest` run the real `RoundStatsTracker` and
  `InMemorySlenderPassRepository` (new methods, same rules as the database) and a fixed `Clock`. The month change is
  tested at the turn of the month in UTC (`2026-10-31T23:30Z` is October, and `2026-11-01T00:30Z` is November). The
  meta is a fake `MonthlyQuotaSource`.
- Integration in `:stats`: `HibernateSlenderPassRepositoryEarningIntegrationTest` against H2 in PostgreSQL mode with
  the real migrations `V1` to `V3` and `hbm2ddl=validate`: the threshold in one transaction, the conditional quota
  grant by two calls (only one credit), the cap, the new reasons under the check constraint, and the constraint swap
  of `V3` keeping the existing rows and their ids.
- No sleeps; the Minestom `Env` drives joins and round ends with `env.tick()`.

## Decisions

- **Counter from the round snapshot**: the classification reads `FinishedRound`, the snapshot the statistics already
  build, so there is one definition of "survivor who played to the end" for both. A takeover and a quit are the same
  fields the statistics use.
- **Reset at round end, not at selection**: the snapshot knows the Slender of the round, including one who quit, so
  the reset needs no hook into `TeamHelper` or `SlenderTakeover`. Trade-off: a crash mid-round keeps the old counter.
- **Counter reset on the threshold even when the cap drops the credit**: the pity was earned; the cap discards the
  pass, it does not reopen the counter.
- **Conditional quota update instead of a read-then-write**: the database decides which server pays.
- **`YYYY-MM` as text**: sortable, portable to H2, one predicate.
- **Named check in `V2`, replaced in `V3`**: `ALTER TABLE ... DROP CONSTRAINT` and `ADD CONSTRAINT` work on both
  databases, and the ledger keeps its ids.
- **Quota read from the cached LuckPerms data on the tick thread**: the lookup is in memory; only the database write
  is off the tick thread.

## Open questions

1. **Capped to zero**: a player at the cap who would earn a pass gets nothing and no message. The brief says a
   message for every credit; a credit of 0 is not one. Alternative: send "balance full, nothing credited". Default
   chosen here: no message.
2. **Counter when the cap blocks the pity pass**: the design resets the counter (see Decisions). If the team wants the
   pity to wait until a pass can be granted, the reset moves to the grant and the counter keeps growing.
3. **Invalid configuration**: the design stops the server at start. Whether this matches the existing reader's
   behaviour for other required keys has to be checked in `GameConfigReader`.
4. **Quota meta changed mid-month**: the quota is credited at the first spawn of the month with the value present
   then. A later increase in the same month is not paid. This follows the "once per month" rule and is not changed here.
