## Context

A round runs `LobbyPhase -> WaitingPhase -> GamePhase -> RestartPhase`. `WaitingPhase` fires `GamePrepareEvent`
(around line 69), which leads to `TeamHelper.prepareTeamAllocation`. That method picks the Slender with
`Players.getRandomPlayer()`, assigns the team and the visibility rule, and collects the survivors. `SlenderTakeover`
picks a random survivor when the Slender is replaced during a round.

The database layer comes from `add-yearly-player-statistics`: Hibernate 7 with Flyway migrations, HikariCP, a
no-op repository when `stats.jdbc-url` is empty, and a deletion by UUID. Cygnus has no DI framework; it builds the
services in its constructor. Permission checks go through the existing `PermissionAwarePlayer` / LuckPerms support.

The round's Slender is not known until `TeamHelper` runs. The redemption therefore has two phases: a reservation in
the lobby, which holds no credit, and a debit at round start, which is started when the reservation is honoured and
does not delay the round.

## Goals / Non-Goals

**Goals**

- A player with a pass can claim the Slender role for the next round, visibly only to the lobby as "a pass was
  redeemed".
- At most one claim per round; the claim is honoured only when the round starts with that player online.
- Credit is spent once, atomically, and every change is in an append-only ledger.
- The round start never waits for the database.
- The feature disappears cleanly when no database is configured.

**Non-Goals**

- Earning passes (pity counter, monthly rank quota, cap): `add-slender-pass-earning`.
- Claims across servers: a round is per Cygnus server, so the reservation is in memory per server. Two servers
  sharing one database have independent rounds and independent reservations, but a single balance.
- Persisting a reservation across a restart. A restart drops the reservation and the pass stays.
- Passes for `SlenderTakeover`.
- A pass shop, transfers between players, or expiry of passes.
- Refunds. A failed debit at round start keeps the role for that round (see Round start), so no refund path exists.

## Data model

```
player_slender_pass                       slender_pass_transaction
-------------------                       ------------------------
player_uuid  UUID     PK                  id          BIGINT   PK (identity)
balance      INT      NOT NULL            player_uuid UUID     NOT NULL (indexed)
                      CHECK (balance>=0)  delta       INT      NOT NULL
version      BIGINT   NOT NULL            reason      VARCHAR  NOT NULL
                                                              CHECK (reason IN
                                                              ('ADMIN_GIVE','ADMIN_TAKE',
                                                               'ADMIN_SET','REDEEM'))
                                          round_id    UUID     NULL
                                          created_at  TIMESTAMP NOT NULL (from Clock, UTC)
```

- `slender_pass_transaction` is append-only. `delta` is the actual change, not the requested one: a `take` of 3 on a
  balance of 1 writes `delta = -1`.
- `round_id` is the id from `RoundTracer`, the same id that `player_round_stats` uses, so a redemption can be
  matched to its round.
- The SQL uses only types and statements that both PostgreSQL and H2 in PostgreSQL mode accept. No `ON CONFLICT`
  and no PostgreSQL-only types: creating a balance row uses load-or-create with the same retry as the statistics
  write path.
- `version` is incremented explicitly in the conditional update
  (`SET balance = balance - 1, version = version + 1`). A JPQL bulk update does not bump a `@Version` column by
  itself, so without this the optimistic lock would not see the redemption.

## Lobby reservation

```
right-click item / /pass  -->  tick thread: command event
                              |  (no balance read here)
                              v
                          async: balance(uuid)  --->  tick thread: decide
                                                       |- phase is not LobbyPhase    -> refuse
                                                       |- slot already held          -> refuse, keep pass
                                                       |- balance == 0              -> refuse
                                                       `- else hold slot, announce "a pass was redeemed"
