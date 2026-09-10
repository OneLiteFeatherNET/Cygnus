# Page-Tooltip Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Provide survivors with an authentic Minecraft client-styled tooltip box displaying the eerie note of a page when looking at it in first-person gameplay.

**Architecture:** The resource pack (`cygnus-pack`) provides modular 9-slice tooltip tiles and negative spacing under `cygnus:font/tooltip.json`. In `Cygnus`, `common` computes text pixel widths (`FontWidthHelper`) and constructs layered Adventure components (`TooltipBox`), while `game` runs a periodic gaze check (`PageGazeService`) targeting nearby pages and displaying the tooltip via `player.showTitle(...)`.

**Tech Stack:** Java 21, Minestom, Kyori Adventure, Gradle Kotlin DSL, Minecraft 1.21.4 Resource Pack Font format.

## Global Constraints

* Branch in Cygnus: `feature/page-tooltip`
* Branch in cygnus-pack: `feature/tooltip-font`
* No tutorial/interaction text ("Page", "[Rechtsklick]"): only the atmospheric page note.
* First-person display via Subtitle with smooth fade-in/fade-out; immediate clear on looking away or collecting the page.

---

### Task 1: Tooltip Font & Slices in `cygnus-pack`

**Files:**
- Create: `/home/theevilreaper/Dokumente/Entwicklung/Github/Packs/cygnus-pack/pack/assets/cygnus/font/tooltip.json`
- Create: `/home/theevilreaper/Dokumente/Entwicklung/Github/Packs/cygnus-pack/pack/assets/cygnus/textures/font/tooltip/corner_tl.png`
- Create: `/home/theevilreaper/Dokumente/Entwicklung/Github/Packs/cygnus-pack/pack/assets/cygnus/textures/font/tooltip/border_top.png`
- Create: `/home/theevilreaper/Dokumente/Entwicklung/Github/Packs/cygnus-pack/pack/assets/cygnus/textures/font/tooltip/corner_tr.png`
- Create: `/home/theevilreaper/Dokumente/Entwicklung/Github/Packs/cygnus-pack/pack/assets/cygnus/textures/font/tooltip/border_left.png`
- Create: `/home/theevilreaper/Dokumente/Entwicklung/Github/Packs/cygnus-pack/pack/assets/cygnus/textures/font/tooltip/bg_fill.png`
- Create: `/home/theevilreaper/Dokumente/Entwicklung/Github/Packs/cygnus-pack/pack/assets/cygnus/textures/font/tooltip/border_right.png`
- Create: `/home/theevilreaper/Dokumente/Entwicklung/Github/Packs/cygnus-pack/pack/assets/cygnus/textures/font/tooltip/corner_bl.png`
- Create: `/home/theevilreaper/Dokumente/Entwicklung/Github/Packs/cygnus-pack/pack/assets/cygnus/textures/font/tooltip/border_bottom.png`
- Create: `/home/theevilreaper/Dokumente/Entwicklung/Github/Packs/cygnus-pack/pack/assets/cygnus/textures/font/tooltip/corner_br.png`

**Interfaces:**
- Produces: Font `cygnus:tooltip` with glyphs:
  - `\uE100` = `corner_tl` (width 3, height 3)
  - `\uE101` = `border_top` (width 1, height 3)
  - `\uE102` = `corner_tr` (width 3, height 3)
  - `\uE103` = `border_left` (width 3, height 10)
  - `\uE104` = `bg_fill` (width 1, height 10)
  - `\uE105` = `border_right` (width 3, height 10)
  - `\uE106` = `corner_bl` (width 3, height 3)
  - `\uE107` = `border_bottom` (width 1, height 3)
  - `\uE108` = `corner_br` (width 3, height 3)
  - Negative space providers: `\uF801` (-1px), `\uF802` (-2px), `\uF804` (-4px), `\uF808` (-8px), `\uF810` (-16px), `\uF820` (-32px), `\uF840` (-64px), `\uF880` (-128px)

