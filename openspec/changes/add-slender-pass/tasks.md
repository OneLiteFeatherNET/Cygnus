## Prerequisite

- [x] 0.1 Confirm `add-yearly-player-statistics` is on `main`: Flyway, Hibernate, the no-op repository pattern, the
  `stats.*` configuration and the deletion by UUID live in the Gradle module `:stats`. This change adds
  `V2__slender_pass.sql` after its `V1__player_statistics.sql`.

## 1. Tests (written first)

- [x] 1.1 `SlenderPassServiceTest` with an in-memory repository, a fixed `Clock` and a Minestom `Env` per test: reserve,
  second player refused, cancel, disconnect releases, zero balance refused, redemption refused outside the lobby,
  the announcement names no player, the balance read is handled on the tick
- [x] 1.2 Round start tests: holder online gives the Slender and one `REDEEM` row with the round id; a failed debit keeps
  the role and is reported; holder offline drops the reservation without debit; `TeamHelperTest` covers a reserved
  Slender; the spawn listener test covers the lobby hook
- [x] 1.3 Admin tests: `give`, `take` clamped at 0 writes the actual delta, `set` writes the difference, a zero take
  writes nothing, the console may change balances and a player without `cygnus.pass.admin` may not (`PassCommandTest`)
- [x] 1.4 Feature-off test: with the no-op repository `/pass` answers "feature unavailable" and no item is offered
- [x] 1.5 `HibernateSlenderPassRepositoryIntegrationTest` against H2 in PostgreSQL mode with the real migrations and
  `hbm2ddl=validate`: give and ledger row, redeem only above zero, redeem row with round, clamped take, set, GDPR deletion
- [x] 1.6 The new tests were written before the production code and failed to compile without it

## 2. Persistence (module `stats`, package `net.onelitefeather.cygnus.stats`)

- [x] 2.1 Flyway migration `V2__slender_pass.sql` for `player_slender_pass` and `slender_pass_transaction`
- [x] 2.2 `SlenderPassRepository` interface, `NoOpSlenderPassRepository` and `InMemorySlenderPassRepository`
- [x] 2.3 `SlenderPassBalance` and `SlenderPassEntry` entities and `HibernateSlenderPassRepository`: conditional
  decrement with `version + 1`, load-or-create of the balance row, ledger row in the same transaction, the statistics
  retry on conflicts
- [x] 2.4 `HibernateStatsRepository.deleteByPlayer` deletes both pass tables in the same transaction as the statistics
- [x] 2.5 `./gradlew :stats:test` green

## 3. Lobby reservation and command

- [x] 3.1 `SlenderPassService` with the in-memory slot, the async balance read and the tick decision
- [x] 3.2 Reservation, cancel, release on disconnect; the lobby exit and the round start release it as well
- [x] 3.3 The lobby item in `Items` (slot 2), the balance in name and lore, right-click reserves and cancels
- [x] 3.4 `PassCommand` (`/pass`, `/pass slender`, `/pass cancel`, `/pass balance`), refusing outside the lobby
- [x] 3.5 The lobby announcement that a pass was redeemed, without a player name
- [x] 3.6 `./gradlew :game:test` green

## 4. Round start

- [x] 4.1 `TeamHelper.prepareTeamAllocation` takes the reserved player first; the random selection stays the fallback
- [x] 4.2 The debit starts in the background at round start; the round does not wait for it. `WaitingPhase` is unchanged
- [x] 4.3 The conditional debit with the round id; a failed debit keeps the role and is reported with a warning log
- [x] 4.4 `SlenderTakeover` is unchanged (it never reads the pass)
- [x] 4.5 `./gradlew :game:test` green

## 5. Admin command

- [x] 5.1 `/pass give|take|set <player> <amount>` with permission `cygnus.pass.admin`; the console may run them
- [x] 5.2 Each admin change writes one ledger entry with the actual delta; none when the delta is 0
- [x] 5.3 `./gradlew :game:test` green

## 6. Wiring and documentation

- [x] 6.1 `StatsRepository.slenderPasses(clock)` is the single selection point: Hibernate on the statistics pool when
  `stats.jdbc-url` is set, no-op otherwise. `Cygnus` builds the `SlenderPassService` from it
- [x] 6.2 The pass messages are constants in `Messages.java`
- [x] 6.3 `./gradlew :game:test` and `./gradlew build` green
- [ ] 6.4 Live test: redeem in a lobby with a local database, check the ledger and the Slender selection (needs a
  running server with PostgreSQL; not done in this branch)
- [x] 6.5 `openspec validate add-slender-pass --strict`

## 7. Pull request

- [ ] 7.1 Open the pull request titled `feat(pass): let players redeem a slender pass in the lobby` (not opened yet)
