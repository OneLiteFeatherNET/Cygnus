# Modular Multi-Line Tooltip & Inline Bookmark Ribbon Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a modular, multi-line Minecraft-client-style tooltip box with a 4x6 px inline bookmark ribbon, seamless 0-pixel vertical slice alignment, and pre-built startup caching.

**Architecture:** 
- In `cygnus-pack`, a modular 3-band slice set (Header 4px, Body Row 8px, Footer 4px, plus Ribbon 4x6 px) enables seamless scaling to 1, 2, 3+ lines without creating new textures per line count. Lines 2 and 3 use descending font ascents (`cygnus:tooltip_line2`, `cygnus:tooltip_line3`).
- In `Cygnus`, `TooltipBox` splits on `\n`, applies the ribbon prefix on line 1, indents subsequent lines, layers the slices with horizontal negative-space resets, and centers the composite.
- `PageNote` pre-builds and caches the tooltip components at initialization so runtime gaze detection incurs zero component allocation overhead.

**Tech Stack:** Java 25, Minestom, Adventure (Kyori) Components, Minecraft 26.2 Resource Pack Font Engine, Python Pillow (for slice generation), JUnit 5.

## Global Constraints
- Target Minecraft version: **26.2**.
- Repository branches: `feature/page-tooltip` (Cygnus) and `feature/tooltip-font` (cygnus-pack).
- Theme colors: Accent Magenta `(168, 24, 148, 240)` / `#A81894`, Border Grey `(142, 154, 150, 90)` / `#8E9A96`, Depth Fill `(7, 9, 10, 244)` / `#07090A`.
- Geometry: `CAP_WIDTH = 5`, `RIBBON_WIDTH = 4`, `RIBBON_GAP = 3`, `INNER_LEFT_PADDING = 3`, `INNER_RIGHT_PADDING = 4`.
- Text baseline: Compact 6px font (`ascent: 4`, `height: 6` for line 1; `ascent: -4` for line 2; `ascent: -12` for line 3).
- 0-pixel vertical gap: Header (4px), Rows (8px each), Footer (4px) join with exact matching ascents.

---

### Task 1: Generate Modular Slices & Ribbon Textures (`cygnus-pack`)

**Files:**
- Create: `pack/assets/cygnus/textures/font/tooltip/ribbon.png`
- Create: `pack/assets/cygnus/textures/font/tooltip/box_top_left.png`
- Create: `pack/assets/cygnus/textures/font/tooltip/box_top_middle.png`
- Create: `pack/assets/cygnus/textures/font/tooltip/box_top_right.png`
- Create: `pack/assets/cygnus/textures/font/tooltip/box_row_left.png`
- Create: `pack/assets/cygnus/textures/font/tooltip/box_row_middle.png`
- Create: `pack/assets/cygnus/textures/font/tooltip/box_row_right.png`
- Create: `pack/assets/cygnus/textures/font/tooltip/box_bottom_left.png`
- Create: `pack/assets/cygnus/textures/font/tooltip/box_bottom_middle.png`
- Create: `pack/assets/cygnus/textures/font/tooltip/box_bottom_right.png`

**Interfaces:**
- Produces: 10 PNG texture assets for modular 3-band tooltip construction and inline bookmark ribbon.

- [ ] **Step 1: Write Python generation script for modular slices**