```

- The balance is read asynchronously. The decision runs on the tick thread when the read returns, so the slot is
  claimed in the order in which the reads return. The phrase "first come" in the specification means "first to be
  confirmed".
- Right-click on the item and `/pass` share one path. A second right-click, `/pass cancel`, a disconnect, or leaving
  the lobby release the slot. Releasing does not announce anything.
- The item shows the last balance read. After a successful reservation, the item and `/pass balance` read the balance
  again, so the display is never older than the last change made on this server.
- The announcement names no player. It is sent when the slot is claimed, so the lobby learns that a Slender has been
  chosen at the same moment the holder does, and nothing else.

## Round start

`WaitingPhase` fires `GamePrepareEvent`. `TeamHelper.prepareTeamAllocation` asks `SlenderPassService` first. The
decision is made from in-memory state only; no database call runs on the round start path.

1. No reservation: the random selection runs as before. Nothing is debited.
2. Reservation and the holder is offline: the reservation is dropped, the random selection runs, nothing is debited.
   The pass stays with the player.
3. Reservation and the holder is online: the holder is selected as Slender immediately. The debit starts
   asynchronously as a conditional update (`balance - 1 WHERE balance > 0`) with a `REDEEM` ledger row carrying the
   round id. The round does not wait for the result.
   - Success: the balance is decremented and the ledger row is written.
   - Failure (balance is 0 by now, because an administrator took passes in the meantime, or the database call
     failed): the holder keeps the Slender role for this round. Nothing is written to the ledger. A warning with the
     player UUID, the round id and the reason is logged.

The debit is started before the selection is applied, but the selection never depends on its result. This is the
only place where a pass touches the round start, and it adds no waiting.

## Admin commands

`/pass give|take|set <player> <amount>` requires `cygnus.pass.admin`. The permission check applies to players only;
the console has the rights implicitly and may run these three subcommands. Each command runs the same way as the
redemption: load the balance off the tick thread, write the new balance and one ledger entry in one transaction, and
report the result on the tick thread.

- `give`: `delta = +amount`.
- `take`: `delta = -min(amount, balance)`. If the clamped delta is 0, no ledger entry is written and the command says
  that nothing was removed.
- `set`: `delta = amount - balance`. A `set` to the current value writes no ledger entry.
- The target may be offline; the balance row is created when it does not exist.
- `/pass slender`, `/pass cancel` and `/pass balance` are for players only. The console gets a message that it has no
  pass.

## Feature switch

The selection is `StatsRepository.slenderPasses(clock)`: the statistics repository returns the Hibernate pass repository
on its own pool and executor when `stats.jdbc-url` is set, and the no-op one otherwise. It is the same condition as for
the statistics, so both features stand or fall together. When it is no-op, the lobby item is not given, the
reservation path is not registered, and `/pass` (all subcommands, admin included) answers "feature unavailable".
Round start skips the pass lookup entirely, so the round behaves as before.

## GDPR deletion

`HibernateStatsRepository.deleteByPlayer(uuid)` deletes the rows of both pass tables in the same transaction as the
statistics rows, so either all rows of that UUID are gone or none is. The ledger rows go too.

## Module placement

The repository interface, its implementations, the entities and the migration live in the Gradle module `:stats`, next
to the statistics, because they share its pool, executor and transactions. The lobby service, the feedback, the
command and the item live in `:game`, which depends on `:stats`.

## Decisions

- **Optimistic debit at round start**: the selection does not wait for the database. The debit is conditional and
  asynchronous. A failed debit keeps the role for the round and is logged, instead of reversing the round. The
  player loses at most the pass that an administrator removed in the same moment.
- **Reservation in memory**: a reservation is about the lobby of this server and the next round only. Persisting it
  would add recovery code for a state that is meaningless after a restart.
- **Announce at claim**: the lobby needs to know that the role is taken to understand a quiet lobby, and the
  message names no one.
- **Conditional update for the spend**: the `balance > 0` condition and the decrement are one statement, so the
  "no double spend" guarantee does not depend on reading the balance first.
- **Ledger in the same transaction**: a balance without its ledger entry cannot exist.
- **No `ON CONFLICT`**: keeps the SQL portable to H2 for the integration tests, as the statistics change does.
- **Console for admin commands only**: the console has no pass of its own, so it can only change balances.

## Risks / Trade-offs

- **Failed debit at round start**: a holder can play the Slender without the pass being spent when the balance was
  removed in the same moment, or when the database call fails. The ledger then has no `REDEEM` row for that round,
  which is visible in the warning log. Accepted: the alternative (reversing a started round) is worse for players.
- **Stale balance in the item**: the item can show a balance that another server has just spent. The redemption
  itself is still safe, because the conditional update decides.
- **One reservation per server**: two servers can each have a reservation in their own lobby. Each round is still
  one claim, because each server runs its own round.

## Open questions

1. **Announcement text**: the wording of the lobby announcement is not set here. It must not name the player.
