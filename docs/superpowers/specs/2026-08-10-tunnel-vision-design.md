# Tunnel vision for survivors

## Goal

A survivor's view narrows as the situation gets worse: the screen edges darken and pulse like a
heartbeat when stamina runs low, when the Slender closes in, or both. The effect is per player,
continuous rather than on/off, and driven entirely by the server.

## Why not a shader

The obvious implementation is a post-processing shader, and on Minecraft 26.2 it does not work.

A resource-pack post effect only runs in contexts vanilla decides: the menu blur, spectator mob
vision, the glowing outline, and the "Improved Transparency" video setting. None of them can be
switched on for one player from the server, and none carries an intensity parameter. The only way
to force one on 26.2 is hijacking spectator mob vision by pointing the player's camera at a hidden
enderman, which takes over the camera and makes the game unplayable.

That changes in 26.3: snapshot 3 (7 July 2026) added `/posteffect add|remove <player> <effect>`
plus the always-on `minecraft:end_of_frame` context. 26.3 is still in snapshots, and Minestom
ships 26.2 (`net.minestom:minestom:2026.07.22-26.2`).

So the effect is rendered as the `camera_overlay` of an item worn on the head — the mechanism
behind the carved pumpkin, and the one thing in vanilla that draws a texture across the whole
screen and scales it with the viewport. Behind an interface that a post-effect renderer can slot
into once 26.3 and Minestom support land; the gameplay side does not change when that happens.

