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

So the effect is rendered as a HUD overlay through the action bar today, behind an interface that
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

The continuous value is quantised to 8 stages. Two mechanisms sit on top, in this order:

1. **Hysteresis on the base value.** `baseStage` starts as `round(combined * 8)` and afterwards
   only moves when `combined * 8` is more than 0.6 stages away from it. Distance and stamina both
   jitter constantly; without this the overlay flickers at every stage boundary.
2. **Pulse on top of the stabilised stage.**

```
amplitude = 0.5 * combined
frequency = 1.0 + 1.5 * combined          // Hz
display   = clamp(round(baseStage + amplitude * sin(2*pi * frequency * t)), 0, 8)
```

The heartbeat gets faster and deeper as it gets tighter, and stays nearly invisible at low
intensity — an amplitude that does not scale would make stage 1 flicker between 0 and 1.

The order matters: hysteresis applies to the base value, the pulse is added afterwards. Reversed,
the hysteresis would damp out exactly the pulsing it is there to allow.

Stage 0 is not a texture. It clears the overlay.

Service tick: 250 ms.

## Pack assets

In `cygnus-pack`, namespace `cygnus`:

```
pack/assets/cygnus/textures/gui/tunnel_vision/stage_1.png … stage_8.png
pack/assets/cygnus/font/tunnel_vision.json
```

Each texture is a soft radial darkening, 512×512, fully opaque at the outer edge. The font is a
bitmap provider mapping `U+E000`–`U+E007` to stages 1–8.

The server builds a `Component` carrying `font("cygnus:tunnel_vision")` and sends it with
`sendActionBar`. Two details that otherwise look broken:

- `shadowColor` must be transparent, or Minecraft renders the vignette a second time, offset,
  underneath itself.
- The action bar fades after 3 seconds. The 250 ms tick refreshes it long before that.

**Positioning is approximate by construction.** Font glyphs render relative to the action bar, and
the server knows neither the client's resolution nor its GUI scale, so pixel-accurate centring is
impossible. The texture is deliberately larger than any realistic viewport and fully opaque at the
edge: the overhang is clipped, and because the vignette is soft, the offset does not read as an
error. `height` and `ascent` in the font provider are calibration values — they start at `height:
512`, `ascent: 200` and get adjusted in-game against a snapshot build of the pack.

This is the cost of the action-bar approach against a real post effect, which would be
full-screen by nature.

## Components

New package `net.onelitefeather.cygnus.tunnelvision`:

- `TunnelVisionIntensity` — the calculation above. Pure, no server needed to test it.
- `TunnelVisionRenderer` — `render(player, stage)` and `clear(player)`. This is the seam a
  post-effect renderer slots into on 26.3.
- `ActionBarTunnelVisionRenderer` — the implementation described above.
- `TunnelVisionService` — holds per-survivor state (current stage for hysteresis, pulse phase) and
  ticks all survivors in one scheduler task.

One task for everyone rather than one per player as `StaminaBar` does: the Slender position is
read once per tick instead of once per survivor, and cleanup happens in one place.

## Wiring

Along the paths `StaminaService` already uses:

| Point | What happens |
| --- | --- |
| `Cygnus` | creates the service |
| `GameStartListener` | starts it for the survivor set |
| `PlayerDeathListener` | removes the player (transition to spectator) |
| `PlayerQuitListener` | removes the player |
| wherever `staminaService.cleanUp()` runs | full cleanup |

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
| Stage drops to 0 | `clear()` rather than rendering — otherwise the last vignette lingers for three seconds until the action bar fades on its own |
| Player dies or becomes a spectator | explicit `clear()`, same reason |

## Tests

- `TunnelVisionIntensityTest` — plain JUnit: edge values (full stamina at long range gives 0,
  empty stamina at close range gives 1), monotonicity in both inputs, the view factor, and
  hysteresis — a small oscillation around a stage boundary must not change the stage.
- `ActionBarTunnelVisionRendererTest` — Cyano: the player receives an action bar packet with the
  expected code point and the `cygnus:tunnel_vision` font, the shadow is transparent, and
  `clear()` sends an empty component.
- `TunnelVisionServiceTest` — lifecycle: start and stop, removing a player, behaviour with no
  Slender.

The pack side cannot be tested automatically. Glyph sizing and the look of the vignette are
verified in-game against a snapshot build of `cygnus-pack`; that is an explicit step in the
implementation plan, not an afterthought.