Create temporary script `scratch/generate_modular_slices.py`:
```python
from PIL import Image

ACCENT = (168, 24, 148, 240)       # #A81894
ACCENT_DARK = (115, 14, 100, 255)  # Shadow for ribbon
BORDER = (142, 154, 150, 90)       # #8E9A96
DEPTH = (7, 9, 10, 244)            # #07090A
TRANSPARENT = (0, 0, 0, 0)

OUT_DIR = '/home/theevilreaper/Dokumente/Entwicklung/Github/Packs/cygnus-pack/pack/assets/cygnus/textures/font/tooltip/'

# 1. ribbon.png (4x6 px)
ribbon = Image.new('RGBA', (4, 6), TRANSPARENT)
for y in range(4):
    ribbon.putpixel((0, y), ACCENT_DARK)
    ribbon.putpixel((1, y), ACCENT)
    ribbon.putpixel((2, y), ACCENT)
    ribbon.putpixel((3, y), ACCENT_DARK)
# Row 4 (notch)
ribbon.putpixel((0, 4), ACCENT_DARK)
ribbon.putpixel((1, 4), ACCENT)
ribbon.putpixel((2, 4), ACCENT)
ribbon.putpixel((3, 4), ACCENT_DARK)
# Row 5 (tips)
ribbon.putpixel((0, 5), ACCENT_DARK)
ribbon.putpixel((3, 5), ACCENT_DARK)
ribbon.save(OUT_DIR + 'ribbon.png')

# 2. Header Band (4px high)
# box_top_left.png (5x4 px): Magenta top bracket (4px arms)
top_left = Image.new('RGBA', (5, 4), DEPTH)
for x in range(4): top_left.putpixel((x, 0), ACCENT)
top_left.putpixel((4, 0), BORDER)
for y in range(4): top_left.putpixel((0, y), ACCENT)
top_left.save(OUT_DIR + 'box_top_left.png')

# box_top_middle.png (1x4 px): 1px top border, 3px fill
top_middle = Image.new('RGBA', (1, 4), DEPTH)
top_middle.putpixel((0, 0), BORDER)
top_middle.save(OUT_DIR + 'box_top_middle.png')

# box_top_right.png (5x4 px): Magenta top-right bracket (4px arms)
top_right = Image.new('RGBA', (5, 4), DEPTH)
top_right.putpixel((0, 0), BORDER)
for x in range(1, 5): top_right.putpixel((x, 0), ACCENT)
for y in range(4): top_right.putpixel((4, y), ACCENT)
top_right.save(OUT_DIR + 'box_top_right.png')

# 3. Row Body Band (8px high)
# box_row_left.png (5x8 px): 1px left border, 4px fill
row_left = Image.new('RGBA', (5, 8), DEPTH)
for y in range(8): row_left.putpixel((0, y), BORDER)
row_left.save(OUT_DIR + 'box_row_left.png')

# box_row_middle.png (1x8 px): 8px fill
row_middle = Image.new('RGBA', (1, 8), DEPTH)
row_middle.save(OUT_DIR + 'box_row_middle.png')

# box_row_right.png (5x8 px): 4px fill, 1px right border
row_right = Image.new('RGBA', (5, 8), DEPTH)
for y in range(8): row_right.putpixel((4, y), BORDER)
row_right.save(OUT_DIR + 'box_row_right.png')

# 4. Footer Band (4px high)
# box_bottom_left.png (5x4 px): Magenta bottom-left bracket (4px arms)
bottom_left = Image.new('RGBA', (5, 4), DEPTH)
for y in range(4): bottom_left.putpixel((0, y), ACCENT)
for x in range(4): bottom_left.putpixel((x, 3), ACCENT)
bottom_left.putpixel((4, 3), BORDER)
bottom_left.save(OUT_DIR + 'box_bottom_left.png')

# box_bottom_middle.png (1x4 px): 3px fill, 1px bottom border
bottom_middle = Image.new('RGBA', (1, 4), DEPTH)
bottom_middle.putpixel((0, 3), BORDER)
bottom_middle.save(OUT_DIR + 'box_bottom_middle.png')

# box_bottom_right.png (5x4 px): Magenta bottom-right bracket (4px arms)
bottom_right = Image.new('RGBA', (5, 4), DEPTH)
bottom_right.putpixel((0, 3), BORDER)
for x in range(1, 5): bottom_right.putpixel((x, 3), ACCENT)
for y in range(4): bottom_right.putpixel((4, y), ACCENT)
bottom_right.save(OUT_DIR + 'box_bottom_right.png')

print('All 10 modular textures generated successfully.')
```

