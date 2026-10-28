## Context

A round runs `LobbyPhase -> WaitingPhase -> GamePhase -> RestartPhase`. Today `PlayerLoginListener.accept` runs during
`AsyncPlayerConfigurationEvent` and checks two things: the online count against `maxPlayers` (`SERVER_FULL`), and
whether the current phase is a `LobbyPhase` (`GAME_ALREADY_STARTED`). A rejection goes through
`CygnusPlayer.kickInPlay`, so the kick is delivered in the play state and never during configuration. A rejected
player is marked before the join, and `PlayerQuitListener` skips the round logic for that player.

`PlayerSpawnListener` handles the first spawn. In the lobby it runs the lobby setup. Outside the lobby it only
teleports a player who has no team tag to the spawn. A player who died in the round already carries the spectator team
tag and is not moved.

`SpectatorService.join(Player)` already converts a player into a spectator: survival with flight, the spectator team
tag and team, the spectator layout, the struck-through tab name, the spectator visibility rule and the visibility
refresh. It is used when a survivor or the slender dies. `PlayerChatListener` isolates the chat of spectators from the
living players by team.

`statsParticipants()` in `Cygnus` builds the statistics participants from the survivor and slender teams at the end of
the round. A spectator is in neither team, so a late joiner is left out by construction. The specification keeps this
as an explicit requirement, so a later change does not break it silently.

`PlayerQuitListener` ends the round only for a survivor who leaves as the last survivor and for the slender (which
starts the takeover). A spectator leaving reaches neither branch.

## Goals / Non-Goals

**Goals**

- A player who connects during a round watches it instead of being turned away.
- The number of late watchers is bounded by its own setting, so they cannot crowd the server.
- Nothing about the running round changes: no broadcast, no statistics, no pity, no pass, no round end.
- The old behaviour stays available by setting the limit to zero.

**Non-Goals**

- Routing late joiners to a running CloudNet service. The proxy and the hub decide that, not Cygnus.
- Changes to the server list ping.
- Letting spectators join the next round before the restart. A restart already handles this.
- A permission for late joining. Everybody may join.

## Decisions

### The phase decides the role, the login decides admission

`PlayerLoginListener` no longer rejects a player only because the round is running. It admits the player when a late
place is free, or when the round is still in the lobby and the lobby limit allows it. In the `LobbyPhase` the player
becomes a normal player. In every other phase the player becomes a spectator at the first spawn, where the instance
and the running map are known. The configuration phase stays free of kicks, as `kickInPlay` requires.

### The lobby limit applies only in the lobby

The existing `SERVER_FULL` check counts every online player. During a round a late joiner must not be turned away
because the round had `maxPlayers` players when it started, so that check only applies in the lobby. A late joiner is
judged by `maxSpectators` alone.

### Late places are reserved at login, in memory, keyed by UUID

The late-join bookkeeping is a set of UUIDs next to `SpectatorService`. The check "is a late place free?" and the
insertion of the UUID happen in one synchronised step in `PlayerLoginListener`, so two logins cannot both take the
last place. A rejected joiner does not hold a place: the reservation is released in the disconnect handling, before
`PlayerQuitListener` returns early for players rejected before the join.

A player who died in the round is never in the set, so the dead never use a late place, even when the limit is
reached. The set is cleared when the round restarts. It is in memory on purpose: a restart ends the round, and the
limit is about the round.

The synchronisation accepts one tradeoff. A player who is admitted but disconnects during configuration holds its place
until the disconnect is processed. The place is freed at that point, so the effect lasts for at most one login.

### Late spectators reuse `SpectatorService.join`

The conversion is the existing one. A second implementation would drift from the rules for dead players (invisible to
the living, flight, chat isolation), and those rules are already covered by tests.

### A late spectator's quit has no round effect, by construction

A late spectator carries the spectator team tag and sits in neither the survivor team nor the slender team.
`PlayerQuitListener` therefore reaches neither the survivor branch nor the takeover. Its only extra work is to release
the late place. The specification states this as a requirement, and a test covers it.

