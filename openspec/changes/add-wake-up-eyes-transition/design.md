## Context

`WaitingPhase` is a `TimedPhase` that ticks once per second. It starts with three ticks, teleports the teams into
the game instance at its tick 1 (about 2 s in), and finishes at its tick 0 (about 4 s in). The team teleport is
`TeamHelper.teleportTeams`, which calls `Player#setInstance`. That switch is deferred: the player stays in the
lobby until the target chunks are loaded, so `player.getInstance()` is the lobby right after the call. The transition
therefore cannot read the game instance from the player at start (see "Arrival" below).

Minestom's `Player#spectate(Entity)` only sends a `CameraPacket` with the entity id, and `stopSpectating()` sends
the same packet for the player itself. Neither changes the game mode, so no mode juggling is needed.

The repo already pins per-player visibility to `Entity#updateViewableRule(Predicate)`. `VisibilityRules` states
that `addViewer` / `removeViewer` must not be mixed into that rule, so the spider uses the same mechanism.

## Client ordering (live test finding)

The first live test with two vanilla 26.2 clients showed no effect at all: the resource pack loaded, but the
client never applied the post effect, and no shader or post-chain error was logged. The camera had never
switched to the spider.

The cause is the order of packets on the client, not the shader:

- `Player#spectate(entity)` sends a `CameraPacket` with only the entity id. The client can only follow the camera
  to an entity it already knows, so the spider's spawn packets must reach the client first.
- The spider is set into the instance with `Entity#setInstance`, which is asynchronous. The spawn packets are
  sent only once the spider's chunk is loaded and the spider is registered, and the viewer set is updated on a
  later tick.
- The player's own switch into the game instance is still in flight at that point. A player instance switch
  sends a respawn, and a respawn resets the camera to the player.

The first version called `setInstance` and `spectate` in the same call, so the camera packet was sent before the
client knew the spider, and the packet was ignored or reset by the respawn.

The fix waits on the server until the client can know the spider, then waits a little longer. The readiness below
is checked after the arrival described above:

1. Readiness, checked on every server tick (`WakeUpTransition#tick`, `Transition#isViewable`):
   - `player.getInstance() == instance`: Minestom sets the player's instance only in the deferred spawn of
     `Player#setInstance`, so this is true once the player's switch has completed.
   - the spider's `setInstance` future is done, so the spider has been spawned in the instance;
   - `spider.getViewers().contains(player)`: the player has the spider in its viewer set, so the spawn packets are
     queued for it.
2. Delay: once the transition is ready, the camera switch happens `CAMERA_DELAY_TICKS = 2` ticks later, so the
   spawn packets are flushed before the `CameraPacket` (`pendingTicks - viewableSince >= CAMERA_DELAY_TICKS`).
3. Timeout: while not ready, the transition counts `pendingTicks`; at `PENDING_TIMEOUT_TICKS = 40` it is
   abandoned, the spider is removed and the camera is never switched. Nothing is sent to the client for it.
4. Duration: `DURATION_TICKS = 100` is counted from the switch (`ticksOnSpider`), not from the start.

No blocking join and no wall clock is used. The check is a repeating task with `TaskSchedule.tick(1)`, so the
tests drive it with `env.tick()`.

Limitation: `WaitingPhase` calls the team teleport as a `VoidConsumer`, so the future returned by the player's
`setInstance` is not visible to the transition. The arrival and readiness checks use the instance field instead,
which Minestom only updates when that switch has completed. The arrival is detected on the next server tick after
the switch, not in the event that completes it. If a future live test still shows a late respawn, the transition
could take the teleport future as a parameter.

## Arrival (second live test finding)

The second live test with two players logged both transitions as abandoned about 40 ms after they started:

```
22:37:15.119 wake-up transition: spider spawned for TheMeinerDev
22:37:15.157 wake-up transition: abandoned for TheMeinerDev (player left the instance)
```

The cause was the instance the spider was spawned in. `start(player)` took `player.getInstance()`, and the team
teleport had not been applied yet, so that was the lobby. About 40 ms later the player left the lobby, the
`RemoveEntityFromInstanceEvent` listener saw a player leaving the spider's instance, and the transition was
abandoned. The spider was also placed at the lobby position, which is wrong for the game map.

The fix separates the target from the player's current instance:

1. `WakeUpTransition#start(Player, Instance target)` and `start(Iterable<Player>, Instance target)` take the game
   instance explicitly. `WaitingPhase` obtains it from a `Supplier<Instance>` that `Cygnus` passes in as
   `mapProvider.getActiveInstance()`, which is the game instance after `switchToGameMap()`. The waiting phase
   calls `start` right after the team teleport, when the target is already the active instance.
2. The transition is created at once, but the spider is spawned only when the player is in the target instance
   (`player.getInstance() == target`). That check runs once at start and then on every tick. The spider takes the
   player's eye position and view at that moment, so the game spawn chosen by the teleport strategy is used.
3. The instance-leave cleanup only applies after the spider was spawned (`transition.arrived`) and the player leaves
   that instance. Leaving the lobby is ignored, so the lobby-to-game switch no longer cancels the transition.
   Disconnect and `GameFinishEvent` cancel in every state.