- [ ] **Step 1: Slice tooltip sprites into font textures**
Generate the 9 slices using Python `Pillow` from `/home/theevilreaper/Dokumente/Entwicklung/Github/Packs/cygnus-pack/pack/assets/minecraft/textures/gui/sprites/tooltip/frame.png` and `background.png`.

- [ ] **Step 2: Create `tooltip.json` font definition**
Write `pack/assets/cygnus/font/tooltip.json` defining the bitmap providers and space provider advances.

- [ ] **Step 3: Commit in `cygnus-pack`**
```bash
cd /home/theevilreaper/Dokumente/Entwicklung/Github/Packs/cygnus-pack
git add pack/assets/cygnus/font/tooltip.json pack/assets/cygnus/textures/font/tooltip/
git commit -m "feat(font): add modular 9-slice tooltip font"
```

---

### Task 2: Page Note Mapping (`PageNote`)

**Files:**
- Create: `common/src/main/java/net/onelitefeather/cygnus/common/page/PageNote.java`
- Test: `common/src/test/java/net/onelitefeather/cygnus/common/page/PageNoteTest.java`

**Interfaces:**
- Produces: `PageNote.forCustomModel(int modelId): Optional<Component>`

- [ ] **Step 1: Write the failing test**
Create `PageNoteTest` testing that model IDs 1 through 6 resolve to non-empty italic components, and unknown IDs return empty.

- [ ] **Step 2: Run test to verify it fails**
Run: `./gradlew :common:test --tests "net.onelitefeather.cygnus.common.page.PageNoteTest"`
Expected: FAIL (class not found)

- [ ] **Step 3: Implement `PageNote`**
Implement `PageNote` with model mappings for `page_1` to `page_6`.

- [ ] **Step 4: Run test to verify it passes**
Run: `./gradlew :common:test --tests "net.onelitefeather.cygnus.common.page.PageNoteTest"`
Expected: PASS

- [ ] **Step 5: Commit**
```bash
git add common/src/main/java/net/onelitefeather/cygnus/common/page/PageNote.java common/src/test/java/net/onelitefeather/cygnus/common/page/PageNoteTest.java
git commit -m "feat: add PageNote mapping for custom page models"
```

---

### Task 3: Font Width Calculator (`FontWidthHelper`)

**Files:**
- Create: `common/src/main/java/net/onelitefeather/cygnus/common/ui/FontWidthHelper.java`
- Test: `common/src/test/java/net/onelitefeather/cygnus/common/ui/FontWidthHelperTest.java`

**Interfaces:**
- Produces: `FontWidthHelper.getWidth(Component component): int`

- [ ] **Step 1: Write the failing test**
Create `FontWidthHelperTest` asserting character widths for standard strings, spaces, and bold formatting.

- [ ] **Step 2: Run test to verify it fails**
Run: `./gradlew :common:test --tests "net.onelitefeather.cygnus.common.ui.FontWidthHelperTest"`
Expected: FAIL (class not found)

- [ ] **Step 3: Implement `FontWidthHelper`**
Implement character width table and recursive component text visitor calculating total pixel width.

- [ ] **Step 4: Run test to verify it passes**
Run: `./gradlew :common:test --tests "net.onelitefeather.cygnus.common.ui.FontWidthHelperTest"`
Expected: PASS

- [ ] **Step 5: Commit**
```bash
git add common/src/main/java/net/onelitefeather/cygnus/common/ui/FontWidthHelper.java common/src/test/java/net/onelitefeather/cygnus/common/ui/FontWidthHelperTest.java
git commit -m "feat: add FontWidthHelper for Minecraft text width computation"
```

---

### Task 4: Tooltip Box Component (`TooltipBox`)

**Files:**
- Create: `common/src/main/java/net/onelitefeather/cygnus/common/ui/TooltipBox.java`
- Test: `common/src/test/java/net/onelitefeather/cygnus/common/ui/TooltipBoxTest.java`

