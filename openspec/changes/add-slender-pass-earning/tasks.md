## Prerequisite

- [ ] 0.1 Confirm `add-slender-pass` is merged: the balance table, the ledger, the repository and the redemption are in
  place in the module `:stats`. This change adds `V3__slender_pass_earning.sql` after `V2`.

## 1. Tests (written first)

- [ ] 1.1 Add `PityCounterTest` (module `:game`, package `net.onelitefeather.cygnus.pass`) with the in-memory repository
  and `RoundStats` snapshots: a Survivor who finishes a round gets +1; a player who quits is not counted; a player who
  is Slender at the start resets to 0; a takeover resets to 0; the tenth round credits one `EARNED_PITY` pass and
  resets the counter; the threshold is read from the config
- [ ] 1.2 Add `MonthlyQuotaTest` with a fixed `Clock` and a fake `MonthlyQuotaSource`: spawn in a new month credits the
  quota once; a second spawn in the same month credits nothing; a spawn after several missed months credits once; a
  missing meta (0) sets the month and credits nothing; the turn of the month in UTC (`2026-10-31T23:30Z` and
  `2026-11-01T00:30Z`)
- [ ] 1.3 Add cap tests: earned credit is reduced to the cap; a balance above the cap is not reduced; an administrator
  `give` above the cap is not reduced; a credit that the cap reduces to 0 writes no ledger row
- [ ] 1.4 Add message tests: one message per credit with delta above 0, none for a credit of 0
- [ ] 1.5 Add `HibernateSlenderPassEarningIntegrationTest` in `stats/src/test/.../stats/` against H2 in PostgreSQL mode
  with `V1` to `V3` and `hbm2ddl=validate`: the threshold credit in one transaction; two conditional quota calls credit
  once; the new reasons pass the check constraint; the rebuild in `V3` keeps the existing ledger rows and their order
- [ ] 1.6 Add config tests in `common/src/test/.../config/`: `pass.pity-rounds` and `pass.max-balance` default to 10 and
  5; `pass.pity-rounds` 0 and `pass.max-balance` -1 are refused with a message that names the key
- [ ] 1.7 Run `./gradlew :stats:test :game:test :common:test` and confirm the new tests fail for the missing production
  code

## 2. Persistence (module `stats`)

- [ ] 2.1 Add Flyway migration `V3__slender_pass_earning.sql`: the two columns, and the ledger rebuild with the replaced
  reason check (named constraint, rows copied in order, index recreated)
- [ ] 2.2 Map `last_quota_month` to `YearMonth` with an `AttributeConverter` and add `rounds_since_slender` to
  `SlenderPassBalance`; add `EARNED_PITY` and `RANK_QUOTA` to `LedgerReason`
- [ ] 2.3 Add the repository methods to `SlenderPassRepository` and implement them in `HibernateSlenderPassRepository`
  (one transaction with the optimistic-lock retry), `InMemorySlenderPassRepository` and `NoOpSlenderPassRepository`:
  the round update with threshold, the conditional quota grant with the cap, the capped credit
- [ ] 2.4 Run `./gradlew :stats:test` and make the persistence tests pass

## 3. Counter and quota logic (module `game`)

- [ ] 3.1 Add the pure classification of a `FinishedRound` (survivor round, slender round, nothing) and the `PityCounter`
  that calls the repository and reports the credits
- [ ] 3.2 Add a consumer of the finished round to `RoundStatsCollector` (a new constructor, the old ones pass a no-op)
  and wire the counter in `Cygnus` with `statsRepository.slenderPasses(...)`
- [ ] 3.3 Add `MonthlyQuota` (the spawn check with `MonthlyQuotaSource`, UTC month from the `Clock`) and wire it to the
  lobby spawn listener in `Cygnus`
- [ ] 3.4 Add the cap to every earned credit and the credit messages (`Messages.java` constants), delivered on the tick
  thread
- [ ] 3.5 Add `SlenderPassConfig` (`pass.pity-rounds`, `pass.max-balance`) to `GameConfig`, `GameConfigReader` and
  `config.properties.example`
- [ ] 3.6 Add `LuckPermsSupport`-backed `MonthlyQuotaSource` in `common` (cached user data, 0 when absent)
- [ ] 3.7 Run `./gradlew :stats:test :game:test :common:test`

## 4. Verification

- [ ] 4.1 Run `./gradlew build`
- [ ] 4.2 Live test: play ten rounds as Survivor on a local database and check the pity pass and its ledger row; set a
  quota meta on a test group and check the spawn grant
- [ ] 4.3 Tick off the completed tasks in this file and run `openspec validate add-slender-pass-earning --strict`

## 5. Pull request

- [ ] 5.1 Open the pull request titled `feat(pass): earn slender passes by playing and through rank quotas` (not opened
  yet)
