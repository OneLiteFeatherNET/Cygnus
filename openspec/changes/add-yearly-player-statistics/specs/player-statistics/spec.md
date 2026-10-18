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

The repository SHALL delete all round records, yearly aggregates and the stored name of a given player UUID in one
transaction.

#### Scenario: Deletion by UUID

- **WHEN** the statistics of a player are deleted
- **THEN** no round record, no yearly aggregate and no name with that UUID remains

### Requirement: Players can see statistics

The system SHALL let every player look at the statistics of every player, including players who are offline and
played before. `/stats [player] [year]` SHALL print a compact overview in chat with a general, a Survivor and a Slender
section; the year SHALL default to the current calendar year in the configured time zone. Win rate, K/D and averages
SHALL be computed when read and SHALL be `0` instead of a division by zero.

#### Scenario: Own statistics

- **WHEN** a player runs `/stats`
- **THEN** the overview of their current year is printed

#### Scenario: Another player, online or offline

- **WHEN** a player runs `/stats Name 2025`
- **THEN** the overview of that player for 2025 is printed, whether the player is online or only known by their stored name

#### Scenario: Unknown player or empty year

- **WHEN** nobody was ever seen with the name, or the player has no rounds in the year
- **THEN** the sender is told so, and in the second case also which years have statistics

#### Scenario: Invalid year

- **WHEN** the year is not a number between 2000 and 9999
- **THEN** the usage message is shown and nothing is looked up

#### Scenario: No rounds, no division by zero

- **WHEN** the overview of a year without rounds is formatted
- **THEN** every rate, ratio and average is shown as zero

### Requirement: The overview opens a menu

The chat overview SHALL carry a clickable part that opens a read-only chest menu for the same player and year, with
one item per area. Every click and drag in the menu SHALL be cancelled.

#### Scenario: Click on the overview

- **WHEN** the player clicks the clickable part
- **THEN** the menu of the same player and year opens

#### Scenario: Menu is read-only

- **WHEN** a player clicks, drags or shift-clicks in the open menu or their own inventory
- **THEN** the click is cancelled and nothing moves

### Requirement: A lobby item opens the player's own menu

While the lobby or the waiting phase runs, a player SHALL carry a hotbar item whose right click opens their own
current-year menu. The item SHALL be removed when the round starts, SHALL NOT be droppable, swappable or movable, and
SHALL NOT be given while statistics are disabled. The player's current year SHALL be loaded asynchronously on join and
cached until they leave or a round finishes; without a cached view the player SHALL be told that it loads.

#### Scenario: Item in the lobby

- **WHEN** a player joins during the lobby phase
- **THEN** the item is in their hotbar and their year is loading

#### Scenario: Item removed at the start

- **WHEN** the round starts
- **THEN** no player carries the item any more and the other items are unchanged

#### Scenario: Statistics disabled

- **WHEN** statistics are disabled
- **THEN** no item is given and `/stats` answers that statistics are not available

#### Scenario: Cache miss

- **WHEN** a player right-clicks the item before their year is loaded
- **THEN** they see "Loading statistics…" and the menu opens once the year is loaded

### Requirement: The round summary shows the updated totals

After a round was committed, every online participant SHALL get one extra line with their updated totals of the year
(rounds, wins, pages, kills). The line SHALL be sent on the server thread. When the write fails or statistics are
disabled the line SHALL be left out and the round end SHALL be unaffected.

#### Scenario: Committed totals

- **WHEN** the round was written and the transaction committed
- **THEN** each online participant receives their year totals including the round

#### Scenario: Failed write

- **WHEN** the write failed
- **THEN** no line is sent and the failure is logged and counted as before

### Requirement: The last known name is stored

The system SHALL store the last known name of each participant, case-insensitively searchable, in the transaction that
stores the round. The newest write SHALL win on a rename, and a name that moved to another player SHALL resolve to the
most recently updated row. Deleting a player SHALL delete the name as well. Lookups SHALL run off the tick thread.

#### Scenario: Rename

- **WHEN** a player is seen with a new name
- **THEN** the new name finds the player and the old name no longer does

#### Scenario: Name reuse

- **WHEN** a name stored for one player is now used by another
- **THEN** a lookup returns the player whose name was seen last

#### Scenario: Deletion

- **WHEN** the statistics of a player are deleted
- **THEN** the stored name is deleted with them

### Requirement: Failures of a lookup are reported without details

A failed read SHALL be logged and SHALL tell the player only that statistics could not be loaded.

#### Scenario: Database error

- **WHEN** reading the statistics fails
- **THEN** the player sees "Could not load statistics, please try again later." and the cause is in the log
