## Context

`WaitingPhase` is a `TimedPhase` that ticks once per second. It starts with three ticks, teleports the teams into
the game instance at its tick 1 (about 2 s in), and finishes at its tick 0 (about 4 s in). The team teleport is
`TeamHelper.teleportTeams`, which calls `Entity#setInstance`. That method assigns the instance synchronously and
only the spawn is deferred to chunk loading, so the instance check right after the teleport is reliable.

Minestom's `Player#spectate(Entity)` only sends a `CameraPacket` with the entity id, and `stopSpectating()` sends
the same packet for the player itself. Neither changes the game mode, so no mode juggling is needed.

The repo already pins per-player visibility to `Entity#updateViewableRule(Predicate)`. `VisibilityRules` states
that `addViewer` / `removeViewer` must not be mixed into that rule, so the spider uses the same mechanism.

## Goals / Non-Goals

**Goals**

- One invisible spider per player, seen only by that player, at the player's eye position and view direction.
- Exactly 100 ticks until the camera returns, driven only by ticks (no wall clock).
- Early, quiet cleanup on disconnect, instance change and phase end.

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
- **Timing**: one delayed task, `TaskSchedule.tick(DURATION_TICKS)` with `DURATION_TICKS = 100`. The task handle is
  kept so an early cancel removes it.
- **Gamemode**: no change. Spectating only switches the camera.
- **Lifetime**: state lives per instance of `WakeUpTransition` in a map keyed by player UUID. There is no static
  state, so tests are independent.
- **Cleanup triggers**: `PlayerDisconnectEvent`, `RemoveEntityFromInstanceEvent` for the player when it leaves the
  spider's instance, and `WaitingPhase#onFinish`. All cancel through one method that is idempotent.
- **Event registration**: `register(EventNode<Event>)` takes the node. Production passes the global handler, tests
  pass the per-environment process handler so listeners do not leak between tests.

## Risks / Trade-offs

- **The waiting phase ends about 40 ticks after the teleport.** `WaitingPhase` finishes about 4 s after it starts
  and the teleport happens about 2 s in, so with the phase cleanup as specified the 100-tick transition is cut to
  roughly 40 ticks (about 2 s), less than the shader's four-second blink. This change follows the requirement as
  written. Moving the cancel to the start of the game phase (or to the end of the round) would keep the full 100
  ticks; that is a follow-up decision, not made here.
- A spider entity that is not spawned yet (chunk still loading) receives no camera packet until it is. The
  instance is set synchronously and the chunks around the spawn are already loaded by the lobby-to-map switch, so
  this is expected to be a non-issue in practice.
- The client may draw the spider's name or hitbox for a split second. Invisibility and the spectator camera hide
  both; this is not verified against a client here.
