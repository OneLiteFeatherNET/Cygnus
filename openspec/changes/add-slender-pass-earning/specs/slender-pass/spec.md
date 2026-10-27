## MODIFIED Requirements

### Requirement: Every balance change is recorded in the ledger

Every change of a player's pass balance SHALL be written as one row in the append-only ledger in the same transaction
as the balance change. The ledger row SHALL carry the actual delta, the reason and the time from the injected clock.
The reasons are `ADMIN_GIVE`, `ADMIN_TAKE`, `ADMIN_SET`, `REDEEM`, `EARNED_PITY` and `RANK_QUOTA`. A balance SHALL
NEVER be negative.

#### Scenario: Admin give writes a ledger row

- **WHEN** an administrator gives 2 passes to a player with balance 1
- **THEN** the balance is 3 and one ledger row with delta `+2` and reason `ADMIN_GIVE` exists

#### Scenario: Balance cannot go negative

- **WHEN** a redemption is attempted on a balance of 0
- **THEN** the balance stays 0 and no ledger row is written

#### Scenario: Earned credit is recorded with its reason

- **WHEN** a player earns a pass through the pity counter
- **THEN** one ledger row with delta `+1` and reason `EARNED_PITY` exists

## ADDED Requirements

### Requirement: Pity counter grants a pass after a fixed number of Survivor rounds

The system SHALL count, per player, the rounds that the player played to the end as a Survivor. When the count
reaches `pass.pity-rounds`, the system SHALL credit one pass with reason `EARNED_PITY` and reset the count to 0. The
count SHALL be reset to 0 whenever the player becomes the Slender, by any path. A player who leaves the round before
the finish SHALL NOT be counted.

#### Scenario: Survivor round is counted

- **WHEN** a Survivor is still in the round when the round ends (the round snapshot of the statistics)
- **THEN** that player's count increases by one

#### Scenario: Early leaver is not counted

- **WHEN** a Survivor quits before the round ends
- **THEN** that player's count does not change

#### Scenario: Becoming Slender resets the count

- **WHEN** a player's count is 6 and the player is chosen as Slender at round start, whether by pass, by chance or by takeover
- **THEN** the count is 0 once that round has ended

#### Scenario: Takeover resets the count

- **WHEN** a survivor with count 3 is taken over as the Slender during a round and the round ends
- **THEN** that player's count is 0

#### Scenario: Threshold earns a pass

- **WHEN** a Survivor's count is 9 and the round ends with that player still a Survivor, with
  `pass.pity-rounds` set to 10
- **THEN** the count is 0, the balance has increased by one with reason `EARNED_PITY`, and the player is told

### Requirement: Monthly rank quota is credited once per calendar month

When a player joins, the system SHALL read the LuckPerms meta `cygnus.slender-pass.monthly-quota` of the player. When
the player's last quota month is before the current month (UTC, from the injected clock), the system SHALL credit the
quota with reason `RANK_QUOTA`, set the last quota month to the current month, and tell the player. Missed months
SHALL NOT be paid back. A quota of 0 or a missing meta SHALL set the last quota month and credit nothing.

#### Scenario: First join of the month credits the quota

- **WHEN** a player with meta quota 2 joins in October 2026 and the last quota month is September 2026
- **THEN** the balance increases by 2 with reason `RANK_QUOTA`, the last quota month is `2026-10`, and the player is told

#### Scenario: Second join in the same month credits nothing

- **WHEN** the same player joins again in October 2026
- **THEN** the balance does not change and no ledger row is written

#### Scenario: Missed months are not paid back

- **WHEN** a player whose last quota month is `2026-03` with meta quota 2 joins in `2026-10`
- **THEN** the balance increases by 2 once and not per missed month

#### Scenario: Turn of the month uses UTC

- **WHEN** a player joins at `2026-10-31T23:30Z` and then at `2026-11-01T00:30Z`, with the same meta quota
- **THEN** the two joins credit the quota once each

#### Scenario: Two servers see the same join

- **WHEN** two servers process a join of the same player in the same month at the same time
- **THEN** exactly one credit is written

### Requirement: Earned credit is capped by the maximum balance

Earned credit (`EARNED_PITY` and `RANK_QUOTA`) SHALL be limited so that the balance does not exceed `pass.max-balance`.
The ledger row SHALL carry the actual delta. A balance that is already above the cap SHALL NOT be reduced, and earned
credit SHALL add nothing until the balance is below the cap. Administrator changes SHALL NOT be limited by the cap.

#### Scenario: Credit is reduced to the cap

- **WHEN** a player with balance 4 earns a pass and `pass.max-balance` is 5
- **THEN** the balance is 5 and the ledger row has delta `+1`

#### Scenario: Credit at the cap is nothing

- **WHEN** a player with balance 5 earns a pass
- **THEN** the balance stays 5 and no ledger row is written

#### Scenario: Balance above the cap is not reduced

- **WHEN** an administrator has set a balance of 7 and the player earns a pass with `pass.max-balance` 5
- **THEN** the balance stays 7

#### Scenario: Administrator give is not capped

- **WHEN** an administrator gives 3 passes to a player with balance 4 and `pass.max-balance` is 5
- **THEN** the balance is 7 and the ledger row has delta `+3` and reason `ADMIN_GIVE`

### Requirement: Credits are announced to the player

Every earned credit with a delta above 0 SHALL send one message to the player that names the reason and the new
balance.

#### Scenario: Pity credit message

- **WHEN** a pity pass is credited with a delta of 1 and the new balance is 3
- **THEN** the player receives one message with the reason and balance 3

#### Scenario: No message without credit

- **WHEN** a credit is reduced to 0 by the cap
- **THEN** the player receives no credit message

### Requirement: Earning settings are configurable

The system SHALL read `pass.pity-rounds` (default 10, at least 1) and `pass.max-balance` (default 5, at least 0) from
the server configuration. Values below their minimum SHALL stop the server at start.

#### Scenario: Defaults

- **WHEN** the server starts without the two keys
- **THEN** the pity threshold is 10 and the cap is 5

#### Scenario: Invalid pity threshold

- **WHEN** `pass.pity-rounds` is 0
- **THEN** the server refuses to start with a message that names the key
