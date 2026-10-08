## 1. Tests (written first)

- [x] 1.1 Add `WakeUpTransitionTest` covering start, visibility, the 100 tick end and the early cancels
- [x] 1.2 Run the new tests and confirm they fail for the missing production code
- [x] 1.3 Replace the phase end test with "game finish removes the spider and cancels the scheduled end"
- [x] 1.4 Add a waiting phase test: finishing the waiting phase keeps the transition running (red before the fix)
- [x] 1.5 Run the new tests before the production change and confirm both fail
- [x] 1.6 Live test fix: rewrite the camera tests so the switch is awaited by ticks, add the pending, timeout and
  pending disconnect tests, and confirm that the new expectations fail on the previous implementation

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

## 3. Phase wiring

- [x] 3.1 Pass the transition into `WaitingPhase` and start it after the team teleport; the waiting phase end does not cancel it
- [x] 3.2 Create the transition in `Cygnus` and register its listeners on the global event handler

## 4. Verification

- [x] 4.1 Run `./gradlew :game:test` and the repo build tasks, and fix any failure
- [ ] 4.2 Tick off the completed tasks in this file

## 5. Pull request

- [ ] 5.1 Open pull request titled `feat(game): add wake-up eyes camera transition`
