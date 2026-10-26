## ADDED Requirements

### Requirement: Every balance change is recorded in the ledger

Every change of a player's pass balance SHALL be written as one row in the append-only ledger in the same transaction
as the balance change. The ledger row SHALL carry the actual delta, the reason and the time from the injected clock.
A balance SHALL NEVER be negative.

#### Scenario: Admin give writes a ledger row

- **WHEN** an administrator gives 2 passes to a player with balance 1
- **THEN** the balance is 3 and one ledger row with delta `+2` and reason `ADMIN_GIVE` exists

#### Scenario: Balance cannot go negative

- **WHEN** a redemption is attempted on a balance of 0
- **THEN** the balance stays 0 and no ledger row is written

### Requirement: Redemption is reserved in the lobby

A player with a balance of at least one SHALL be able to reserve the Slender role for the next round in the lobby,
either by right-clicking the redemption item or with `/pass` (alias `/pass slender`). A second right-click or
`/pass cancel` SHALL cancel the reservation. A reservation SHALL NOT change the balance.

#### Scenario: Reserving with the item

- **WHEN** a player with balance 2 right-clicks the redemption item in the lobby
- **THEN** the player holds the reservation and the balance is still 2

#### Scenario: Cancelling with a second right-click

- **WHEN** a player who holds the reservation right-clicks the item again
- **THEN** the reservation is released and the balance is unchanged

#### Scenario: Cancelling with the command

- **WHEN** a player who holds the reservation runs `/pass cancel`
- **THEN** the reservation is released and the balance is unchanged

#### Scenario: Balance is shown

- **WHEN** a player runs `/pass balance`
- **THEN** the player sees the current balance

### Requirement: Redemption is allowed only in the lobby

The system SHALL accept a reservation only while the lobby phase is running. Outside the lobby phase, the item and the
reservation commands SHALL be refused with a message.

#### Scenario: Reservation during the game

- **WHEN** a player runs `/pass` while the game phase is running
- **THEN** no reservation is made and the player receives a message that redemption is closed

### Requirement: At most one reservation per round, first come first served

At most one reservation SHALL exist per round. A player who requests a reservation while another player holds it
SHALL be refused with a message, and SHALL keep the pass. A player with a balance of zero SHALL be refused.

#### Scenario: Second player is refused

- **WHEN** player A holds the reservation and player B requests one
- **THEN** B receives a refusal message, B's balance is unchanged, and A still holds the reservation

#### Scenario: Slot is free again after a cancel

- **WHEN** player A cancels the reservation and player B then requests one with a balance of 1
- **THEN** B holds the reservation

#### Scenario: Zero balance is refused

- **WHEN** a player with balance 0 requests a reservation
- **THEN** the request is refused and no reservation exists

### Requirement: Lobby learns that a pass was redeemed without the player name

When a reservation is made, every player in the lobby SHALL receive a message that a pass was redeemed. The message
SHALL NOT name the player.

#### Scenario: Lobby announcement

- **WHEN** player A reserves the Slender role in the lobby with players B and C present
- **THEN** B and C each receive the redemption message, and the message contains neither the name of A nor any other
  player name

#### Scenario: Cancel is not announced

- **WHEN** a reservation is cancelled
- **THEN** no redemption message is sent to the lobby

### Requirement: A reservation ends when its holder leaves the lobby

A reservation SHALL be released when its holder disconnects or leaves the lobby before the round starts. A countdown
reset or a force start SHALL NOT release it.

#### Scenario: Disconnect releases the reservation

- **WHEN** the holder disconnects
- **THEN** the reservation is released and the next player may reserve

#### Scenario: Countdown reset keeps the reservation

- **WHEN** the lobby countdown is reset while a reservation exists
- **THEN** the reservation still exists

#### Scenario: Force start keeps the reservation

- **WHEN** an administrator force starts the round while a reservation exists
- **THEN** the reservation is honoured at the round start

### Requirement: Round start honours a reservation of an online player without waiting for the database

At the round start, when a reservation exists and its holder is online, the system SHALL select the holder as the
Slender instead of drawing at random. It SHALL then start the debit of one pass with reason `REDEEM` and the round id
asynchronously, and SHALL NOT wait for its result before the round proceeds.

#### Scenario: Reserved player becomes Slender

