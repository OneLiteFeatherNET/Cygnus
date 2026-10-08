## ADDED Requirements

### Requirement: Wake-up spider per player

When a player is teleported into the game map at the start of a round, the system SHALL spawn one invisible,
silent, gravity-free spider in the player's instance that only that player can see. The spider SHALL have no
name tag and SHALL NOT collide with or be pushed by entities.

#### Scenario: Spider is spawned in the player's instance

- **WHEN** the waiting phase teleports a player into the game instance and starts the transition
- **THEN** a spider exists in that instance for that player

#### Scenario: Spider is visible to its player only

- **WHEN** the transition is running for a player and a second player shares the instance
- **THEN** the spider is a viewer for the first player and is not a viewer for the second player

### Requirement: Spider matches the player's eye and view

The spider SHALL be placed so that its eye position equals the player's eye position, and SHALL take the player's
yaw and pitch, so that switching the camera does not change what the player sees.

#### Scenario: Spider position and view follow the player

- **WHEN** the transition starts for a player at position P with yaw Y and pitch T
- **THEN** the spider's x and z equal P's x and z, its y equals P.y + player eye height - spider eye height,
  and its yaw and pitch equal Y and T

### Requirement: Camera points at the spider

The system SHALL point the player's camera at the spider using the spectate API, and SHALL NOT change the
player's game mode to do so.

#### Scenario: Camera is on the spider during the transition

- **WHEN** the transition has started and fewer than 100 ticks have passed
- **THEN** the last camera packet sent to the player targets the spider's entity id

#### Scenario: Game mode is unchanged

- **WHEN** the transition starts for a player
- **THEN** the player's game mode is the same as before the transition started

### Requirement: Transition ends after exactly 100 ticks

The transition SHALL last exactly 100 server ticks. When the time has passed, the system SHALL point the camera
back at the player and SHALL remove the spider. The duration SHALL be counted in ticks, not wall-clock time.

#### Scenario: Camera is still on the spider at 99 ticks

- **WHEN** 99 ticks have passed since the transition started
- **THEN** the camera still targets the spider and the spider is still in the instance

#### Scenario: Camera returns and the spider is removed at 100 ticks

- **WHEN** 100 ticks have passed since the transition started
- **THEN** the last camera packet sent to the player targets the player's own entity id and the spider is removed

### Requirement: Early cleanup

The transition SHALL end early without error when the player disconnects or when the player leaves the instance the
spider is in. Each early end SHALL remove the spider and cancel the scheduled end.

#### Scenario: Disconnect before the end

- **WHEN** the player disconnects 50 ticks into the transition
- **THEN** the spider is removed and no error is raised when the scheduled end would have run

#### Scenario: Leaving the instance before the end

- **WHEN** the player leaves the instance the spider is in 50 ticks into the transition
- **THEN** the spider is removed and the camera is no longer on it

### Requirement: Transition survives the waiting phase end

The end of the waiting phase SHALL NOT end a running transition. The camera SHALL stay on the spider for the full
100 ticks even though the game phase has already started.

#### Scenario: Waiting phase ends during the transition

- **WHEN** the waiting phase ends 50 ticks into the transition
- **THEN** the camera still targets the spider, the spider still exists, and the transition is still running

#### Scenario: Transition still ends at 100 ticks after the waiting phase ends

- **WHEN** the waiting phase ends 50 ticks into the transition and 100 ticks have passed since the transition started
- **THEN** the camera returns to the player and the spider is removed, exactly as without the phase end

### Requirement: Cleanup when the round finishes

The transition SHALL end early without error when the round finishes, that is when a `GameFinishEvent` is dispatched.
Every running transition SHALL be cancelled: the spider is removed, the camera returns to the player, and the
scheduled end is cancelled.

#### Scenario: Game finish before the end

- **WHEN** a `GameFinishEvent` is dispatched 50 ticks into the transition
- **THEN** the spider is removed, the camera returns to the player, and no error is raised when the scheduled end
  would have run

#### Scenario: Game finish is wired through the register method

- **WHEN** the transition is registered on an event node and a `GameFinishEvent` is dispatched on that node
- **THEN** the transition is no longer running for the player
