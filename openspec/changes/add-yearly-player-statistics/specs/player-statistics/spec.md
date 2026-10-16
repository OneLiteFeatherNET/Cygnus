## ADDED Requirements

### Requirement: Participants are tracked per round

When a round starts, the system SHALL track every player who is on the Slender or the Survivor team at
`GameStartEvent` as a participant of that round, with the role they started in. Players who join later or only
spectate SHALL NOT be tracked.

#### Scenario: Team members become participants

- **WHEN** a round starts with one Slender and three Survivors
- **THEN** the collector tracks four participants, one with role `SLENDER` and three with role `SURVIVOR`

#### Scenario: Spectators are not tracked

- **WHEN** a player joins as a spectator after `GameStartEvent`
- **THEN** no round statistics are recorded for that player

### Requirement: Gameplay events update the participant's round statistics

The system SHALL update the round statistics of the affected participant for each counted gameplay event: a page
found, a kill credit, a death, damage taken or dealt, a painkiller used, a creek hunt end, a jump scare and a Slender
takeover. Event handling SHALL NOT perform I/O on the tick thread.

#### Scenario: Page found

- **WHEN** a Survivor finds a page
- **THEN** that Survivor's `pages_found` for the round increases by one

#### Scenario: Last page found

- **WHEN** a Survivor finds the page that makes the found count equal the maximum
- **THEN** that Survivor's round statistics mark them as the finisher

#### Scenario: Kill and death

- **WHEN** a Survivor dies and the Slender receives the kill credit
- **THEN** the Survivor's round statistics record the death and the time survived, and the Slender's `kills`
  increases by one

### Requirement: Round outcome is resolved from the finish reason

At `GameFinishEvent` the system SHALL set each remaining participant's outcome to `WIN` or `LOSS` from the finish
reason and their role: Survivors win on `TIME_OVER`, `ALL_PAGES_FOUND` and `SLENDER_LEFT`; the Slender wins on
`ALL_SURVIVOR_DEAD` and `SURVIVOR_LEFT`. A participant who quit before the finish SHALL have the outcome `ABANDONED`.

#### Scenario: Survivors win by pages

- **WHEN** the round finishes with `ALL_PAGES_FOUND`
- **THEN** every Survivor participant has outcome `WIN`, including dead Survivors, and the Slender has outcome `LOSS`

#### Scenario: Player quits mid-round

- **WHEN** a Survivor quits before `GameFinishEvent`
- **THEN** that Survivor's outcome is `ABANDONED` and their play time ends at the quit

### Requirement: Finished rounds are persisted per player

When a round finishes, the system SHALL persist one round record per participant, keyed by round id and player UUID,
in a single transaction. The round id SHALL be the id that the round telemetry uses.

#### Scenario: Round rows are written

- **WHEN** a round with four participants finishes and statistics are enabled
- **THEN** four round records with the same round id exist in the database

#### Scenario: Persistence runs off the tick thread

- **WHEN** the round records are written
- **THEN** the write runs on the statistics executor and `GameFinishEvent` handling returns without waiting for it

### Requirement: Statistics are aggregated per calendar year

The system SHALL keep one yearly aggregate per player and calendar year. The year SHALL be the calendar year of the
round end in the configured time zone (default `Europe/Berlin`). Counters SHALL be summed; best-round values SHALL keep
the maximum and the fastest win SHALL keep the minimum.

#### Scenario: First round of a year creates the aggregate

- **WHEN** a player finishes their first round in 2026
- **THEN** a yearly aggregate for that player and 2026 exists with `rounds_played = 1`

#### Scenario: Further rounds add up

- **WHEN** a Slender with 2 kills in the 2026 aggregate finishes another round in 2026 with 3 kills
- **THEN** the aggregate has `kills = 5` and `best_kills_round = 3`

#### Scenario: Turn of the year uses the configured zone

- **WHEN** a round finishes at `2026-12-31T23:30Z` with the time zone `Europe/Berlin`
- **THEN** the round counts for the year 2027

#### Scenario: Concurrent writes to one aggregate

- **WHEN** two servers update the same player's aggregate for the same year at the same time
- **THEN** both rounds are included in the aggregate after the optimistic-lock retry

#### Scenario: Two servers create the same aggregate

- **WHEN** two servers finish a round for the same player at the same time and no aggregate for that year exists yet
- **THEN** exactly one aggregate exists afterwards and it includes both rounds

### Requirement: Round rows carry the lobby size and the takeover flag

Every round row SHALL carry `participants`, the number of tracked participants (Slender and Survivors at the round
start), with the same value on every row of the round. When a survivor becomes the Slender because the original Slender
left, the row of that survivor SHALL have `taken_over` set while `role` keeps the role at the round start. The yearly
aggregate SHALL sum the takeovers in `takeovers`.

#### Scenario: Lobby size on every row

- **WHEN** a round with one Slender and three Survivors finishes
- **THEN** all four rows have `participants` 4, also the row of a player who quit during the round

#### Scenario: Survivor takes over as Slender

- **WHEN** the Slender leaves and a survivor becomes the Slender (`SlenderReviveEvent`)
- **THEN** only that survivor's row has `taken_over` set and its `role` is still `SURVIVOR`
- **AND** the `takeovers` of that player's yearly aggregate increases by one

#### Scenario: Round without takeover

- **WHEN** a round finishes and no survivor took over
- **THEN** no row of the round has `taken_over` set

#### Scenario: Recent history reads use an index

- **WHEN** the database schema is migrated
- **THEN** an index on `player_round_stats (player_uuid, finished_at)` exists

### Requirement: Persistence failures do not affect the game

A failed write SHALL be logged with the round id and counted in the metric `cygnus.stats.write.failures`. It SHALL
NOT prevent the round from ending or the server from restarting.

#### Scenario: Database unavailable

- **WHEN** the database is unreachable at the round end
- **THEN** the round ends and the restart phase starts as without statistics, and the failure metric increases by one

### Requirement: Statistics can be disabled

When no `stats.jdbc-url` is configured, the system SHALL run without a database and SHALL NOT open any connection.

#### Scenario: No JDBC URL configured

- **WHEN** the server starts without `stats.jdbc-url`
- **THEN** rounds run as before and no statistics are written

### Requirement: A player's statistics can be deleted

The repository SHALL delete all round records and yearly aggregates of a given player UUID in one transaction.

#### Scenario: Deletion by UUID

- **WHEN** the statistics of a player are deleted
- **THEN** no round record and no yearly aggregate with that UUID remains
