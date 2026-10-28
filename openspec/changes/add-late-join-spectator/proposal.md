## Why

A player who connects while a round is running is turned away with "the game has already started". Such a player
cannot play, and cannot even watch the round they came to see. The round is over for them before it has begun, and
the only way back is to reconnect after the server restarts.

Turning them into spectators fixes both. The player sees the round from the first moment, with the same spectator
rules as a player who died in the round, and nothing about the round changes for the players who are still playing.

## Commit type and scope

Conventional Commits type and scope: `feat(spectator)` for the implementation, with its tests in the same commit. The
OpenSpec documents are a separate `docs(openspec)` commit, so each commit carries one type.

## What Changes

- A player who joins while the round is running (`WaitingPhase`, `GamePhase` or `RestartPhase`) is no longer turned
  away. They become a spectator. In the lobby nothing changes: a joining player is a normal player.
- Everybody may join late. No permission is needed.
- A new round setting `maxSpectators` (default `8`, zero or more) limits how many late joiners may watch. It sits next
  to `maxPlayers` in the round settings. `0` turns late joining off, which is the current behaviour.
- Only late joiners count against `maxSpectators`. A player who died in the round and became a spectator had a player
  slot and does not count. `maxPlayers` keeps applying in the lobby only.
- When the spectator places are full, the joining player is refused with a new message that says the spectator
  places are full. The refusal follows the existing rule for turning players away: it is delivered in the play state,
  never during configuration.
- A late joiner is placed in the running map at its spawn and gets the spectator mode of `SpectatorService`: invisible
  to the living players, flying, the spectator hotbar and overview, a struck-through tab name, and the spectator chat
  isolation. They receive the resource pack like every other player.
- A late spectator is not a round participant. They get no statistics row, no pity progress, no Slender pass use, are
  never counted as a living survivor, and can neither end nor decide the round.
- A late spectator who leaves has no round effect. The round does not end and no Slender takeover starts.
- Nobody who is still playing is told about the join. The joiner gets a short message that they are watching the round.
  Other spectators may see that somebody joined.
- Out of scope: routing players to an INGAME CloudNet service (proxy and hub side), changes to the server list ping,
  and spectators switching into the next round beyond what already happens on a restart.

## Capabilities

### New Capabilities

- `late-join-spectator`: joining a running round as a spectator, the `maxSpectators` limit, and the rule that a late
  spectator is not a round participant.

### Modified Capabilities

None.

## Impact

- `common/src/main/java/net/onelitefeather/cygnus/common/config/GameConfig.java` (new `maxSpectators` in `Round`) and
  `GameConfigReader.java` (reads the key, documented in its list of properties).
- `common/.../Messages.java` (new spectator-places-full message, plus the message for the joiner).
- `game/src/main/java/net/onelitefeather/cygnus/listener/PlayerLoginListener.java` (admit a late joiner instead of
  rejecting it, check the spectator limit).
- `game/src/main/java/net/onelitefeather/cygnus/listener/PlayerSpawnListener.java` (place and convert a late joiner
  after the first spawn).
- `game/src/main/java/net/onelitefeather/cygnus/spectator/` (the late-join bookkeeping next to `SpectatorService`).
- `game/src/main/java/net/onelitefeather/cygnus/Cygnus.java` (wiring, and the late-join count is reset with the round).
- `game/src/main/java/net/onelitefeather/cygnus/listener/PlayerQuitListener.java` (releases the late place; the round
  logic is unchanged, since a spectator leaving already has no round effect, and tests keep it that way).
- Tests in `game/src/test/...` and `common/src/test/...`, written before the production code. The existing login
  rejection test for the game phase changes with this change.
- Permissions: none.
- Operations: no new service. The server list ping keeps showing `maxPlayers`, which is now the lobby limit only.
  Operators who want the old behaviour set `maxSpectators` to `0`.
