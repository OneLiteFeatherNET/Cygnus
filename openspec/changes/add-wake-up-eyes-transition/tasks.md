## 1. Tests (written first)

- [x] 1.1 Add `WakeUpTransitionTest` covering start, visibility, the 100 tick end and the early cancels
- [x] 1.2 Run the new tests and confirm they fail for the missing production code
- [x] 1.3 Replace the phase end test with "game finish removes the spider and cancels the scheduled end"
- [x] 1.4 Add a waiting phase test: finishing the waiting phase keeps the transition running (red before the fix)
- [x] 1.5 Run the new tests before the production change and confirm both fail
- [x] 1.6 Live test fix: rewrite the camera tests so the switch is awaited by ticks, add the pending, timeout and
  pending disconnect tests, and confirm that the new expectations fail on the previous implementation
- [x] 1.7 Live test fix 2: add the arrival tests (lobby-to-game regression, leaving the lobby, disconnect while
  waiting for arrival, never arriving) against the `start(Player, Instance)` signature, and confirm the red step
  (test compilation fails on the missing signatures)

## 2. Camera transition

- [x] 2.1 Add the `camera` package with `WakeUpTransition` and the invisible spider entity
- [x] 2.2 Place the spider at the player's eye position and view direction, and restrict it to the player
- [x] 2.3 Start `Player#spectate`, schedule the 100 tick end, and stop spectating and remove the spider on expiry
- [x] 2.4 Cancel early on disconnect and instance change without errors
- [x] 2.5 Cancel every running transition on `GameFinishEvent`, registered in `WakeUpTransition#register`
- [x] 2.6 Live test fix: switch the camera only once the player is in the spider's instance, the spider's spawn is
  complete and the spider is a viewer of the player, then after `CAMERA_DELAY_TICKS` ticks
- [x] 2.7 Live test fix: check readiness every tick, abandon the transition after `PENDING_TIMEOUT_TICKS` ticks
- [x] 2.8 Live test fix: count the 100 ticks from the camera switch, not from the start
- [x] 2.9 Live test fix: log INFO lines for spawn, switch, return and abandon
- [x] 2.10 Live test fix 2: take the target game instance in `start`, and spawn the spider only once the player is in
  it, at the player's position on arrival
- [x] 2.11 Live test fix 2: cancel on instance change only after the arrival in the target; leaving the lobby is ignored
- [x] 2.12 Live test fix 2: count `PENDING_TIMEOUT_TICKS` (now 100) from the start, and log the wait and the arrival

## 3. Phase wiring

- [x] 3.1 Pass the transition into `WaitingPhase` and start it after the team teleport; the waiting phase end does not cancel it
- [x] 3.3 Live test fix 2: give `WaitingPhase` a `Supplier<Instance>` of the game instance (`mapProvider.getActiveInstance()`)
  and pass it to the transition as the target
- [x] 3.2 Create the transition in `Cygnus` and register its listeners on the global event handler

## 4. Verification

- [x] 4.1 Run `./gradlew :game:test` and the repo build tasks, and fix any failure
- [x] 4.2 Tick off the completed tasks in this file
- [x] 4.3 Run `openspec validate add-wake-up-eyes-transition --strict` and `./gradlew :game:shadowJar`

## 6. Lobby close and world age (third change)

- [x] 6.1 Red step: add the close, open, world-age, batch, sequence and WaitingPhase tests, and run them against a stub
  API that still behaves as the open transition (9 behavioural failures, the stub compiles)
- [x] 6.2 Add `Kind.CLOSE` / `Kind.OPEN`, `close` / `open` entry points, transitions keyed by player and kind, and the
  lobby exit as the normal end of the close (`camera returned for <name> (teleported)`)
- [x] 6.3 Set the world age once per batch and instance right before the first camera switch (`worldAgeFor`), and rely
  on Minestom's `setWorldAge` to send the time packet at once
- [x] 6.4 Start the close in `WaitingPhase#onStart` before the instance switch, and the open at tick 1 after the teleport
- [x] 6.5 Update proposal, design and spec with the two kinds and the world-age band table
- [x] 6.6 Live check with the resource pack: eyes shut in the lobby, open in the game, and the gap between the lobby exit
  and the open switch (not run here, no server was started)

## 5. Pull request

- [x] 5.1 Open pull request titled `feat(game): add wake-up eyes camera transition`
