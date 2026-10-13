## ADDED Requirements

### Requirement: Wake-up spider per player

When a player is teleported into the game map at the start of a round, the system SHALL spawn one invisible,
silent, gravity-free spider in the player's instance that only that player can see. The spider SHALL have no
name tag and SHALL NOT collide with or be pushed by entities.

#### Scenario: Spider is spawned in the game instance on arrival

- **WHEN** the waiting phase starts the transition for a player with the game instance as target, and the player
  arrives in that instance
- **THEN** a spider exists in the game instance for that player

#### Scenario: Spider is spawned at once when the player is already in the target

- **WHEN** the transition starts for a player who is already in the target instance
- **THEN** the spider exists in that instance before the next server tick

#### Scenario: Spider is visible to its player only

- **WHEN** the transition is running for a player and a second player shares the instance
- **THEN** the spider is a viewer for the first player and is not a viewer for the second player

### Requirement: Spider waits for the player's arrival in the target instance

The transition SHALL take the target instance it is started with, and SHALL NOT take the player's current instance
as the spider's instance. The player's instance switch to the target is deferred, so the transition SHALL wait until
the player is in the target instance before it spawns the spider. The spider SHALL be placed at the player's position
at the moment of arrival, which is the position the team teleport gave the player in the game instance. The system
SHALL check for the arrival on every server tick.

#### Scenario: Nothing is spawned before the player arrives

- **WHEN** the transition is started with a game instance as target while the player is still in the lobby
- **THEN** no spider exists in the game instance and the transition is still running

#### Scenario: Spider is placed at the arrival position

- **WHEN** the player arrives in the game instance at position Q and the next server tick runs
- **THEN** the spider is in the game instance at Q's x and z, and the transition has spawned it exactly once

#### Scenario: Leaving the lobby does not cancel the transition

- **WHEN** the player leaves the lobby instance while the transition waits for the arrival in the game instance
- **THEN** the transition is still running and no spider is removed

### Requirement: Spider matches the player's eye and view

The spider SHALL be placed so that its eye position equals the player's eye position, and SHALL take the player's
yaw and pitch, so that switching the camera does not change what the player sees.

#### Scenario: Spider position and view follow the player

- **WHEN** the spider is spawned for a player at position P with yaw Y and pitch T
- **THEN** the spider's x and z equal P's x and z, its y equals P.y + player eye height - spider eye height,
  and its yaw and pitch equal Y and T

### Requirement: Camera switches only once the client can know the spider

The system SHALL NOT point the player's camera at the spider until the player is in the spider's instance, the
spider's spawn is complete and the spider is a viewer of the player. After that, the system SHALL wait
`CAMERA_DELAY_TICKS` (2) more server ticks before it sends the camera packet, so that the spider's spawn packets
are sent before the camera packet. The system SHALL check this on every server tick and SHALL NOT block or wait on
wall-clock time.

#### Scenario: Camera is not switched before the spider is viewable

- **WHEN** the transition starts and the spider is not yet a viewer of the player
- **THEN** no camera packet targeting the spider is sent

#### Scenario: Camera switches on a later tick than the spider became viewable

- **WHEN** the spider becomes a viewer of the player on tick N
- **THEN** the camera packet targeting the spider is sent on a tick after N, at least `CAMERA_DELAY_TICKS` ticks after
  the first tick on which the spider was viewable

#### Scenario: Game mode is unchanged

- **WHEN** the transition starts for a player
- **THEN** the player's game mode is the same as before the transition started

### Requirement: Pending transition is abandoned after a timeout

If the camera has not been switched within `PENDING_TIMEOUT_TICKS` (100) server ticks counted from the start of the
transition, the system SHALL abandon the transition: the spider is removed if it exists, the camera is never switched
to the spider and the transition is no longer running. The abandon SHALL be logged at INFO level with the reason.

#### Scenario: Player never arrives in the target instance

- **WHEN** the player never enters the target instance and 100 ticks pass since the start
- **THEN** no spider is spawned, no camera packet targeting a spider is sent, and the transition is no longer running

#### Scenario: Spider never becomes viewable

- **WHEN** the player arrived and the spider is still not viewable for the player after 100 ticks since the start
- **THEN** the spider is removed, no camera packet targeting the spider is sent, and the transition is no longer running

#### Scenario: Transition is not abandoned before the timeout

- **WHEN** 99 ticks pass since the start and the player has not arrived yet
- **THEN** the transition is still running

### Requirement: Camera points at the spider

