# Role visibility: Slender / Survivor / Spectator

Status: Implemented
Branch: `fix/role-visibility-matrix`

## 1. Target matrix

Who (row) may see whom (column) as an entity:

| Viewer | Slender | Survivor | Spectator |
|---|---|---|---|
| **Slender**   | –                       | yes | **never** |
| **Survivor**  | only while revealed     | yes | **never** |
| **Spectator** | same as the survivor view | yes | **yes** |

"only while revealed" means `Tags.HIDDEN == VISIBLE`, set by `SlenderBar.enterDraining()`.

Chat:

| Sender | Recipients |
|---|---|
| Survivor | everyone |
| Slender  | everyone |
| Spectator | spectators only — **in every phase** |

## 2. Root cause

Visibility used to be driven by **two competing mechanisms** that desynchronized:

1. **Viewable rule** (`updateViewableRule`) — maintains `EntityView.Option.bitSet`
2. **Manual viewer packets** (`updateNewViewer` / `updateOldViewer`) — purely packet based,
   they never touch the bit set (Minestom `Entity.java:553` / `:579`)

Observed consequences:
- `GameStartListener:58` hid the slender through `updateOldViewer` while the bit set kept everyone
  registered. A later `updateViewableRule()` therefore saw `isRegistered == true` and sent **no**
  spawn packet.
- `enterDraining` sent `updateNewViewer` manually, after which `SlenderBarTrigger:58` re-evaluated
  the rule -> **duplicated spawn packets**.
- The automatic transition once the bar ran dry (`SlenderBar:109`) went through mechanism 2 **only**
  and was silently reverted by the next `updateViewableRule()`.

**Decision: a single layer.** Per-viewer predicates exclusively. Every manual
`updateNewViewer` / `updateOldViewer` / `broadcastPlayPacket` call is gone.

This works because the predicate receives the **viewer** in Minestom `2026.07.22-26.2`
(`EntityView.updateRule0`, `predicate.test(entity)`) and is also evaluated whenever somebody enters
range (`EntityView:73`).

Constraint: `addViewer` / `removeViewer` must **not** be used — they record the player in
`manualViewers`, and `update()` skips those players permanently (`EntityView:258`).

## 3. Findings and measures

### P0 — target matrix (implemented here)

| # | File:line | Finding | Measure |
|---|---|---|---|
| 1 | `spectator/SpectatorService.java:61` | `_ -> false` — spectators could not see each other | `viewer -> TeamHelper.isSpectatorTeam(viewer)` |
| 2 | `team/TeamHelper.java:77` | predicate ignored the viewer | explicit per-viewer rule via `VisibilityRules` |
| 3 | `listener/game/SlenderReviveListener.java:33-42` | set neither rule nor `Tags.HIDDEN` -> revived slender permanently visible | set both |
| 4 | `team/TeamHelper.java:75-79` | rule installed before `Tags.HIDDEN` existed -> slender visible between GamePrepare and GameStart | set `Tags.HIDDEN = HIDDEN` in `assignSlender` |
| 5 | `listener/stamina/StaminaStateChangeListener.java:26-37` | bypassed the rule, broadcast metadata to everyone | replaced by `updateViewableRule()` |
| 6 | `listener/game/GameStartListener.java:54-59` | same pattern | same |
| 7 | `stamina/SlenderBar.java:109` | automatic transition did not re-evaluate the rule | resolved by removing the competing path instead |
| 8 | `utils/ViewRuleUpdater.java:11-13` | `isViewAble` — dead code, inverted name | deleted |
| 9 | `utils/ViewRuleUpdater.java:15-22` | iterated survivors twice | simplified into `VisibilityRules.refresh` |
| 10 | `listener/PlayerChatListener.java:31-34` | fail-open: `null instanceof GamePhase == false` leaked spectator chat to everyone, plus a gap during the restart phase | fail-closed, independent of the phase |
| 11 | `stamina/SlenderBarHelper.java:82-91` | `setHealth` without team/game mode filter -> spectators took damage | survivors only |
| 12 | `stamina/SlenderBarHelper.java:123-133` | teleport sound without role filter -> spectators heard when the slender vanished | survivors only |
| 13 | `listener/player/CygnusPlayerTickListener.java:19-22` | jumpscare on every tick without filter -> slender received `DARKNESS` for 40 ticks | survivors only |