- [ ] **Step 2: Run generation script and verify PNG dimensions**

Run: `python3 scratch/generate_modular_slices.py`
Verify output and check image sizes with `file pack/assets/cygnus/textures/font/tooltip/*.png`.

- [ ] **Step 3: Commit in cygnus-pack**

```bash
cd /home/theevilreaper/Dokumente/Entwicklung/Github/Packs/cygnus-pack
git add pack/assets/cygnus/textures/font/tooltip/
git commit -m "feat(font): add modular 3-band box slices and inline bookmark ribbon"
```

---

### Task 2: Configure Modular Font Definitions (`cygnus-pack`)

**Files:**
- Modify: `pack/assets/cygnus/font/tooltip.json`
- Create: `pack/assets/cygnus/font/tooltip_line2.json`
- Create: `pack/assets/cygnus/font/tooltip_line3.json`

**Interfaces:**
- Produces:
  - Font `cygnus:tooltip`: Ribbon (`\uE103`), Header (`\uE110`-`\uE112`), Row 1 Body (`\uE113`-`\uE115`), Row 1 Footer (`\uE116`-`\uE118`), Text Line 1 (`ascii.png` at `ascent: 4`).
  - Font `cygnus:tooltip_line2`: Row 2 Body (`\uE113`-`\uE115` at `ascent: -4`), Row 2 Footer (`\uE116`-`\uE118` at `ascent: -12`), Text Line 2 (`ascii.png` at `ascent: -4`).
  - Font `cygnus:tooltip_line3`: Row 3 Body (`ascent: -12`), Row 3 Footer (`ascent: -20`), Text Line 3 (`ascent: -12`).

- [ ] **Step 1: Update `pack/assets/cygnus/font/tooltip.json`**

Configure modular slices and line 1 providers:
- `\uE103`: `ribbon.png`, `ascent: 4`, `height: 6`
- `\uE110`: `box_top_left.png`, `ascent: 8`, `height: 4`
- `\uE111`: `box_top_middle.png`, `ascent: 8`, `height: 4`
- `\uE112`: `box_top_right.png`, `ascent: 8`, `height: 4`
- `\uE113`: `box_row_left.png`, `ascent: 4`, `height: 8`
- `\uE114`: `box_row_middle.png`, `ascent: 4`, `height: 8`
- `\uE115`: `box_row_right.png`, `ascent: 4`, `height: 8`
- `\uE116`: `box_bottom_left.png`, `ascent: -4`, `height: 4`
- `\uE117`: `box_bottom_middle.png`, `ascent: -4`, `height: 4`
- `\uE118`: `box_bottom_right.png`, `ascent: -4`, `height: 4`
- Keep existing `\uE100`-`\uE102` for backward compatibility.
- `minecraft:font/ascii.png` with full 16x16 grid, `ascent: 4`, `height: 6`.
- `reference: space:default`.
- Last entry: `space` provider with `" ": 4` (highest priority).

- [ ] **Step 2: Create `pack/assets/cygnus/font/tooltip_line2.json` and `tooltip_line3.json`**

Create `tooltip_line2.json` with matching slice glyphs and `ascii.png` at `ascent: -4` (Body) and `ascent: -12` (Footer).
Create `tooltip_line3.json` with matching slice glyphs and `ascii.png` at `ascent: -12` (Body) and `ascent: -20` (Footer).

- [ ] **Step 3: Validate JSON syntax with python**

Run: `python3 -m json.tool pack/assets/cygnus/font/tooltip.json > /dev/null`
Run: `python3 -m json.tool pack/assets/cygnus/font/tooltip_line2.json > /dev/null`
Run: `python3 -m json.tool pack/assets/cygnus/font/tooltip_line3.json > /dev/null`
Expected: Return code 0.

- [ ] **Step 4: Commit in cygnus-pack**

```bash
cd /home/theevilreaper/Dokumente/Entwicklung/Github/Packs/cygnus-pack
git add pack/assets/cygnus/font/
git commit -m "feat(font): add modular slice and multi-line font definitions"
```

