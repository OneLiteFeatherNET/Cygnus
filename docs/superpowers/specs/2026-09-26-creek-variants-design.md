# Creek: Patrol Entity and Per-Player Variants

## Problem

The creek is a single server entity that switches between wandering, stalking,
hunting and vanishing. That design assumes one survivor alone in the woods and
does not scale with the player count:

- While wandering, the creek vanishes as soon as **any** survivor comes closer
  than `personalSpace` (default 15 blocks) and reappears elsewhere 20–40 s
  later. With several players spread over the map, someone is almost always
  near the route, so the creek spends most of the round vanishing and
  teleporting instead of walking.
- There is only one creek at one position. While it stalks or hunts one
  survivor, it is gone for everybody else.

A custom model rendered with WorldSeedEntityEngine (WSEE) will replace the
creaking later. WSEE renders each bone as its own server entity and animates
every model in its own per-tick task, so the number of simultaneously shown
creeks must stay small and predictable.

## Decision

Split the creek into two roles:

1. **Patrol entity** – one creek that everybody sees. It only walks the
   routes. It never stalks, hunts, vanishes on proximity or teleports itself.
   When a survivor comes close, it picks one, stares at them and stuns them or
   teleports them away, then walks on.
2. **Variants** – stalking and hunting move to per-player variants. A variant
   is a separate creek body that only its target sees. It runs the existing
   stalk → hunt sequence for that one survivor and is removed afterwards.

One shared patrol entity plus a small, capped number of variants keeps the
server cost bounded: at most 1 + 3 creek bodies (and later WSEE models) exist
at any time.

Rejected alternatives:

- **A central "director"** that replaces the states with a round-wide planner:
  more flexible for future events, but effectively a rewrite that throws away
  the tested stalk and hunt states.
- **One entity that "hands off"** by simulating a copy for the hunted player:
  contradicts keeping the patrol entity on the routes and cannot run several
  variants cleanly.
- **One variant per player, uncapped**: maximum horror, but the server cost
  (bone entities and animation tasks) grows linearly with the player count.

## Architecture

`CreekService` stays the entry point (round start/end, step task every
`TICK_MILLIS` = 100 ms) and owns both roles.

### Patrol entity

- The existing `Creek` class becomes the patrol entity. It keeps one
  `CreakingBody`, visible to all survivors.
- `WanderState` is reworked and renamed to `PatrolState`. It is the only state
  the patrol entity uses during a normal round.
- `VanishState.forever()` stays for the last-survivor rule (see below).
  `VanishState.after(...)` and the timed vanish are no longer used by the
  patrol entity.

### Variants

- New `CreekVariants` (manager) and `CreekVariant` (one variant) in
  `net.onelitefeather.cygnus.creek`.
- A `CreekVariant` has exactly one target (`UUID`), its own `CreekBody`
  (a `CreakingBody` whose viewers are only the target), and its own state:
  `StalkState` → `HuntState` → `DoneState`.
- The target never changes during a variant's lifetime.
- A survivor has at most one running variant.
- `CreekBody` stays the seam for the future WSEE model; nothing in this change
  depends on the creaking specifically.

## Patrol Behaviour (`PatrolState`)

Each step, in this order:

1. **Selection in a small radius.** If the selection is not on cooldown and at
   least one survivor is within `SELECT_RADIUS` (4 blocks) of the patrol
   entity, it selects exactly one survivor: the nearest. It then
   - stops and looks at the selected survivor for `STARE_MILLIS` (1 s),
   - applies the selection consequence to them (see below),
   - starts the selection cooldown `SELECT_COOLDOWN_MILLIS` (10 s), during
     which it selects nobody,
   - walks on along its route.

   Other survivors within the radius are ignored for this selection. If the
   selected survivor leaves the round during the stare, the selection is
   dropped without a consequence and without a cooldown.
2. **Seen from afar.** When a survivor sees it (existing sight check) and no
   selection is running, it stops and looks back for `wanderPauseMillis`
   (config, default 1.5 s), then walks on. Unchanged from today.
3. **Walking.** Otherwise it walks its route exactly as today: `PathRoute`
   steps, waypoint pauses (`pauseMillis`), random stops, and the stuck
   detection (`stuckMillis`) that picks the next waypoint.

Removed from today's `WanderState`:

- vanishing when a survivor is closer than `personalSpace`,
- starting a stalk (moves to `CreekVariants`),
- skipping route points that are near survivors (the route `allowed` filter no
  longer filters by survivor distance).

`personalSpace` stays in the config; the stalk spot search still uses it.

New constants in `PatrolState` (not configurable for now):

| Constant | Value |
|---|---|
| `SELECT_RADIUS` | 4.0 blocks |
| `STARE_MILLIS` | 1000 ms |
| `SELECT_COOLDOWN_MILLIS` | 10000 ms |

## Selection Consequence

New `SelectionConsequence` in `net.onelitefeather.cygnus.creek.consequence`.
It is separate from `CatchConsequence`: a selection is **not** a catch. It does
not touch the catch counter (`StagedCatchConsequence.CATCHES`) and never
reveals the survivor to the slender.

On apply, it picks one of two effects with equal chance:

- **Stun**
  - the selected survivor gets strong slowness for `STUN_MILLIS` (2 s),
    effectively frozen, and hears the creaking scare sound
    (`CatchEffects.SCARE_SOUND`),
  - every **other** survivor within `BLIND_RADIUS` (8 blocks) of the selected
    survivor gets blindness for `BLIND_MILLIS` (2 s). The selected survivor,
    the slender and non-survivors are not affected.