4. `PENDING_TIMEOUT_TICKS` is now 100 (5 s) and counts from the start of the transition. It covers the deferred
   switch with the chunk load and respawn of the game dimension. The 40 ticks (2 s) of the first version were too
   tight for that; 100 ticks is a margin chosen by reasoning, not a value measured in a live run. A transition that never arrives is abandoned after the timeout with no camera packet.

Logging adds the arrival steps: `wake-up transition: waiting for <name> to arrive in the game instance` at start when
the player is not in the target yet, and `wake-up transition: <name> arrived, spawning spider` when the spider is
spawned for the player.

## Goals / Non-Goals

**Goals**

- One invisible spider per player, seen only by that player, at the player's eye position and view direction.
- Exactly 100 ticks until the camera returns, driven only by ticks (no wall clock).
- Early, quiet cleanup on disconnect and instance change. Cleanup at round finish.
- The waiting phase end does not interrupt the transition: the camera stays on the spider for the full 100 ticks.

**Non-Goals**

- No change to the resource pack or to the post effect.
- No change to game mode, inventory or movement.
- No re-start of the transition for a respawn or a revive.

## Decisions

- **Spider entity**: a plain `Entity` of `EntityType.SPIDER`, not an `EntityCreature`. A plain entity has no goal
  selectors, so it never moves on its own and there is no AI to disable. Invisible, silent, `setNoGravity(true)`,
  custom name not visible (the default). `hasEntityCollision()` is overridden to `false` so players cannot push it.
- **Visibility**: `spider.updateViewableRule(viewer -> viewer.getUuid().equals(owner.getUuid()))`, the same
  pattern as `VisibilityRules`. No `addViewer` / `removeViewer` calls.
- **Placement**: `y = player.y + player.getEyeHeight() - spider.getEyeHeight()`, `setView(yaw, pitch, yaw)`, so the
  camera's eye lines up with the player's eye and the view direction is unchanged.
- **Timing**: one repeating task, `TaskSchedule.tick(1)`, that drives a small state machine per transition:
  waiting for arrival, pending (waiting for readiness, then the delay), on spider (counting `DURATION_TICKS = 100`
  from the switch), and ended. The task handle is kept so an early cancel removes it. Constants:
  `CAMERA_DELAY_TICKS = 2`, `PENDING_TIMEOUT_TICKS = 100`.
- **Target instance**: passed explicitly to `start`, never read from the player at start. The spider lives in that
  instance; the lobby is not used for it.
- **Logging**: INFO lines with the repo's SLF4J logger: `wake-up transition: waiting for <name> to arrive in the game
  instance`, `wake-up transition: <name> arrived, spawning spider`, `wake-up transition: spider spawned for <name>`,
  `camera switched to spider for <name>`, `camera returned for <name> (<reason>)`, and
  `abandoned for <name> (<reason>)`.
- **Gamemode**: no change. Spectating only switches the camera.
- **Lifetime**: state lives per instance of `WakeUpTransition` in a map keyed by player UUID. There is no static
  state, so tests are independent. A transition that is pending when a disconnect, an instance change or a game
  finish happens is removed without sending a camera packet, since the camera was never switched.
- **Cleanup triggers**: `PlayerDisconnectEvent`, `RemoveEntityFromInstanceEvent` for the player when it leaves the
  target instance after the arrival, and `GameFinishEvent` (cancels every running transition). All cancel through one method that
  is idempotent.
- **Round finish hook**: `GameFinishEvent` is the hook, not `RestartPhase`. The event is dispatched when the game
  phase reaches a finish condition, before the restart countdown. Other game services (`CreekService`,
  `AdrenalineService`, `SlenderGazeService`) already clean up on the same event, so the wiring matches the codebase.
  The listener is registered inside `register(EventNode<Event>)`, so `Cygnus` only calls `register` once.
- **Waiting phase end**: `WaitingPhase#onFinish` does not touch the transition. It only unfreezes the players and
  hands them to the game view. The transition is started at the waiting phase's tick 1 and ends by its own tick
  count, so the full 100 ticks run into the game phase.
- **Event registration**: `register(EventNode<Event>)` takes the node. Production passes the global handler, tests
  pass the per-environment process handler so listeners do not leak between tests.

## Risks / Trade-offs

- **The camera can stay on the spider while the game has started.** The transition runs for 5 s from the camera switch,
  a few ticks after the teleport, and the game phase starts about 4 s after the teleport. The first seconds of the round therefore show the wake-up effect
  together with the game phase. This is the agreed product behaviour; the cut happens only at round finish or on
  disconnect and instance change.
- A transition waits up to `PENDING_TIMEOUT_TICKS` (100 ticks, from its start) for the player to arrive and for the
  client to know the spider. If the lobby-to-map switch is slower than that, the transition is abandoned with no
  visible effect. The abandon line in the log names the reason: `player did not arrive in the game instance` or
  `spider not viewable`.
- The client may draw the spider's name or hitbox for a split second. Invisibility and the spectator camera hide
  both; this is not verified against a client here.