---

### Task 3: Update `FontWidthHelper` for Ribbon & Multi-Line Measurement (`Cygnus`)

**Files:**
- Modify: `common/src/main/java/net/onelitefeather/cygnus/common/ui/FontWidthHelper.java`
- Modify: `common/src/test/java/net/onelitefeather/cygnus/common/ui/FontWidthHelperTest.java`

**Interfaces:**
- Consumes: Nothing
- Produces:
  - `FontWidthHelper.getMaxTooltipLineWidth(@Nullable String text)`: computes the maximum line width across `\n`-separated lines.
  - `FontWidthHelper.getMaxTooltipLineWidth(@Nullable Component component)`: computes maximum line width across lines.
  - `FontWidthHelper.TOOLTIP_ASCII_WIDTHS['l'] = 2` (confirmed in previous step).

- [ ] **Step 1: Write failing test in `FontWidthHelperTest.java`**

Add tests:
```java
@Test
@DisplayName("Multi-line string returns max line width")
void testMultiLineStringWidth() {
    // Line 1: "Always watches," -> A(5)+l(2)+w(5)+a(5)+y(5)+s(5)+' '(4)+w(5)+a(5)+t(3)+c(5)+h(5)+e(5)+s(5)+,(2) = 61
    // Line 2: "no eyes" -> n(5)+o(5)+' '(4)+e(5)+y(5)+e(5)+s(5) = 34
    String multiLine = "Always watches,\nno eyes";
    assertEquals(61, FontWidthHelper.getMaxTooltipLineWidth(multiLine));
}
```

- [ ] **Step 2: Run test to verify failure**

Run: `./gradlew :common:test --tests "net.onelitefeather.cygnus.common.ui.FontWidthHelperTest"`
Expected: Compilation failure (method `getMaxTooltipLineWidth` does not exist yet).

- [ ] **Step 3: Implement `getMaxTooltipLineWidth` in `FontWidthHelper.java`**

```java
public static int getMaxTooltipLineWidth(@Nullable String text) {
    if (text == null || text.isEmpty()) {
        return 0;
    }
    int maxWidth = 0;
    String[] lines = text.split("\n", -1);
    for (String line : lines) {
        maxWidth = Math.max(maxWidth, getTooltipWidth(line));
    }
    return maxWidth;
}
```
And overload for `Component` splitting text content on `\n`.

- [ ] **Step 4: Run test to verify pass**

Run: `./gradlew :common:test --tests "net.onelitefeather.cygnus.common.ui.FontWidthHelperTest"`
Expected: PASS.

- [ ] **Step 5: Commit in Cygnus**

```bash
git add common/src/main/java/net/onelitefeather/cygnus/common/ui/FontWidthHelper.java common/src/test/java/net/onelitefeather/cygnus/common/ui/FontWidthHelperTest.java
git commit -m "feat(ui): add multi-line width calculation to FontWidthHelper"
```

---

### Task 4: Implement Modular Multi-Line `TooltipBox` with Inline Ribbon (`Cygnus`)

**Files:**
- Modify: `common/src/main/java/net/onelitefeather/cygnus/common/ui/TooltipBox.java`
- Modify: `common/src/test/java/net/onelitefeather/cygnus/common/ui/TooltipBoxTest.java`

**Interfaces:**
- Consumes: `SpaceHelper`, `FontWidthHelper`, `Key.key("cygnus", "tooltip")`, `Key.key("cygnus", "tooltip_line2")`, `Key.key("cygnus", "tooltip_line3")`.
- Produces:
  - `TooltipBox.RIBBON = "\uE103"`
  - `TooltipBox.CAP_WIDTH = 5`, `RIBBON_WIDTH = 4`, `RIBBON_GAP = 3`, `INNER_LEFT_PADDING = 3`, `INNER_RIGHT_PADDING = 4`.
  - `TooltipBox.Builder.line(Component)` / `TooltipBox.of(...)` supporting single and multi-line (`\n`) inputs seamlessly.