Follow-up chain of #11: a spectator died -> `PlayerDeathListener:55` broadcast a second death
message, `:57` removed the tag, `:58` fired another `SpectatorAddEvent` -> `SpectatorService.join`
ran twice, `:61` re-checked the finish condition. Fixing #11 removes the cause.

### P1 — adjacent leaks, deliberately NOT part of this change

Found during the analysis; each needs a design decision:

- **`team/TeamHelper.java:157-161`** — `updateTabList` gives the slender the display name
  `"⛧ " + name` in red. `setDisplayName` broadcasts `UPDATE_DISPLAY_NAME` to everyone
  (Minestom `Player.java:1188-1193`). **Every survivor and every spectator immediately sees who the
  slender is in the tab list** — and the same display name sits under every chat message
  (`PlayerChatListener:62`). The largest remaining leak, but possibly intentional design.
- **`common/.../page/PageProvider.java:165`** — page discoveries are broadcast to everyone, so the
  slender learns in real time which survivor is making progress where.
- **`PlayerDeathListener.java:55`** — death messages go to everyone; the slender gets every kill
  confirmed.
- **`entity/DeadPlayerMannequin.java:109-114`** — particles sent through
  `instance.sendGroupedPacket` bypass every viewable rule and reveal the corpse position even while
  a jumpscare hides it.
- **`listener/game/GameFinishListener.java:36-41`** — dead players carry `SPECTATOR_KEY`, so
  `isSlenderTeam` is false and they all render as survivor boxes.
- **`utils/ScoreboardDisplay.java`** — dead code; `getTeamName:86-88` would map spectators into the
  survivor team, and `TeamCreator:41-43` lacks the `TeamNameComponent`, which would throw an NPE.
- **`Cygnus.java`** (`finishGame`) — no reset of rule / `Tags.HIDDEN` / `TEAM_KEY`. Harmless today
  (`RestartPhase` kicks everyone and calls `stopCleanly()`, one process serves exactly one round),
  but immediately relevant once a round reset or map switch is introduced.

## 4. Implementation

### `game/.../visibility/VisibilityRules.java`

Holds the matrix in a single place, as per-viewer predicates:

- `slenderRule(Player slender)` -> `viewer -> !isHidden(slender)`
- `spectatorRule()` -> `TeamHelper::isSpectatorTeam`
- survivors deliberately get **no** rule (Minestom default = visible to everyone)
- `refresh(Player)` -> re-evaluates the rules of the player and of every other online player,
  which is required because `spectatorRule()` tests the *viewer*

`ViewRuleUpdater` was removed and absorbed into this class.

### Tests

Based on `CygnusPlayerTestBase` + `MicrotusExtension`, asserting through `player.isViewer(other)`.

`SpectatorServiceTest.testJoinMakesPlayerInvisibleToOthers` cemented finding 1 and was replaced.

One test per matrix cell plus regression tests:
- spectator sees spectator, spectator sees survivor
- survivor does not see spectator, slender does not see spectator
- spectator does not see a hidden slender, spectator sees a revealed slender
- revived slender is hidden
- spectator chat reaches spectators only — game phase, restart phase and without an active phase
- spectator takes no slender damage, only survivors hear the sounds, no jumpscares for
  slender/spectator

### Known follow-up

`stamina` -> `team` is now a package cycle, because `TeamHelper` reads the `HIDDEN` constant from
`SlenderBarHelper`. Moving `VISIBLE` / `HIDDEN` into a neutral holder would resolve it.
