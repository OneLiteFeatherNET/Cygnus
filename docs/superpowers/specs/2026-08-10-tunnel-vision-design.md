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

So the effect is rendered as a HUD overlay through the title today, behind an interface that
a post-effect renderer can slot into once 26.3 and Minestom support land. The gameplay side does
not change when that happens.

Reference: [Shader – Minecraft Wiki](https://minecraft.wiki/w/Shader),
[Java Edition 26.3 Snapshot 3](https://minecraft.wiki/w/Java_Edition_26.3_Snapshot_3).

## Intensity

`TunnelVisionIntensity` turns two inputs into a value in `[0, 1]`. It has no Minestom dependency
beyond positions, so it is testable without a server.

**Stamina.** With `s = currentSpeedCount / 20`:

```
stamina = s >= 0.5 ? 0 : ((0.5 - s) / 0.5)^2
```

Nothing happens above half a bar; below it the curve accelerates, so the last few percent are far
more dramatic than crossing the halfway mark.

**Slender.** With `d` the distance between survivor and Slender:

```
proximity = clamp((25 - d) / (25 - 6), 0, 1)
view      = 0.6 + 0.4 * max(0, dot(survivorLookDirection, directionToSlender))
slender   = proximity * view
```

The effect starts at 25 blocks and peaks at 6. Looking straight at him is worse than having him
behind you, but never by more than a factor of 1.67 — he is frightening either way.

**Combination:**

```
combined = 1 - (1 - stamina) * (1 - slender)
```

Both sources add up noticeably but saturate cleanly at 1.0 instead of clamping hard, so neither
one can hide the other.

**No line-of-sight raycast.** A wall between survivor and Slender does not dampen the effect. It
would cost a block walk per survivor per tick, and "I can feel him through the wall" is the better
atmosphere anyway.

## Stages and pulse

The continuous value is quantised to 16 stages, which double as the frames of the heartbeat.
Minecraft cannot animate a font texture — `.mcmeta` animation covers block, item, particle,
painting and effect textures only, and vanilla ships no font texture with one — so the animation
is the server walking through the frames. Two mechanisms sit on top, in this order:

1. **Hysteresis on the base value.** `baseStage` starts as `round(combined * 16)` and afterwards
   only moves when `combined * 16` is more than 0.6 stages away from it. Distance and stamina both
   jitter constantly; without this the overlay flickers at every stage boundary.
2. **Pulse on top of the stabilised stage.**

```
depth     = (16 / 16) * combined          // one stage per 16, i.e. a fixed share of the scale
frequency = 1.0 + 1.5 * combined          // Hz
display   = clamp(round(baseStage + depth * (sin(2*pi * frequency * t) - 1)), 0, 16)
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
pack/assets/cygnus/textures/gui/tunnel_vision/stage_1.png … stage_16.png
pack/assets/cygnus/font/tunnel_vision.json
```

Each texture is 256×128, 2:1 so it covers a widescreen viewport, and fully opaque at the outer
edge. The darkening closes in from all four edges rather than as a circle from the middle: it is a
superellipse whose exponent eases from 4 at stage 1 — a rounded rectangle framing the screen — to 2
at stage 16, where a plain ellipse reads as a tunnel rather than a frame. The font is a bitmap
provider mapping `U+E000`–`U+E00F` to stages 1–16.

**The 256 pixel limit is not cosmetic.** Font glyphs are stamped into 256×256 sheets at their
texture resolution, and a glyph that does not fit is dropped without a word in the log — the
client then draws the missing-glyph box. Anything larger simply does not work, however good it
looks in an image viewer. The glyph is still drawn several times that size; each texture carries a
`blur` mcmeta so that upscale stays smooth instead of banding into nearest-neighbour blocks.

The server builds a `Component` carrying `font("cygnus:tunnel_vision")` and sends it as the title.
Three details that otherwise look broken:

- `shadowColor` must be transparent, or Minecraft renders the vignette a second time, offset,
  underneath itself.
- The title times are sent once per player with `fadeIn` and `fadeOut` at zero. A fade would make
  the vignette pump on every update, and repeating the times ten times a second would double the
  packet count for nothing — they stay in effect for every following title.
- `stay` is two seconds: long enough that the overlay never blinks between updates, short enough
  that it disappears on its own if the server stops drawing.

**The title is the channel, not the action bar.** Titles render centred on the screen and at four
times scale, which is what makes the position independent of the client's resolution: centring
needs `ascent = height/2 - 3` regardless of window size, where the action bar's anchor at the
bottom edge forced a recalibration for every viewport. `height: 70` covers a 240-pixel-high GUI
viewport four times over at that scale, and the 2:1 texture covers the width.

## Components

New package `net.onelitefeather.cygnus.tunnelvision`:

- `TunnelVisionIntensity` — the calculation above. Pure, no server needed to test it.
- `TunnelVisionStage` — one survivor's overlay state: hysteresis and heartbeat. Also pure.
- `TunnelVisionRenderer` — `render(player, stage)` and `clear(player)`. This is the seam a
  post-effect renderer slots into on 26.3.
- `TitleTunnelVisionRenderer` — the implementation described above.
- `TunnelVisionService` — holds a `TunnelVisionStage` per survivor and ticks all of them in one
  scheduler task.
- `TunnelVisionCommand` — `/tunnelvision stage <0-16> | intensity <0.0-1.0> | off`, for judging the
  vignette from the lobby without a running round. `stage` freezes one stage to calibrate the font
  against; `intensity` runs the real heartbeat.

One task for everyone rather than one per player as `StaminaBar` does: the Slender position is
read once per tick instead of once per survivor, and cleanup happens in one place.

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
  game logic off a display detail.
- **The service only exists when the resource pack is active.** `Cygnus` creates it only if
  `resourcePackService` is present, reusing the `Optional` already in place. Without the pack the
  font does not exist and players would see an empty box instead of a vignette.

## Failure modes

The service keeps running in all of these; none of them throws.

| Situation | Behaviour |
| --- | --- |
| No Slender (disconnected, not yet assigned) | stamina share only |
| Slender in a different instance | slender share is 0 |
| No `FoodBar` registered for a player | stamina share is 0 |
| Stage drops to 0 | `clear()` rather than rendering — otherwise the last vignette lingers until the title's `stay` runs out |
| Player dies or becomes a spectator | explicit `clear()`, same reason |

## Tests

- `TunnelVisionIntensityTest` — plain JUnit: edge values (full stamina at long range gives 0,
  empty stamina at close range gives 1), monotonicity in both inputs, and the view factor.
- `TunnelVisionStageTest` — plain JUnit: the pulse at full intensity, steadiness at low intensity,
  hysteresis (a small oscillation around a stage boundary must not change the stage), and bounds.
- `TitleTunnelVisionRendererTest` — Cyano: the player receives a title packet with the expected
  code point and the `cygnus:tunnel_vision` font, the shadow is transparent, the times hold rather
  than fade and are sent only once, and `clear()` empties the title.
- `TunnelVisionServiceTest` — lifecycle: start and stop, removing a player, behaviour with no
  Slender or one in another instance, and the four lifecycle events.
- `TunnelVisionCommandTest` — the command draws the requested stage, previews an intensity, and
  clears on `off`.
- `FoodBarTest` — a fresh bar reports a full share.

The pack side cannot be tested automatically. Glyph sizing and the look of the vignette are
verified in-game against a snapshot build of `cygnus-pack`; that is an explicit step in the
implementation plan, not an afterthought.
