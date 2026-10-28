## 1. Configuration (tests first)

- [ ] 1.1 `GameConfigReaderTest`: `maxSpectators` defaults to 8 when absent, reads a configured value, accepts 0 and
  rejects a negative value with the same error as the other round values
- [ ] 1.2 `GameConfigTest`: `GameConfig.Round` with the fifth component keeps the other values and rejects a negative
  `maxSpectators`
- [ ] 1.3 Implement `maxSpectators` in `GameConfig.Round` (default 8, `atLeast` check), read it in `GameConfigReader`
  next to `maxPlayers`, document the key in its properties list, and pass the default at the four-argument call sites
- [ ] 1.4 `./gradlew :common:test` green

## 2. Late-join bookkeeping (tests first)

- [ ] 2.1 Test class for the late-join set with a fresh instance per test: reserve a free place, refuse a full set,
  release on disconnect, release of a refused joiner, clear on restart, and a dead survivor never being in the set
- [ ] 2.2 Test that two reservations competing for the last place admit exactly one, from two threads, with a latch
  and no sleep
- [ ] 2.3 Implement the late-join set next to `SpectatorService`, with one synchronised check-and-reserve method and
  a release method
- [ ] 2.4 `./gradlew :game:test` green

## 3. Login (tests first)

- [ ] 3.1 `PlayerLoginListenerTest`: a player joining in the `GamePhase`, `WaitingPhase` and `RestartPhase` is admitted
  when a late place is free, with the instance set and the resource pack sent
- [ ] 3.2 `PlayerLoginListenerTest`: a full late place is refused with the spectator places full message through
  `kickInPlay`, and `maxSpectators = 0` is refused with `GAME_ALREADY_STARTED`
- [ ] 3.3 `PlayerLoginListenerTest`: a running round above `maxPlayers` admits a late joiner, and a lobby join above
  `maxPlayers` is still refused with `SERVER_FULL`
- [ ] 3.4 `PlayerLoginRejectionTest`: a refused late joiner holds no place after the disconnect
- [ ] 3.5 Change `PlayerLoginListener`: the `SERVER_FULL` check applies only in the lobby, the phase check is replaced
  by the late-place check, and the reservation runs in the same step as the check
- [ ] 3.6 `./gradlew :game:test` green

## 4. Spawn (tests first)

- [ ] 4.1 `PlayerSpawnListenerTest`: a late joiner's first spawn places them at the spawn of the running map and
  converts them with `SpectatorService.join`, which sets the spectator team tag
- [ ] 4.2 `PlayerSpawnListenerTest`: a lobby joiner's spawn is unchanged, including the lobby hook
- [ ] 4.3 Change `PlayerSpawnListener`: for a late joiner outside the lobby, convert to a spectator at the spawn and send
  the joiner the watching message
- [ ] 4.4 `./gradlew :game:test` green

## 5. Quit and round rules (tests first)

- [ ] 5.1 `PlayerQuitListenerTest`: a late spectator quitting during the `GamePhase` does not finish the round and does
  not start a takeover, and releases its late place
- [ ] 5.2 `PlayerQuitListenerTest`: a refused joiner disconnecting releases nothing and runs no round logic
- [ ] 5.3 Test that a late spectator is absent from `statsParticipants()` when the round ends, and that no pity or pass
  call is made for them
- [ ] 5.4 Change `PlayerQuitListener` to release the late place, and wire the late-join set in `Cygnus.java`
- [ ] 5.5 `./gradlew :game:test` green

## 6. Visibility and messages (tests first)

- [ ] 6.1 Test that a late joiner's admission sends no join message to survivors or the slender, that the joiner receives
  the watching message, and that other spectators may receive the join message
- [ ] 6.2 Test that a late spectator is invisible to a survivor and that a late spectator's chat reaches only
  spectators
- [ ] 6.3 Add the new message constants to `common/.../Messages.java` and send the messages from the listeners
- [ ] 6.4 `./gradlew :game:test` green

## 7. Wiring and documentation

- [ ] 7.1 `Cygnus.java`: pass `maxSpectators` and the late-join set to `PlayerLoginListener`, `PlayerSpawnListener` and
  the quit listener
- [ ] 7.2 `./gradlew build` green
- [ ] 7.3 Live test: connect a second client during a running round with `maxSpectators` 1 and 0, check the spectator
  mode, the refusal and that the living players see no join message (needs a running server; not done in this change)
- [ ] 7.4 `openspec validate add-late-join-spectator --strict`

## 8. Pull request

- [ ] 8.1 Open the pull request titled `feat(spectator): let late joiners watch the running round`