- **Teleport away**
  - the survivor is teleported to a random route point between
    `TELEPORT_MIN_DISTANCE` (20) and `TELEPORT_MAX_DISTANCE` (40) blocks from
    their position, settled on the floor via `Ground.settle`, and hears the
    scare sound,
  - if no route point in that range settles on a floor, the stun is applied
    instead.

`cleanUp()` removes any slowness/blindness it applied that is still running
when the round ends.

New constants (not configurable for now):

| Constant | Value |
|---|---|
| `STUN_MILLIS` | 2000 ms |
| `BLIND_RADIUS` | 8.0 blocks |
| `BLIND_MILLIS` | 2000 ms |
| `TELEPORT_MIN_DISTANCE` | 20.0 blocks |
| `TELEPORT_MAX_DISTANCE` | 40.0 blocks |

## Variant Lifecycle (`CreekVariants`)

### Capacity

```
capacity = min(MAX_VARIANTS, ceil(aliveSurvivors / SURVIVORS_PER_VARIANT))
```

with `SURVIVORS_PER_VARIANT = 4` and `MAX_VARIANTS = 3` as constants
(not configurable for now):

| Alive survivors | Capacity |
|---|---|
| 1–4 | 1 |
| 5–8 | 2 |
| 9+ | 3 |

The capacity is recalculated every step. When it drops below the number of
running variants, running variants are **not** cut short; no new variant
starts until there is room again.

### Start

Each step, while `running < capacity`:

1. Candidates are survivors with `dread >= stalkThreshold` that have no
   running variant and no cooldown, and only once `stalkAllowedAt` (the
   round's earliest stalk time, as today) has passed.
2. The candidate is chosen with today's `WanderState.pickTarget` rule: highest
   dread, ties broken by the largest distance to the nearest other survivor.
   This logic moves to `CreekVariants`.
3. A spot is searched with `SpotFinder.beside(...)` exactly as today's
   `WanderState.startStalking` does (between `stalkMinDistance` and
   `stalkMaxDistance`, out of the target's view).
4. If a spot is found, a body is spawned there, shown only to the target, and
   the variant starts in `StalkState.starting(target, ctx)`. If no spot is
   found, nothing starts this step; the next step tries again.

### Running

`StalkState` and `HuntState` run unchanged against a context that holds the
variant's body. A catch during the hunt uses the existing `CatchConsequence`
(`StagedCatchConsequence`: effects, catch counter, possible betrayal).

### End

A variant ends when its state returns `DoneState`, which happens when:

- the stalk or hunt time runs out,
- the target is caught,
- the target is no longer a survivor (dead, left the round, became the
  slender); the existing states already end when `ctx.survivor(target)` is
  empty.

On end, the manager removes the variant's body and gives the target a
cooldown of `VanishState.cooldownMillis(config, dread)` (20–40 s by default,
shorter at higher dread; the existing formula moves or is reused here).

### Round end

`CreekService.stop()` removes all variant bodies and clears all cooldowns, in
addition to what it does today.

## Last Survivor

With `activeWithLastSurvivor = false` and one survivor left:

- no new variants start,
- running variants end immediately (bodies removed, no cooldown needed),
- the patrol entity switches to `VanishState.forever()` as today.

## Changes to Existing Code

- `StalkState`, `HuntState`: every `return VanishState.after(ctx)` becomes
  `return DoneState.INSTANCE`.
- New `DoneState implements CreekState`: `enter` stops the body, `tick`
  returns itself.
- `WanderState` → `PatrolState` as described above; stalk start and proximity
  vanish removed.
- `Creek`: runs the patrol entity only; the last-survivor rule stays here.
- `CreekService`: creates `CreekVariants`, ticks it after the patrol entity
  each step with the same survivor views, and cleans it up on stop.
- `CreekContext`: unchanged fields; the patrol entity and each variant get
  their own context instance with their own body.
- `CreekDebug`: one line for the patrol entity (state, route, selection
  cooldown) plus one line per variant (target name, stalk/hunt, remaining
  time) and the occupancy, e.g. `variants 1/2`.

## Testing

Unit tests with fake bodies and a fake clock, integration tests with the
Minestom test environment, following the existing test layout.

- `PatrolState`
  - selects exactly one survivor within 4 blocks, the nearest;
  - stares for 1 s, then applies the consequence, then walks on;
  - selects nobody during the 10 s cooldown;
  - does not vanish when survivors are near;
  - still pauses when seen from afar.
- `SelectionConsequence`
  - stun: slowness and sound on the selected survivor;
  - stun: blindness for other survivors within 8 blocks, not for the selected
    survivor, not for survivors farther away;
  - teleport: lands 20–40 blocks away on a route point;
  - teleport without a fitting point falls back to the stun;
  - never changes the catch counter.
- `CreekVariants`
  - capacity for 1, 4, 5, 8, 9, 12 survivors is 1, 1, 2, 2, 3, 3;
  - at most one variant per survivor; highest dread first;
  - cooldown after a variant ends;
  - a variant ends when its target dies or leaves;
  - a dropping capacity does not cut a running variant short;
  - no spot found: no variant this step, retried next step.
- `StalkState`, `HuntState`: existing tests updated to expect `DoneState`
  instead of `VanishState`.
- Movement tests (`CreakingBodyIntegrationTest`: slabs, plants, narrow gap)
  stay unchanged.

## Out of Scope

- The WSEE model. It will implement `CreekBody` later; the patrol entity and
  the variants pick it up without further logic changes.
- New config values. All new numbers are constants in code for now.
- Changes to dread, sight checks, routes or catch consequences beyond what is
  listed above.
