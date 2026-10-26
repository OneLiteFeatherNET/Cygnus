## Prerequisite

- [ ] 0.1 Confirm `add-yearly-player-statistics` is merged: Flyway, Hibernate, the no-op repository pattern, the `stats.*`
  configuration and the deletion by UUID are in place. This change adds `V2__slender_pass.sql` after its `V1`.
  Deferred: the persistence work (section 2 and the JPA parts of 1.5 and 6.1) follows in a later change, when the
  database infrastructure is on `main`.

## 1. Tests (written first)

- [ ] 1.1 Add `SlenderPassServiceTest` with an in-memory `SlenderPassRepository` fake, a fixed `Clock` and a Minestom
  `Env` per test with explicit `env.tick()`: reserve, second player refused, cancel, disconnect releases, lobby exit
  releases, countdown reset keeps the reservation, zero balance refused, redemption refused outside the lobby
- [ ] 1.2 Add round start tests: holder online and balance available gives Slender without waiting for the debit and
  one `REDEEM` entry with the round id after the debit completes; failed debit keeps the role and logs a warning;
  holder offline falls back without debit; no reservation leaves `TeamHelper` unchanged
- [ ] 1.3 Add admin command tests: `give`, `take` clamped at 0 writes the actual delta, `set` writes the difference,
  missing permission is refused, console may run `give`, unknown player row is created
- [ ] 1.4 Add a feature-off test: with `NoOpSlenderPassRepository` the lobby item is absent and `/pass` answers
  "feature unavailable"
- [ ] 1.5 Deferred with section 2: `HibernateSlenderPassRepositoryIntegrationTest` against H2 in PostgreSQL mode
  (conditional decrement, no decrement at zero, ledger row in the same transaction, optimistic lock, GDPR deletion,
  `hbm2ddl` validation)
- [ ] 1.6 Run `./gradlew :game:test` and confirm the new tests fail for the missing production code

## 2. Persistence (deferred, not part of this branch)

- [ ] 2.1 Add Flyway migration `V2__slender_pass.sql` for `player_slender_pass` and `slender_pass_transaction`
- [ ] 2.2 Add the `SlenderPassRepository` interface, the `NoOpSlenderPassRepository` and the in-memory test fake
- [ ] 2.3 Add the entities and `HibernateSlenderPassRepository`: conditional decrement with `version + 1`, load-or-create
  of the balance row with the statistics retry, ledger insert in the same transaction
- [ ] 2.4 Extend the GDPR deletion by UUID to both tables in one transaction
- [ ] 2.5 Run `./gradlew :game:test` and make the persistence tests pass

## 3. Lobby reservation and command

- [ ] 3.1 Add `SlenderPassService` with the in-memory slot, the async balance read and the tick-thread decision
- [ ] 3.2 Implement reservation, cancel and expiry on disconnect and lobby exit; cover the expiry rules in a test
- [ ] 3.3 Add the lobby item to `Items` and `LobbyPhase`, with the balance in name and lore; right-click reserves and
  cancels
- [ ] 3.4 Add `PassCommand` (`/pass`, `/pass slender`, `/pass cancel`, `/pass balance`), refusing outside the lobby
- [ ] 3.5 Add the lobby announcement that a pass was redeemed, without a player name
- [ ] 3.6 Run `./gradlew :game:test`

## 4. Round start

- [ ] 4.1 Make `TeamHelper.prepareTeamAllocation` ask `SlenderPassService` first and fall back to the random selection
- [ ] 4.2 Start the debit asynchronously at round start without waiting for it; `WaitingPhase` is unchanged
- [ ] 4.3 Implement the conditional debit with the round id; a failed debit keeps the role and logs a warning
- [ ] 4.4 Confirm `SlenderTakeover` is unchanged and add a test that it still picks randomly
- [ ] 4.5 Run `./gradlew :game:test`

## 5. Admin command

- [ ] 5.1 Add `/pass give|take|set <player> <amount>` with permission `cygnus.pass.admin`; the console may run them
- [ ] 5.2 Each admin change writes one ledger entry with the actual delta; no entry when the delta is 0
- [ ] 5.3 Run `./gradlew :game:test`

## 6. Wiring and documentation

- [ ] 6.1 Create the repository, service and commands in the `Cygnus` constructor and register them; always build
  `NoOpSlenderPassRepository` for now, at a single selection point that the JPA change replaces
- [ ] 6.2 Add the pass messages as constants in `Messages.java`
- [ ] 6.3 Run `./gradlew :game:test` and `./gradlew build`
- [ ] 6.4 Live test: redeem in a lobby with a local database, check the ledger and the Slender selection (deferred
  with section 2)
- [ ] 6.5 Tick off the completed tasks in this file and run `openspec validate add-slender-pass --strict`

## 7. Pull request

- [ ] 7.1 Open the pull request titled `feat(pass): let players redeem a slender pass in the lobby` (not part of this
  branch; the PR is opened after the persistence work)
