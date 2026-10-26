## Why

Players cannot choose to play the Slender; the role is always drawn at random from the online players. A slender
pass lets a player spend a stored credit to claim the Slender role for the next round. The team needs the credit
to be a durable, auditable balance, so that it can be granted deliberately by administrators now and earned by
playing later (see `add-slender-pass-earning`).

A pass is a one-round claim. Only one claim may exist per round, so a player who reserves the role in the lobby
keeps the reservation until the round actually starts. The role is granted at the round start and the credit is
spent in the background. If the spend fails, the player keeps the role for that round and the failure is logged.

## Commit type and scope

Conventional Commits type and scope: `feat(pass)` for the implementation, with its tests in the same commit. The
OpenSpec documents are a separate `docs(openspec)` commit, so each commit carries one type.

Prerequisite: `add-yearly-player-statistics` is on `main`. It lives in the Gradle module `:stats`, which this change
extends: the pass persistence is part of that module, next to the statistics repository, on the same Hikari pool,
executor and session factory. The pass migration is `V2__slender_pass.sql`, after `V1__player_statistics.sql`.

## What Changes

- Add the `stats` package `net.onelitefeather.cygnus.stats` (module `:stats`) with the `SlenderPassRepository`
  interface, `NoOpSlenderPassRepository` (used when `stats.jdbc-url` is empty), `InMemorySlenderPassRepository`
  (for tests), `HibernateSlenderPassRepository` and the entities `SlenderPassBalance` and `SlenderPassEntry`.
- Add the Flyway migration `V2__slender_pass.sql` with two tables:
  - `player_slender_pass`: one balance per player (`player_uuid` primary key, `balance` integer with
    `CHECK (balance >= 0)`, a `version` column for optimistic locking).
  - `slender_pass_transaction`: an append-only ledger (`id`, `player_uuid`, `delta`, `reason` in
    `ADMIN_GIVE`, `ADMIN_TAKE`, `ADMIN_SET`, `REDEEM`, `round_id` nullable, `created_at` from the injected `Clock`).
- Every balance change writes its ledger entry in the same transaction. A redemption decreases the balance with one
  conditional update (`balance = balance - 1 WHERE player_uuid = ? AND balance > 0`), so two servers cannot spend the
  same pass twice. No database call runs on the tick thread.
- Lobby redemption, in the `game` module `net.onelitefeather.cygnus.pass`:
  - a hotbar item in the lobby shows the remaining balance in its name and lore; a right-click reserves a round,
    a second right-click cancels the reservation;
  - the command `/pass` (alias `/pass slender`) reserves, `/pass cancel` cancels and `/pass balance` shows the balance.
    Redemption is allowed only in the lobby phase.
- Conflict rules: at most one reservation per round, first come first served. Other players are refused with a
  message and keep their pass. Players with a zero balance are refused. Everyone in the lobby sees that a pass was
  redeemed, without the name of the player.
- A reservation is dropped when its holder disconnects. Countdown resets and force starts keep it.
- At round start, `GamePrepareEvent` and `TeamHelper` select the reserved player as Slender when that player is
  online, without waiting for the database. The pass is debited asynchronously (`REDEEM` with the round id). If the
  debit fails (balance zero by then, or a database error), the holder keeps the role for this round and a warning is
  logged. `SlenderTakeover` stays random.
- Admin command `/pass give|take|set <player> <amount>` with permission `cygnus.pass.admin`; the console may run these
  three subcommands. `take` never goes below zero. Admin changes are not capped (the cap comes with the earning change).
  Only players that are online can be named: resolving offline names needs a name lookup that this change does not add.
- When the statistics database is not configured, the lobby item is not shown and `/pass` answers "feature unavailable".
- The GDPR deletion by UUID (`HibernateStatsRepository.deleteByPlayer`) also deletes the rows of both pass tables in
  the same transaction.
- New messages are constants in `common/.../Messages.java`.

## Capabilities

### New Capabilities

- `slender-pass`: the stored pass balance, the lobby redemption and reservation, the round-start claim of the
  Slender role, the admin commands and the ledger.

### Modified Capabilities

None.

## Impact

- `stats/src/main/java/net/onelitefeather/cygnus/stats/` (repository interface, no-op and in-memory implementations,
  Hibernate implementation, entities, `StatsRepository.slenderPasses`, GDPR deletion).
- `stats/src/main/resources/db/migration/V2__slender_pass.sql`.
- `game/src/main/java/net/onelitefeather/cygnus/pass/` (service, feedback, command, feature).
- `game/src/main/java/net/onelitefeather/cygnus/team/TeamHelper.java` and `utils/StaminaHelper.java` (round-start
  selection takes the reserved player first), `utils/Items.java` (lobby item), `listener/PlayerSpawnListener.java`
  (lobby hook), `Cygnus.java` (wiring, round start claim, command registration). `phase/WaitingPhase.java` is not changed.
- `common/.../Messages.java` (new constants). No new configuration keys: the database URL and credentials are the ones
  of the statistics change.
- Tests: `stats/src/test/.../stats/` (repository, integration, schema) and `game/src/test/.../pass/`, written before
  the production code.
- Permissions: `cygnus.pass.admin` is checked through the existing permission support, as `StartCommand` does.
- Operations: no new service. Players need the statistics database to hold passes; without it the feature is hidden.