The system SHALL point the player's camera at the spider using the spectate API, and SHALL NOT change the
player's game mode to do so.

#### Scenario: Camera is on the spider during the transition

- **WHEN** the camera has been switched and fewer than 100 ticks have passed since the switch
- **THEN** the last camera packet sent to the player targets the spider's entity id

### Requirement: Open transition ends after exactly 100 ticks from the camera switch

An open transition SHALL keep the camera on the spider for exactly 100 server ticks counted from the moment the camera
is switched, not from the start of the transition. When the time has passed, the system SHALL point the camera back
at the player and SHALL remove the spider. The duration SHALL be counted in ticks, not wall-clock time.

#### Scenario: Camera is still on the spider at 99 ticks after the switch

- **WHEN** 99 ticks have passed since the camera was switched to the spider
- **THEN** the camera still targets the spider and the spider is still in the instance

#### Scenario: Camera returns and the spider is removed at 100 ticks after the switch

- **WHEN** 100 ticks have passed since the camera was switched to the spider
- **THEN** the last camera packet sent to the player targets the player's own entity id and the spider is removed

### Requirement: Early cleanup

The transition SHALL end early without error when the player disconnects at any point, or when the player leaves the
target instance after having arrived there, whether the camera is still pending or already on the spider. Leaving any
other instance, such as the lobby, SHALL NOT end the transition. Each early end SHALL remove the spider if it exists
and cancel the scheduled checks. A pending transition that ends early SHALL NOT send a camera packet.

#### Scenario: Disconnect before the camera switch

- **WHEN** the player disconnects while the transition is pending
- **THEN** the spider is removed, no camera packet targeting the spider is sent, and the transition is no longer running

#### Scenario: Disconnect while waiting for the arrival

- **WHEN** the player disconnects before arriving in the target instance
- **THEN** no spider exists, no camera packet is sent, and the transition is no longer running

#### Scenario: Disconnect after the camera switch

- **WHEN** the player disconnects 50 ticks after the camera was switched to the spider
- **THEN** the spider is removed and no error is raised when the scheduled end would have run

#### Scenario: Leaving the target instance before the end

- **WHEN** the player leaves the target instance 50 ticks after the camera was switched to the spider
- **THEN** the spider is removed and the camera is no longer on it

### Requirement: Transition survives the waiting phase end

The end of the waiting phase SHALL NOT end a running transition. The camera SHALL stay on the spider for the full
100 ticks after the switch even though the game phase has already started.

#### Scenario: Waiting phase ends during the transition

- **WHEN** the waiting phase ends 50 ticks after the camera was switched to the spider
- **THEN** the camera still targets the spider, the spider still exists, and the transition is still running

#### Scenario: Transition still ends at 100 ticks after the switch once the waiting phase ended

- **WHEN** the waiting phase ends 50 ticks after the camera was switched and 100 ticks have passed since the switch
- **THEN** the camera returns to the player and the spider is removed, exactly as without the phase end

### Requirement: Cleanup when the round finishes

The transition SHALL end early without error when the round finishes, that is when a `GameFinishEvent` is dispatched.
Every running transition SHALL be cancelled: the spider is removed, the camera returns to the player if it was on
the spider, and the scheduled checks are cancelled.

#### Scenario: Game finish after the camera switch

- **WHEN** a `GameFinishEvent` is dispatched 50 ticks after the camera was switched to the spider
- **THEN** the spider is removed, the camera returns to the player, and no error is raised when the scheduled end
  would have run

#### Scenario: Game finish is wired through the register method

- **WHEN** the transition is registered on an event node and a `GameFinishEvent` is dispatched on that node
- **THEN** the transition is no longer running for the player

### Requirement: Lobby close keeps the camera until the player leaves the lobby

When the waiting phase starts, the system SHALL start a close transition for every online player with the lobby as
target, before the instance switch. The close SHALL point the camera at a spider in the lobby, SHALL have no duration,
and SHALL end only when the player leaves the lobby. Leaving the lobby SHALL remove the lobby spider and return the camera
to the player without error. The lobby SHALL be read before the active instance switches to the game.

#### Scenario: Close starts with the waiting phase

- **WHEN** the waiting phase starts with a player in the lobby
- **THEN** a close transition is running for the player and the spider exists in the lobby

#### Scenario: Close does not end by itself

- **WHEN** the close camera is on the lobby spider and 150 ticks pass while the player stays in the lobby
- **THEN** the camera still targets the lobby spider and the close is still running

#### Scenario: Leaving the lobby ends the close

