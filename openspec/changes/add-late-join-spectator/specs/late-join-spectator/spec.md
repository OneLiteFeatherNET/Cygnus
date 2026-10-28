## ADDED Requirements

### Requirement: Joining in the lobby is unchanged

A player who joins while the lobby phase is running SHALL become a normal player, and the lobby limit `maxPlayers`
SHALL apply to them as before.

#### Scenario: Lobby join

- **WHEN** a player connects while the lobby phase is running and fewer than `maxPlayers` players are online
- **THEN** the player is admitted as a normal player in the lobby

#### Scenario: Lobby full

- **WHEN** a player connects in the lobby while `maxPlayers` players are online
- **THEN** the player is refused with the server full message, as before

### Requirement: A player who joins a running round becomes a spectator

A player who joins while the round is in the `WaitingPhase`, `GamePhase` or `RestartPhase` SHALL become a spectator
at the first spawn, in the running map's instance, and SHALL receive the resource pack like every other player.

#### Scenario: Join during the game

- **WHEN** a player connects while the `GamePhase` is running and a late place is free
- **THEN** the player spawns in the running map as a spectator with the spectator team tag, flying, with the
  spectator hotbar and the struck-through tab name, and the resource pack is sent

#### Scenario: Join while the teams are being prepared

- **WHEN** a player connects while the `WaitingPhase` is running and a late place is free
- **THEN** the player becomes a spectator and is not assigned to the survivor or slender team

#### Scenario: Join after the round ended

- **WHEN** a player connects while the `RestartPhase` is running and a late place is free
- **THEN** the player becomes a spectator

### Requirement: Late joiners are limited by maxSpectators

The round setting `maxSpectators` SHALL limit how many late joiners may be spectators at the same time. The default
SHALL be `8`. The value SHALL be zero or more. A value of `0` SHALL turn late joining off.

#### Scenario: Default limit

- **WHEN** the configuration sets no `maxSpectators`
- **THEN** the limit is `8`

#### Scenario: Limit reached

- **WHEN** a player connects during the `GamePhase` while `maxSpectators` late joiners are connected
- **THEN** the player is refused with a message that the spectator places are full, and the refusal is delivered in
  the play state, not during configuration

#### Scenario: Limit zero

- **WHEN** `maxSpectators` is `0` and a player connects during the `GamePhase`
- **THEN** the player is refused with the game already started message, as before

#### Scenario: Negative value is rejected

- **WHEN** the configuration sets `maxSpectators` to a negative number
- **THEN** the configuration is refused with the same error as the other round values

### Requirement: Only late joiners count against maxSpectators

Players who were alive in the round and became spectators by dying SHALL NOT count against `maxSpectators`. The
lobby limit `maxPlayers` SHALL NOT apply to a player who joins a running round.

#### Scenario: Dead players do not use a late place

- **WHEN** a survivor dies during the round and a late joiner connects while `maxSpectators` late joiners are not yet
  connected
- **THEN** the late joiner is admitted, because the dead survivor holds no late place

#### Scenario: Running round above maxPlayers

- **WHEN** the round started with `maxPlayers` players and a player connects during the `GamePhase` with a late place
  free
- **THEN** the player is admitted and is not refused with the server full message

### Requirement: Two joiners cannot take the last late place

The check for a free late place and the reservation of that place SHALL happen as one step, so that two players
connecting at the same time cannot both take the last place.

#### Scenario: Two joiners for one place

- **WHEN** `maxSpectators` is 1, no late joiner is connected, and two players connect during the `GamePhase`
- **THEN** exactly one player is admitted as a late joiner and the other is refused with the spectator places full
  message

### Requirement: A late joiner frees the place when leaving

A late place SHALL be released when its holder disconnects, including a holder who was refused or left before the
spawn.

#### Scenario: Late joiner leaves

- **WHEN** a late joiner disconnects during the `GamePhase`
- **THEN** the late place is free again and the next player may join as a late joiner

#### Scenario: Refused joiner holds no place

- **WHEN** a player is refused with the game already started message and disconnects
- **THEN** no late place is held by that player

### Requirement: A late joiner is not a round participant

A late joiner SHALL NOT be a participant of the round. A late joiner SHALL NOT get a statistics row, SHALL NOT make
pity progress, SHALL NOT use a Slender pass, SHALL NOT be counted as a living survivor, and SHALL NOT end or decide the
round.

#### Scenario: No statistics row

- **WHEN** the round ends with a late joiner connected
- **THEN** the statistics participants do not contain the late joiner

#### Scenario: No pity progress or pass use

- **WHEN** a late joiner is connected during a round in which pity progress is earned and a pass is redeemed
- **THEN** the late joiner's pity progress and pass balance are unchanged

#### Scenario: Not a living survivor

- **WHEN** the last living survivor dies while a late joiner is connected
- **THEN** the round ends exactly as it would without the late joiner

### Requirement: A late spectator leaving has no round effect

When a late spectator disconnects, the system SHALL NOT end the round and SHALL NOT start the Slender takeover. Only
the late place SHALL be released.

#### Scenario: Late spectator quits

- **WHEN** a late spectator disconnects while the `GamePhase` is running
- **THEN** the round continues, no takeover starts, and the late place is free again

### Requirement: The join is not announced to the living

When a late joiner is admitted, the system SHALL NOT send a join message to the living players. The joiner SHALL
receive a short message that they are watching the round. Other spectators MAY receive the join message.

#### Scenario: Living players hear nothing

- **WHEN** a late joiner is admitted while survivors and the Slender are playing
- **THEN** no survivor and no Slender receives a join message

#### Scenario: Joiner is told they are watching

- **WHEN** a late joiner is admitted
- **THEN** the joiner receives a message that they are watching the round

### Requirement: Late spectators keep the spectator rules

A late spectator SHALL get the same spectator rules as a player who died in the round: they are invisible in the world
to the living players, can fly, read the chat of the living players, and their own chat stays with the spectators.

#### Scenario: Invisible to the living

- **WHEN** a late joiner is a spectator and a survivor is near them in the world
- **THEN** the survivor does not see the late spectator; the tab list shows them struck through, like a dead player

#### Scenario: Chat stays with spectators

- **WHEN** a late spectator writes a chat message
- **THEN** only spectators receive the message