- [ ] **Step 1: Write failing tests in `TooltipBoxTest.java`**

```java
@Test
@DisplayName("Single line TooltipBox includes ribbon glyph and correct layout")
void testSingleLineWithRibbon() {
    Component box = TooltipBox.of(Component.text("Help me"));
    String plain = PLAIN.serialize(box);
    assertTrue(plain.contains(TooltipBox.RIBBON), "Must contain ribbon glyph");
    assertTrue(plain.contains("Help me"), "Must contain note text");
}

@Test
@DisplayName("Multi-line string with newline builds multi-line components")
void testMultiLineBoxBuild() {
    Component box = TooltipBox.of(Component.text("Always watches,\nno eyes"));
    String plain = PLAIN.serialize(box);
    assertTrue(plain.contains(TooltipBox.RIBBON));
    assertTrue(plain.contains("Always watches,"));
    assertTrue(plain.contains("no eyes"));
    assertFalse(plain.contains("\n"), "Rendered component should not contain raw newline");
}
```

- [ ] **Step 2: Run test to verify failure**

Run: `./gradlew :common:test --tests "net.onelitefeather.cygnus.common.ui.TooltipBoxTest"`
Expected: FAIL.

- [ ] **Step 3: Implement modular multi-line logic in `TooltipBox.java`**

- Define constants:
  ```java
  public static final Key FONT_LINE1 = Key.key("cygnus", "tooltip");
  public static final Key FONT_LINE2 = Key.key("cygnus", "tooltip_line2");
  public static final Key FONT_LINE3 = Key.key("cygnus", "tooltip_line3");
  public static final String RIBBON = "\uE103";
  public static final String TOP_LEFT = "\uE110";
  public static final String TOP_MIDDLE = "\uE111";
  public static final String TOP_RIGHT = "\uE112";
  public static final String ROW_LEFT = "\uE113";
  public static final String ROW_MIDDLE = "\uE114";
  public static final String ROW_RIGHT = "\uE115";
  public static final String BOTTOM_LEFT = "\uE116";
  public static final String BOTTOM_MIDDLE = "\uE117";
  public static final String BOTTOM_RIGHT = "\uE118";
  public static final int CAP_WIDTH = 5;
  public static final int RIBBON_WIDTH = 4;
  public static final int RIBBON_GAP = 3;
  public static final int INNER_LEFT_PADDING = 3;
  public static final int INNER_RIGHT_PADDING = 4;
  public static final int TEXT_LEFT_INDENT = INNER_LEFT_PADDING + RIBBON_WIDTH + RIBBON_GAP; // 10px
  ```
- In `build()`:
  - Extract text lines from `this.lines`, splitting on `\n`.
  - Calculate line widths via `FontWidthHelper.getTooltipWidth(line)`.
  - Content width = `TEXT_LEFT_INDENT + max(line_width_i)`.
  - `middleTiles = contentWidth + INNER_RIGHT_PADDING`.
  - `boxWidth = CAP_WIDTH + middleTiles + CAP_WIDTH`.
  - Assemble Header band (`TOP_LEFT + \uF001 + (TOP_MIDDLE + \uF001)*middleTiles + TOP_RIGHT + \uF001`).
  - Assemble Body bands for each line $k \in [1..N]$ with matching font keys `FONT_LINE1`, `FONT_LINE2`, `FONT_LINE3`.
  - Assemble Footer band at the bottom font.
  - Draw Ribbon and Text:
    - Line 1: `RIBBON` + `getPositiveSpace(RIBBON_GAP)` + `line1Text`.
    - Line $k \ge 2$: cursor shift back, indent by `TEXT_LEFT_INDENT`, draw `lineKText` in `FONT_LINE[k]`.
  - Final compensation advance.

- [ ] **Step 4: Run test to verify pass**

Run: `./gradlew :common:test --tests "net.onelitefeather.cygnus.common.ui.TooltipBoxTest"`
Expected: PASS.