- **WHEN** the round starts with a reservation of an online player whose balance is 2
- **THEN** that player is the Slender, and after the debit has completed the balance is 1 and one ledger row with
  delta `-1`, reason `REDEEM` and the round id exists

#### Scenario: Selection does not wait for the debit

- **WHEN** the round starts with a reservation and the debit has not completed yet
- **THEN** the holder is the Slender and the round proceeds on the same tick

### Requirement: A failed debit keeps the role for the round

When the debit fails at round start, because the balance is zero by then or the database call fails, the holder SHALL
keep the Slender role for that round. No ledger row SHALL be written for the failed debit, and the system SHALL log a
warning with the player UUID, the round id and the reason.

#### Scenario: Balance dropped to zero before the start

- **WHEN** the round starts with a reservation whose holder has balance 0 at the debit
- **THEN** the holder is the Slender for this round, the balance stays 0, no ledger row is written, and a warning is
  logged

#### Scenario: Database call fails at the start

- **WHEN** the round starts with a reservation and the debit fails with a database error
- **THEN** the holder is the Slender for this round, no ledger row is written, and a warning is logged

### Requirement: Reservation of an offline player is dropped without debit

When the holder of a reservation is offline at the round start, the system SHALL drop the reservation, draw the
Slender at random and SHALL NOT debit any pass.

#### Scenario: Holder offline at the start

- **WHEN** the round starts and the holder of the reservation is not online
- **THEN** the Slender is drawn at random, the balance is unchanged and no ledger row is written

### Requirement: SlenderTakeover ignores passes

The takeover of the Slender during a round SHALL keep choosing a survivor at random, and SHALL NOT use or debit a pass.

#### Scenario: Takeover does not redeem

- **WHEN** the Slender is replaced during a round while a player holds a pass
- **THEN** the replacement is chosen at random and the pass balance is unchanged

### Requirement: Administrators change balances with give, take and set

Players with the permission `cygnus.pass.admin` and the console SHALL be able to run `/pass give`, `/pass take` and
`/pass set` with a player name and an amount. Each change SHALL be recorded in the ledger with reason `ADMIN_GIVE`,
`ADMIN_TAKE` or `ADMIN_SET`. `take` SHALL NOT reduce a balance below zero.

#### Scenario: Take is clamped

- **WHEN** an administrator takes 3 passes from a player with balance 1
- **THEN** the balance is 0 and the ledger row has delta `-1` and reason `ADMIN_TAKE`

#### Scenario: Set writes the difference

- **WHEN** an administrator sets a balance of 1 to 4
- **THEN** the balance is 4 and the ledger row has delta `+3` and reason `ADMIN_SET`

#### Scenario: Taking nothing writes nothing

- **WHEN** an administrator takes passes from a player with balance 0
- **THEN** the balance stays 0 and no ledger row is written

#### Scenario: Missing permission

- **WHEN** a player without `cygnus.pass.admin` runs `/pass give`
- **THEN** the command is refused and the balance is unchanged

#### Scenario: Console may give

- **WHEN** the console runs `/pass give` for a player with balance 1 and amount 2
- **THEN** the balance is 3 and one ledger row with delta `+2` and reason `ADMIN_GIVE` exists

#### Scenario: Offline target

- **WHEN** an administrator gives a pass to a player who has never had a balance
- **THEN** the balance row is created with balance 1

### Requirement: The feature is off without a database

When `stats.jdbc-url` is not configured, the system SHALL NOT show the redemption item, SHALL answer every `/pass`
subcommand with "feature unavailable", and SHALL run rounds as before.

#### Scenario: No database configured

- **WHEN** the server starts without `stats.jdbc-url` and a player joins the lobby
- **THEN** the player gets no redemption item and `/pass balance` answers "feature unavailable"

### Requirement: Database access stays off the tick thread

Every balance read and write SHALL run off the tick thread. The tick thread SHALL only apply the result of a finished
operation.

#### Scenario: Reservation does not block the tick

- **WHEN** a reservation request is made and the balance read is still running
- **THEN** the server tick continues and the decision is applied on a later tick

### Requirement: GDPR deletion covers both pass tables

The deletion of a player's data by UUID SHALL delete the row of `player_slender_pass` and all ledger rows of
`slender_pass_transaction` for that UUID in one transaction.

#### Scenario: Deletion by UUID

- **WHEN** the data of a player is deleted
- **THEN** no balance row and no ledger row with that UUID remains