### No join broadcast to the living

The join is announced only to spectators. Nothing reaches the survivors or the slender, because the number of joins
would tell them how many people are watching.

### The spectator limit is separate from `maxPlayers`

The two limits answer different questions. `maxPlayers` bounds the lobby, which decides who plays. `maxSpectators`
bounds the late watchers. Keeping them apart lets a server run a large lobby and a small gallery, or the reverse.

## Alternatives considered

- **Permission gate.** Only players with a permission could join late. Rejected: the maintainers decided that everybody
  may join. A permission would be a second rule to maintain for no gain, since the limit already bounds the watchers.
- **Counting late joiners against `maxPlayers`.** The existing check would turn joiners away with `SERVER_FULL` whenever
  the lobby count is reached during a round, and the player limit would double as the watcher limit. Rejected: the two
  limits answer different questions.
- **Only `GamePhase`.** Joins during `WaitingPhase` (teams are being prepared) and `RestartPhase` (the round is over,
  but the server still runs) would still be refused. Rejected: a player who connects in those windows wants to watch as
  much as anybody else, and the spectator rules apply the same way in every phase after the lobby.
- **Persisting the late-join set.** Rejected: a restart ends the round, and a persisted set would be wrong for the next
  round.

## Configuration

`GameConfig.Round` gets a fifth component, `maxSpectators`, read in `GameConfigReader` next to `maxPlayers` with the same
key style. The default is `8`. A negative value is rejected with the same check the other round values use (`atLeast`).
The properties list in `GameConfigReader` documents the new key.

Existing call sites that construct `GameConfig.Round` with four arguments pass the default for the new component.

## Messages

- An admitted late joiner gets one short message that says they are watching the round.
- A late joiner refused because the spectator places are full gets one message that says so. It is delivered through
  `kickInPlay`.

The wording is not fixed here. Both are new constants in `Messages.java`.

## Testing

Every test is written before the production code it covers and follows F.I.R.S.T.: it runs in Minestom's in-process
`Env`, drives the server with `env.tick()`, uses no sleeps, and injects a fixed `Clock` where time matters.

- `PlayerLoginListenerTest` (existing): the game-phase rejection test is replaced by admission tests for each post-lobby
  phase, a refusal when the spectator places are full, and a refusal when `maxSpectators` is zero.
- `PlayerSpawnListenerTest` (existing): a late joiner is placed at the spawn and converted to a spectator, and a lobby
  joiner is unchanged.
- A new test class for the late-join bookkeeping covers the reservation, two logins competing for the last place, the
  release on disconnect, the release of a rejected joiner, and the reset at the restart. One behaviour per test.
- `PlayerQuitListenerTest` (existing): a late spectator leaving does not end the round and does not start a takeover.
- `GameConfigReaderTest` and `GameConfigTest` (existing): the default, a configured value, zero, and a negative value.
- The statistics and pass tests are not extended. A test asserts that a late spectator is absent from
  `statsParticipants()`.

## Risks / Trade-offs

- **Burst of joins at the last place.** The check and the insertion must stay one step. The test for two competing
  logins guards this.
- **Watchers see the round.** Spectators are invisible to the living and their chat stays with the spectators, so
  inside the game they cannot reveal the Slender's position. Telling it over voice chat stays possible, as it already
  is for dead players. The spectator overview shows the survivors, as it already does for
  the dead.
- **Server load.** Up to eight more connections during a round. The limit bounds it, and operators can set it to zero.
- **Two similar limits.** `maxPlayers` and `maxSpectators` sit side by side in the round settings. The properties
  documentation says which applies where.

## Open questions

1. **Message wording**: the welcome for a late joiner and the full-places message are not written here.
2. **Overview for late joiners**: the overview lists the survivors. Whether it should also show how long the round has
   been running is not decided here.
