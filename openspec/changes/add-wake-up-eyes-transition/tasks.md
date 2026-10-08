## 1. Tests (written first)

- [x] 1.1 Add `WakeUpTransitionTest` covering start, visibility, the 100 tick end and the early cancels
- [x] 1.2 Run the new tests and confirm they fail for the missing production code

## 2. Camera transition

- [x] 2.1 Add the `camera` package with `WakeUpTransition` and the invisible spider entity
- [x] 2.2 Place the spider at the player's eye position and view direction, and restrict it to the player
- [x] 2.3 Start `Player#spectate`, schedule the 100 tick end, and stop spectating and remove the spider on expiry
- [x] 2.4 Cancel early on disconnect, instance change and phase end without errors

## 3. Phase wiring

- [x] 3.1 Pass the transition into `WaitingPhase`, start it after the team teleport, and cancel it on phase end
- [x] 3.2 Create the transition in `Cygnus` and register its listeners on the global event handler

## 4. Verification

- [x] 4.1 Run `./gradlew :game:test` and the repo build tasks, and fix any failure
- [ ] 4.2 Tick off the completed tasks in this file

## 5. Pull request

- [ ] 5.1 Open pull request titled `feat(game): add wake-up eyes camera transition`
