## Why

When the round starts, survivors are teleported from the lobby into the game map and immediately see the
world at full clarity. The resource pack overrides the vanilla `minecraft:post_effect/spider.json` post effect
with a "wake-up eyes" shader: the eyelids blink open over about four seconds with blurred, double vision that
fades out. Vanilla clients only apply that post effect while their camera spectates a spider, so the server has
to point each player's camera at a spider that matches their eye position for the effect to play.

Creeper cannot carry the effect because the pack uses that camera for a glitch effect, and Enderman cannot
carry it because Slender himself is an Enderman.

## Commit type and scope

Conventional Commits type and scope: `feat(game)`. The change adds one player-facing behaviour to the game
phases and ships as a single commit on the implementation branch.

## What Changes

- Add a `WakeUpTransition` service that, for each player who enters the game map, spawns an invisible, silent,
  gravity-free spider visible only to that player, places it at the player's eye position with the player's
  view direction, and points the player's camera at it through `Player#spectate` once the client can know the
  spider (see design.md, "Client ordering").
- The camera switch waits until the player is in the spider's instance, the spider is spawned and is a viewer of
  the player, and then `CAMERA_DELAY_TICKS` (2) more ticks so the spider's spawn packets are sent first. If that
  does not happen within `PENDING_TIMEOUT_TICKS` (40) ticks, the transition is abandoned and the spider removed.
- The camera returns to the player and the spider is removed exactly 100 server ticks after the camera switch
  (about 5 seconds), counted with a tick based repeating task on the Minestom scheduler.
- The transition is cancelled early, with the spider removed and no error, when the player disconnects, leaves
  the instance they were teleported into, or the round finishes (`GameFinishEvent`), whether the camera is still
  pending or already on the spider. The end of the waiting phase does not cancel it: the camera stays on the spider
  for the full 100 ticks.
- The transition logs its steps at INFO level: spider spawned, camera switched, camera returned, abandoned.
- Hook the service into `WaitingPhase` directly after the team teleport.
- No game mode change: the camera packet is sent without touching the player's game mode.

## Capabilities

### New Capabilities

- `game-camera-transition`: the per-player wake-up camera transition that runs when the round starts.

### Modified Capabilities

None.

## Impact

- `game/src/main/java/net/onelitefeather/cygnus/camera/` (new package with the service and its spider entity).
- `game/src/main/java/net/onelitefeather/cygnus/phase/WaitingPhase.java` (start after the teleport; no cancel on phase end).
- `game/src/main/java/net/onelitefeather/cygnus/Cygnus.java` (creates the service, passes it to the phase and
  registers its disconnect, instance-change and game-finish listeners).
- `game/src/test/java/net/onelitefeather/cygnus/camera/` (tests written before the production code).
- Requires the resource pack to override `minecraft:post_effect/spider.json`; without the pack the camera change
  is visible as a plain spectator camera on the spider and nothing else.
