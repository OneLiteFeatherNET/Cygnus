## Why

`add-slender-pass` lets players spend passes, but the only way to get one is an administrator. Players who keep
playing would never see the Slender role they redeem for, and the feature has no reward for regular play. This
change adds two sources that work without anyone at a keyboard:

- a pity counter: a player who keeps being a Survivor and never draws the Slender role gets a pass after a fixed
  number of rounds;
- a monthly rank quota: a rank (LuckPerms meta) grants a number of passes each calendar month.

Both sources are capped, so a player cannot stockpile passes for a year.

## Commit type and scope

Conventional Commits type and scope: `feat(pass)`, the same scope as `add-slender-pass`, with the tests in the same
commit. The OpenSpec documents are a separate `docs(openspec)` commit. The title is different because this change
adds a separate capability of the same feature; it does not mix in a second type.

Prerequisite: `add-slender-pass` is merged, because this change extends its tables, its ledger and its repository in
the Gradle module `:stats` (package `net.onelitefeather.cygnus.stats`). The migration is `V3__slender_pass_earning.sql`,
after `V2__slender_pass.sql`. The new Java classes carry `@since 2.21.0`.

## What Changes

- Add the Flyway migration `stats/src/main/resources/db/migration/V3__slender_pass_earning.sql`:
  - two new columns on `player_slender_pass`: `rounds_since_slender` (integer, default 0, `CHECK (>= 0)`) and
    `last_quota_month` (text `YYYY-MM`, nullable);
  - the `reason` check of `slender_pass_transaction` is replaced by one that also allows `EARNED_PITY` and
    `RANK_QUOTA`. The inline check of `V2` is unnamed, so the migration rebuilds the ledger table; the rows are kept.
  - `V1` and `V2` are not changed.
- Pity counter: the rounds are counted from the round snapshot that the statistics build when a round ends
  (`RoundStatsCollector.gameFinished`, `FinishedRound`). A Survivor who plays to the end, dead or alive, increases the
  counter by one. A player who quit before the end is not counted. A player who was Slender at the start (by pass,
  by chance or by takeover) gets the counter reset to 0. When the counter reaches `pass.pity-rounds` (default 10),
  the player receives one pass with reason `EARNED_PITY` and the counter goes back to 0.
- Monthly rank quota: the LuckPerms meta `cygnus.slender-pass.monthly-quota` (integer) gives passes per month. In the
  lobby spawn, when `last_quota_month` is before the current month (UTC, from the injected `Clock`), the quota is
  credited once with reason `RANK_QUOTA` and `last_quota_month` is set to the current month. Missed months are not
  paid back. Without LuckPerms, or without the meta, there is no quota.
- Cap: `pass.max-balance` (default 5) limits the balance for `EARNED_PITY` and `RANK_QUOTA`. A credit is reduced to
  what fits, and a credit reduced to 0 writes no ledger row and sends no message. Administrator changes are not capped.
- Every credit with a delta above 0 sends the player one message (new constants in `Messages.java`).
- Add the section `SlenderPassConfig` (keys `pass.pity-rounds` and `pass.max-balance`) to `GameConfig` and
  `GameConfigReader`, and list both keys in `config.properties.example`. Values below their minimum stop the server at
  start.

## Capabilities

### New Capabilities

- None.

### Modified Capabilities

- `slender-pass`: the requirement on the ledger now lists the earned reasons, and the earning rules are added
  (pity counter, monthly quota, cap, credit messages, settings).

## Impact

- `stats/src/main/java/net/onelitefeather/cygnus/stats/`: `LedgerReason` (two values), `SlenderPassBalance`
  (two columns, the `YearMonth` converter), `SlenderPassRepository` with `HibernateSlenderPassRepository`,
  `InMemorySlenderPassRepository` and `NoOpSlenderPassRepository` (the new methods).
- `stats/src/main/resources/db/migration/V3__slender_pass_earning.sql`.
- `game/src/main/java/net/onelitefeather/cygnus/pass/`: the pity counter (classification of the round snapshot,
  threshold credit, messages) and the monthly quota (the spawn hook, the `MonthlyQuotaSource` interface).
- `game/src/main/java/net/onelitefeather/cygnus/stats/RoundStatsCollector.java`: one more consumer of the finished
  round, called before it is written. The statistics themselves do not change.
- `game/src/main/java/net/onelitefeather/cygnus/Cygnus.java`: wiring of the counter and the quota, and the lobby spawn
  listener.
- `common/src/main/java/net/onelitefeather/cygnus/common/config/` (`SlenderPassConfig`, `GameConfig`,
  `GameConfigReader`), `config.properties.example`.
- `common/src/main/java/net/onelitefeather/cygnus/common/permission/LuckPermsSupport.java` (the meta read behind the
  quota source), `common/.../Messages.java` (credit messages).
- Tests: `stats/src/test/.../stats/` (integration against H2 for `V3` and the new methods), `game/src/test/.../pass/`
  (counter, quota, cap, messages) and `common/src/test/.../config/` (defaults and refusals), all written before the
  production code.
- Operations: the rank quota is set in LuckPerms per group. No new service.