- [ ] **Step 5: Commit in Cygnus**

```bash
git add common/src/main/java/net/onelitefeather/cygnus/common/ui/TooltipBox.java common/src/test/java/net/onelitefeather/cygnus/common/ui/TooltipBoxTest.java
git commit -m "feat(ui): implement modular multi-line TooltipBox with inline ribbon"
```

---

### Task 5: Pre-build & Cache Tooltips in `PageNote` & Lifecycle (`Cygnus`)

**Files:**
- Modify: `common/src/main/java/net/onelitefeather/cygnus/common/page/PageNote.java`
- Modify: `common/src/test/java/net/onelitefeather/cygnus/common/page/PageNoteTest.java`
- Modify: `game/src/main/java/net/onelitefeather/cygnus/page/PageGazeService.java`
- Modify: `game/src/test/java/net/onelitefeather/cygnus/page/PageGazeServiceTest.java`

**Interfaces:**
- Consumes: `TooltipBox.of(...)`, `PageNote`
- Produces:
  - `PageNote.getTooltipComponent()`: returns the pre-built, ready-to-display tooltip component built at startup.
  - `PageNote.tooltipForItem(@Nullable ItemStack itemStack)`: $O(1)$ cache lookup returning pre-built tooltip component.
  - `PageGazeService`: zero component rebuilding on gaze ticks.

- [ ] **Step 1: Write test for pre-built tooltip components in `PageNoteTest.java`**

```java
@Test
@DisplayName("PageNote pre-builds tooltip components at startup")
void testPrebuiltTooltipComponent() {
    for (PageNote note : PageNote.values()) {
        Component tooltip = note.getTooltipComponent();
        assertNotNull(tooltip);
        assertFalse(PlainTextComponentSerializer.plainText().serialize(tooltip).isEmpty());
    }
}
```

- [ ] **Step 2: Run test to verify failure**

Run: `./gradlew :common:test --tests "net.onelitefeather.cygnus.common.page.PageNoteTest"`
Expected: FAIL (method `getTooltipComponent` does not exist).

- [ ] **Step 3: Implement pre-built caching in `PageNote.java`**

- Update note texts with optimal multi-line formatting:
  ```java
  ALWAYS_WATCHES(1, "Always watches,\nno eyes"),
  DONT_LOOK(2, "Don't look\nor it takes you"),
  CANT_RUN(3, "Can't run"),
  LEAVE_ME_ALONE(4, "Leave me alone"),
  HELP_ME(5, "Help me"),
  NO_NO_NO(6, "No no no\nno...");
  ```
- Store `private final Component tooltipComponent;` initialized in constructor or static initializer:
  `this.tooltipComponent = TooltipBox.of(this.component);`
- Provide `public Component getTooltipComponent()` and `public static Optional<Component> tooltipForItem(@Nullable ItemStack itemStack)`.

- [ ] **Step 4: Update `PageGazeService.java` to use pre-built cache**

In `updatePlayerGaze`:
```java
Optional<Component> tooltip = PageNote.tooltipForItem(bestPage.getPageItem());
if (tooltip.isPresent()) {
    player.showTitle(Title.title(Component.empty(), tooltip.get(), TITLE_TIMES));
    this.activeGaze.put(player, currentUuid);
}
```

- [ ] **Step 5: Run full verification suite**

Run: `./gradlew check`
Expected: All tests across `:common`, `:game`, and `:setup` PASS.

- [ ] **Step 6: Commit in Cygnus**

```bash
git add common/src/main/java/net/onelitefeather/cygnus/common/page/PageNote.java common/src/test/java/net/onelitefeather/cygnus/common/page/PageNoteTest.java game/src/main/java/net/onelitefeather/cygnus/page/PageGazeService.java game/src/test/java/net/onelitefeather/cygnus/page/PageGazeServiceTest.java
git commit -m "perf(page): pre-build and cache tooltip components at startup"
```