Reference: [Shader – Minecraft Wiki](https://minecraft.wiki/w/Shader),
[Java Edition 26.3 Snapshot 3](https://minecraft.wiki/w/Java_Edition_26.3_Snapshot_3).

## Intensity

`TunnelVisionIntensity` turns the survivor's stamina into a value in `[0, 1]`. It has no Minestom
dependency at all, so it is testable without a server.

**Stamina.** With `s = currentSpeedCount / 20`:

```
stamina = s >= 0.5 ? 0 : ((0.5 - s) / 0.5)^2
```

Nothing happens above half a bar; below it the curve accelerates, so the last few percent are far
more dramatic than crossing the halfway mark.

**Why the Slender is not an input.** An earlier draft folded a proximity term into this value, so
that the view also narrowed as he closed in. That half of the idea became its own effect: the
slender gaze glitch tears the screen when he is in view, driven by `SlenderGaze` with its own range
and field-of-view constants, and both draw onto the same `ScreenOverlay` as separate layers. Keeping
them apart means each can be tuned - and switched off - without touching the other, and a survivor
who is merely exhausted does not get the effect meant for one who is being hunted.

**No line-of-sight raycast.** Neither effect dampens on a wall between survivor and Slender. It
would cost a block walk per survivor per tick, and "I can feel him through the wall" is the better
atmosphere anyway.

## Stages and pulse

The continuous value is quantised to 32 stages, which double as the frames of the heartbeat.
Minecraft cannot animate an overlay texture — `.mcmeta` animation covers block, item, particle,
painting and effect textures only — so the animation is the server walking through the frames.
Thirty-two of them make the view close smoothly; at sixteen the steps were visible as the tunnel
narrowed. Two mechanisms sit on top, in this order:

1. **Hysteresis on the base value.** `baseStage` starts as `round(intensity * 32)` and afterwards
   only moves when `intensity * 32` is more than 0.6 stages away from it. Stamina jitters constantly
   as a survivor starts and stops sprinting; without this the overlay flickers at every stage
   boundary.
2. **Pulse on top of the stabilised stage.**

```
depth     = (32 / 16) * intensity         // a sixteenth of the scale, whatever the stage count is
frequency = 1.0 + 1.5 * intensity         // Hz
display   = clamp(round(baseStage + depth * (sin(2*pi * frequency * t) - 1)), 0, 32)
```

The heartbeat gets faster and deeper as it gets tighter, and stays nearly invisible at low
intensity — a depth that does not scale would make stage 1 flicker between 0 and 1.

The pulse only ever opens the view back up, never past the base stage. A symmetric pulse would be
clipped away exactly where it matters most: at full intensity the base stage is already the
maximum, so everything above it is lost and the heartbeat disappears.

The order matters: hysteresis applies to the base value, the pulse is added afterwards. Reversed,
the hysteresis would damp out exactly the pulsing it is there to allow.

Stage 0 is not a texture. It clears the overlay.

**Service tick: 100 ms.** The heartbeat reaches 2.5 Hz, and sampling it at 4 Hz — a 250 ms tick —
aliases it into something jerky. 100 ms samples it ten times per second, which is smooth and still
a tiny packet per survivor.

## Pack assets

In `cygnus-pack`, namespace `cygnus`:

```
pack/assets/cygnus/textures/gui/tunnel_vision/stage_1.png … stage_32.png
pack/assets/cygnus/equipment/empty.json
```

Each texture is 768×432 — 16:9, because the client stretches a camera overlay across the screen
rather than fitting it. The darkening closes in from all four edges rather than as a circle from
the middle: it is a superellipse whose exponent eases from 4 at stage 1, a rounded rectangle
framing the screen, to 2 at stage 32, where a plain ellipse reads as a tunnel. Textures are
generated by `tools/generate_overlay.py`, which also produces the blood splatter.

**How it reaches the screen.** The server puts an item in the player's head slot carrying
`equippable{slot:head, camera_overlay:"cygnus:gui/tunnel_vision/stage_N"}`. Three details keep the
carrier out of the way:

- `asset_id` points at `cygnus:empty`, an equipment model with no layers. Without it Minecraft
  draws the item itself on the player's head.
- `swappable`, `dispensable` and `damage_on_hurt` are all off, so nobody strips the overlay by
  accident and it is not treated as armour.
- The equip sound is `minecraft:intentionally_empty`; the default would click on every stage
  change, ten times a second.

**The position needs no calibration.** This is the whole reason for the mechanism: the client
scales the overlay to the viewport, so it fits every resolution and GUI scale on its own. A font
glyph cannot — its size is fixed in the pack, so it has to be calibrated against one resolution
and drifts on every other.

## Components

New package `net.onelitefeather.cygnus.tunnelvision`:

- `TunnelVisionIntensity` — the calculation above. Pure, no server needed to test it.
- `TunnelVisionStage` — one survivor's overlay state: hysteresis and heartbeat. Also pure.
- `TunnelVisionRenderer` — `render(player, stage)` and `clear(player)`. This is the seam a
  post-effect renderer slots into on 26.3.
- `OverlayTunnelVisionRenderer` — the implementation described above; it contributes a texture to
  the shared `ScreenOverlay` rather than dressing the player itself.
- `TunnelVisionService` — holds a `TunnelVisionStage` per survivor and ticks all of them in one
  scheduler task.
- `TunnelVisionCommand` — `/tunnelvision stage <0-32> | intensity <0.0-1.0> | off`, for judging the
  vignette from the lobby without a running round. `stage` freezes one stage to judge the drawing;
  `intensity` runs the real heartbeat.

One task for everyone rather than one per player as `StaminaBar` does: a single 100 ms task walks
every tracked survivor, so the count of scheduler tasks does not grow with the lobby, and cleanup
happens in one place.

## Wiring

`Cygnus` creates the service and the command. The service then listens for the round's lifecycle
itself, the way `SpectatorService` and `ResourcePackService` already do, rather than being called
from the existing listeners:

| Event | What happens |
| --- | --- |
| `GameStartEvent` | starts drawing for the survivor team |
| `PlayerDeathEvent` | removes the player (transition to spectator) |
| `PlayerDisconnectEvent` | removes the player |
| `GameFinishEvent` | full cleanup |

This keeps `GameStartListener`, `PlayerDeathListener` and `PlayerQuitListener` — and their tests —
untouched: none of them has anything the service needs beyond the moment itself.

Two changes to existing code:

- **`FoodBar` gains a getter** for normalised stamina. `currentSpeedCount` is private today. The
  service could read `player.getExp()`, since `FoodBar` mirrors the value there, but that hangs
  game logic off a display detail. `StaminaHelper.remainingShare` wraps the lookup, so the service
  takes a plain `ToDoubleFunction<Player>` and `Cygnus` keeps none of it.
- **The effects are gated by `OverlayProperties`, not by the resource pack.** An earlier draft tied
  them to `resourcePackService` being present, on the grounds that the textures would otherwise be
  missing. That conflated two questions: whether this server hands out a pack, and whether a player
  has one loaded - a player can arrive with the pack already installed, and a server can hand one
  out that a player declines. `cygnus.overlays` answers the second directly, and
  `Cygnus.registerOverlayListeners` holds that one gate for all three effects.

## Failure modes

The service keeps running in all of these; none of them throws.

| Situation | Behaviour |
| --- | --- |
| No `FoodBar` registered for a player | reads as a full bar, so the effect stays off |
| Ticked outside a round (before `GameStartEvent`, after `GameFinishEvent`) | nobody is tracked; the task does nothing |
| Stage drops to 0 | the layer is dropped rather than drawn — otherwise the last vignette stays on the head |
| Player dies or becomes a spectator | explicit `clear()`, same reason |
| The pack is not loaded on a client | the overlay resolves to a missing texture for that player only; the server side is unaffected |

## Tests

- `TunnelVisionIntensityTest` — plain JUnit: the threshold at half a bar, an empty bar giving 1,
  and monotonicity as the bar drains.
- `TunnelVisionStageTest` — plain JUnit: the pulse at full intensity, steadiness at low intensity,
  hysteresis (a small oscillation around a stage boundary must not change the stage), and bounds.
- `OverlayTunnelVisionRendererTest` — Cyano: the renderer contributes the expected texture, and
  `clear()` drops only its own layer rather than wiping the screen out from under the blood
  splatter.
- `EquipmentScreenOverlayTest` — Cyano: a layer becomes a camera overlay on the head, the blood
  wins over the tunnel vision and the tunnel vision returns afterwards, the last layer leaving
  empties the slot, and an unchanged overlay is not re-sent.
- `TunnelVisionServiceTest` — lifecycle: start and stop, removing a player, and the four lifecycle
  events it registers for.
- `TunnelVisionCommandTest` — the command draws the requested stage, previews an intensity, and
  clears on `off`.
- `FoodBarTest` — a fresh bar reports a full share.
- `StaminaShareTest` — `StaminaHelper.remainingShare` reads a full bar for a player who has none,
  which is what keeps the effect off for anyone not playing a round.

The pack side cannot be tested automatically. The look of the vignette is verified in-game against a
snapshot build of `cygnus-pack`; that is an explicit step in the implementation plan, not an
afterthought. What *can* be checked mechanically is that both sides agree on the texture paths:
`OverlayTextureKeys` builds them and `tools/README.md` in `cygnus-pack` documents them, and a
mismatch shows up as a fullscreen missing-texture checkerboard with nothing in any log.