**Interfaces:**
- Consumes: `FontWidthHelper.getWidth(Component)`
- Produces: `TooltipBox.builder().line(Component).build(): Component`

- [ ] **Step 1: Write the failing test**
Create `TooltipBoxTest` verifying:
- Glyphs for top border, body lines with negative spacing shift, and bottom border.
- Font key set to `cygnus:tooltip` for frame elements.
- Text content embedded and styled correctly.

- [ ] **Step 2: Run test to verify it fails**
Run: `./gradlew :common:test --tests "net.onelitefeather.cygnus.common.ui.TooltipBoxTest"`
Expected: FAIL

- [ ] **Step 3: Implement `TooltipBox`**
Implement builder, 9-slice composition, and negative space decomposition.

- [ ] **Step 4: Run test to verify it passes**
Run: `./gradlew :common:test --tests "net.onelitefeather.cygnus.common.ui.TooltipBoxTest"`
Expected: PASS

- [ ] **Step 5: Commit**
```bash
git add common/src/main/java/net/onelitefeather/cygnus/common/ui/TooltipBox.java common/src/test/java/net/onelitefeather/cygnus/common/ui/TooltipBoxTest.java
git commit -m "feat: implement TooltipBox component builder"
```

---

### Task 5: Page Gaze Detection (`PageGazeService`)

**Files:**
- Create: `game/src/main/java/net/onelitefeather/cygnus/page/PageGazeService.java`
- Test: `game/src/test/java/net/onelitefeather/cygnus/page/PageGazeServiceTest.java`

**Interfaces:**
- Consumes: `PageNote`, `TooltipBox`, `PageProvider`
- Produces: `PageGazeService.startTask()`, `PageGazeService.stopTask()`, `PageGazeService.clearPlayer(Player)`

- [ ] **Step 1: Write the failing test**
Create `PageGazeServiceTest` testing that:
- Looking directly at a page entity within 4 blocks sends the title with the page's note.
- Looking away clears the title.
- Distances beyond 4 blocks do not trigger gaze.

- [ ] **Step 2: Run test to verify it fails**
Run: `./gradlew :game:test --tests "net.onelitefeather.cygnus.page.PageGazeServiceTest"`
Expected: FAIL

- [ ] **Step 3: Implement `PageGazeService`**
Implement the repeating task, survivor filtering, raycast/direction dot-product check, title delivery, and state clearing.

- [ ] **Step 4: Run test to verify it passes**
Run: `./gradlew :game:test --tests "net.onelitefeather.cygnus.page.PageGazeServiceTest"`
Expected: PASS

- [ ] **Step 5: Commit**
```bash
git add game/src/main/java/net/onelitefeather/cygnus/page/PageGazeService.java game/src/test/java/net/onelitefeather/cygnus/page/PageGazeServiceTest.java
git commit -m "feat: implement PageGazeService for first-person page focus"
```

---

### Task 6: Lifecycle Integration & Interactivity

**Files:**
- Modify: `game/src/main/java/net/onelitefeather/cygnus/Cygnus.java`
- Modify: `game/src/main/java/net/onelitefeather/cygnus/listener/page/PlayerPageInteractListener.java`

**Interfaces:**
- Consumes: `PageGazeService`

- [ ] **Step 1: Update `PlayerPageInteractListener`**
Inject or pass `PageGazeService` and call `pageGazeService.clearPlayer(player)` when a page is claimed.

- [ ] **Step 2: Register and start `PageGazeService` in `Cygnus.java`**
Initialize `PageGazeService` during game setup and start/stop its task with round lifecycle.

- [ ] **Step 3: Run full build and tests**
Run: `./gradlew check`
Expected: PASS across all modules.

- [ ] **Step 4: Commit**
```bash
git add game/src/main/java/net/onelitefeather/cygnus/Cygnus.java game/src/main/java/net/onelitefeather/cygnus/listener/page/PlayerPageInteractListener.java
git commit -m "feat: integrate PageGazeService into round lifecycle and pickup listener"
```