- **WHEN** the player leaves the lobby while the close camera is on the lobby spider
- **THEN** the lobby spider is removed, the camera targets the player's own entity id, and the close is no longer running

#### Scenario: Starting the open does not end a running close

- **WHEN** an open transition starts for the player while the close is still running in the lobby
- **THEN** the close is still running and the camera still targets the lobby spider

### Requirement: Open starts at the game tick

The waiting phase's tick 1 SHALL start an open transition for every online player with the game instance as target,
after the team teleport. The open SHALL behave as the open transition in the requirements above.

#### Scenario: Tick 1 opens the eyes in the game instance

- **WHEN** the waiting phase reaches tick 1 with a close running for a player who is still in the lobby
- **THEN** an open transition is running for the player with the game instance as target, and the close is still running

### Requirement: World age is moved into the band of the kind before the first switch

Right before the first camera switch of a batch in an instance, the system SHALL set the world age of that instance to the
value `worldAgeFor(kind, current)`. The batch SHALL set the world age only once per instance: other players of the same
batch SHALL NOT move it again. For the close the value SHALL be the next multiple of 24000 not below the current age. For
the open the value SHALL be the next age not below the current age whose value modulo 24000 is 12000. The world age
SHALL NOT move backwards. The new world age SHALL be sent to the instance's players at once.

The resource pack reads the world age as follows:

| World age mod 24000 | Animation | Effect                                                              |
|---------------------|-----------|---------------------------------------------------------------------|
| `[0, 6000)`         | close     | `t = (age mod 24000) / 20 s`; fully shut by 1.6 s, then held shut   |
| `[12000, 18000)`    | open      | `t = (age mod 24000 - 12000) / 20 s`; neutral from 4.5 s            |
| any other value     | none      | no visible effect                                                   |

#### Scenario: Close sets the lobby into the close band

- **WHEN** the lobby world age is 100 at the close switch
- **THEN** the lobby world age is set to 24000, and the time packet with 24000 is sent to the players in the lobby

#### Scenario: Open sets the game instance into the open band

- **WHEN** the game world age is 100 at the open switch
- **THEN** the game world age is set to 12000, and the time packet with 12000 is sent to the players in the game instance

#### Scenario: Two players of one batch set the world age once

- **WHEN** two players are started by one close call and both switch the camera in the lobby
- **THEN** the lobby world age is set exactly once

#### Scenario: Keeping an age that is already in the band

- **WHEN** the world age is already 24000 at the close switch, or 12000 at the open switch
- **THEN** the world age is set to the same value and does not move

### Requirement: Transition logging

The system SHALL log each step of a transition at INFO level with the kind and the player's name, so that a live test
can be followed in the server log, for example `wake-up transition [open]: spider spawned for <name>`. The steps are:
`wake-up transition [<kind>]: world age of <instance> set to <n>` when the world age is set, and the lines below with
`[<kind>]`: `wake-up transition: spider spawned for <name>` when the spider is created,
`wake-up transition: camera switched to spider for <name>` when the camera switches,
`wake-up transition: camera returned for <name> (<reason>)` when a switched camera returns, and
`wake-up transition: abandoned for <name> (<reason>)` when a transition ends without having switched the camera.
A transition that starts before its player is in the target instance logs
`wake-up transition: waiting for <name> to arrive in the game instance`, and the arrival logs
`wake-up transition: <name> arrived, spawning spider` before the spawn line.

#### Scenario: Spawn, switch and return are logged

- **WHEN** a transition runs from start to the 100 tick end for a player named Alice who arrives in the game instance
- **THEN** the log contains, in order, `wake-up transition [open]: waiting for Alice to arrive in the game instance`,
  `wake-up transition [open]: Alice arrived, spawning spider`, `wake-up transition [open]: spider spawned for Alice`,
  `wake-up transition [open]: world age of <instance> set to 12000`,
  `wake-up transition [open]: camera switched to spider for Alice`, then `wake-up transition [open]: camera returned for Alice (100 ticks elapsed)`

#### Scenario: Lobby close is logged with the teleport as its end

- **WHEN** a close for a player named Alice switches the camera in the lobby and Alice then leaves the lobby
- **THEN** the log contains `wake-up transition [close]: camera switched to spider for Alice`, then
  `wake-up transition [close]: camera returned for Alice (teleported)`

#### Scenario: Abandoned transition is logged with the reason

- **WHEN** a pending transition for a player named Bob never arrives and is abandoned after the timeout
- **THEN** the log contains `wake-up transition: abandoned for Bob (player did not arrive in the game instance after 100 ticks)`
