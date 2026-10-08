## Why

The round starts with the survivors' eyes closing in the lobby and opening again in the game world. The resource
pack overrides the vanilla `minecraft:post_effect/spider.json` post effect with a "wake-up eyes" shader. The shader
picks its animation from the client's world age, so the server has to move each instance's world age into the band
of the animation it wants. Vanilla clients only apply the post effect while their camera spectates a spider, so the
server has to point each player's camera at a spider that matches their eye position for the effect to play.

- Close: in the lobby, when the waiting phase starts, the eyes close and stay shut until the player leaves the lobby.
- Open: in the game world, after the player arrived there, the eyes open over about four seconds.

Creeper cannot carry the effect because the pack uses that camera for a glitch effect, and Enderman cannot
carry it because Slender himself is an Enderman.

## Commit type and scope

Conventional Commits type and scope: `feat(game)` for the implementation, with the tests in the same commit. The
OpenSpec documents for the lobby close and the world-age contract are a separate `docs(openspec)` commit, so each
commit carries one type.

## What Changes

- Add a `WakeUpTransition` service with two kinds, `Kind.CLOSE` and `Kind.OPEN`. For each player it spawns an
  invisible, silent, gravity-free spider visible only to that player, places it at the player's eye position with the
  player's view direction, and points the player's camera at it through `Player#spectate` once the client can know the
  spider (see design.md, "Client ordering").
- `close(players, lobby)` starts when the waiting phase starts, before the instance switch. The camera stays on the
  lobby spider with no duration. The close ends when the player leaves the lobby (the teleport), which removes the
  lobby spider and logs `camera returned for <name> (teleported)`.
- `open(players, game)` starts at the waiting phase's tick 1, after the team teleport, as before. The camera returns
  exactly `DURATION_TICKS` (100) ticks after the switch, counted with a tick based repeating task on the Minestom
  scheduler.
- World age: right before the first camera switch of a batch (the players started by one call) in an instance, the
  instance's world age is moved into the band of the kind. Close: the next multiple of 24000 not below the current
  age. Open: the next age not below the current one that is 12000 modulo 24000. Each batch sets it once per instance,
  so later players of the batch do not restart the animation. Minestom sends the new world age to the instance's
  players when it is set.
- The camera switch waits until the player is in the spider's instance, the spider is spawned and is a viewer of the
  player, and then `CAMERA_DELAY_TICKS` (2) more ticks so the spider's spawn packets are sent first. If that does not
  happen within `PENDING_TIMEOUT_TICKS` (100) ticks, the transition is abandoned and the spider removed.
- Both kinds are cancelled early, with the spider removed and no error, when the player disconnects, when the player
  leaves the instance the transition belongs to after arriving there, or when the round finishes (`GameFinishEvent`).
  The end of the waiting phase does not cancel them. Starting an open does not end a running close of the same
  player.
- The transition logs its steps at INFO level with the kind: `wake-up transition [close|open]: ...`, including the
  world age set per instance.
- No game mode change: the camera packet is sent without touching the player's game mode.

## Capabilities

### New Capabilities

- `game-camera-transition`: the per-player wake-up camera transitions that run when the round starts: the lobby close
  and the game open, and the world-age bands the resource pack reads them from.

### Modified Capabilities

None.

## Impact

- `game/src/main/java/net/onelitefeather/cygnus/camera/` (new package with the service, the kinds and the spider entity).
- `game/src/main/java/net/onelitefeather/cygnus/phase/WaitingPhase.java` (close in `onStart` before the switch, open at
  tick 1 after the teleport; no cancel on phase end).
- `game/src/main/java/net/onelitefeather/cygnus/Cygnus.java` (creates the service, passes it to the phase and
  registers its disconnect, instance-change and game-finish listeners).
- `game/src/test/java/net/onelitefeather/cygnus/camera/` and `.../phase/WaitingPhaseIntegrationTest.java` (tests
  written before the production code).
- Requires the resource pack to override `minecraft:post_effect/spider.json` and to read the `GameTime` uniform from
  the world age. Without the pack the camera change is visible as a plain spectator camera on the spider and nothing
  else.
- Known gap: the close ends when the player leaves the lobby, and the open switch comes after the arrival in the
  game. Between the two the client has no spider camera, so the eyes are not closed during the teleport itself. This
  is the agreed end of the close; it still needs a live check.
