# Page-Notiz als TextDisplay in der Welt: Implementierungsplan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Die Notiz einer Page erscheint als `TextDisplay` direkt an der Page, nur für den lesenden Spieler, statt als Subtitle über dem Bildschirm.

**Architecture:** `TooltipBox` bekommt eine zentrierte Variante. `PageNote` baut daraus beim Start ein Welt-Component. `PageNoteDisplay` kapselt ein `TextDisplay` für einen Spieler und eine Page: Spawnen, Ein- und Ausblenden über die Skalierung, Entfernen. `PageGazeService` behält den Blickkegel und seine öffentliche API, hält pro Spieler aber einen Zustand mit Verweilen, Hysterese und Nachlauf und zeigt statt Titeln Displays.

**Tech Stack:** Java 25, Minestom 2026.09.12-26.2 (`TextDisplayMeta`, `AbstractDisplayMeta`), Adventure, JUnit 5 + Cyano 0.7.x (`MicrotusExtension`, `Env`, `TestConnection`).

**Spec:** `docs/superpowers/specs/2026-10-03-page-note-world-display-design.md`

## Global Constraints

- Javadoc auf allen neuen Typen mit `@author theEvilReaper`, `@version 1.0.0`, `@since 2.15.0`. Bei geänderten Typen wird `@version` in der Minor-Stelle erhöht.
- Schwellen in `PageGazeService`: Einblenden bei Cosinus ≥ `0.85` und Abstand ≤ `4.0`. Sichtbar bleiben bei Cosinus ≥ `0.70` und Abstand ≤ `5.5`. `DWELL_TICKS = 6`, `LINGER_TICKS = 10`. `UPDATE_INTERVAL_TICKS` bleibt `2`.
- `TextDisplay`: Hintergrund `0x00000000`, `shadow = false`, `seeThrough = false`, `lineWidth = 1000`, Billboard `CENTER`, Helligkeit `15/15`, `NOTE_SCALE = 0.35`, Versatz Richtungsvektor der Page · `0.15` + `0.45` nach oben.
- Einblenden: Spawn mit Scale `0`, im nächsten Tick Interpolation über `4` Ticks auf `NOTE_SCALE`. Ausblenden: Interpolation über `3` Ticks auf Scale `0`, danach `remove()`.
- Die `Interaction`-Hitbox von `PageEntity` und `PlayerPageInteractListener` werden nicht angefasst.
- Die öffentliche API von `PageGazeService` bleibt: `startTask`, `stopTask`, `isRunning`, `clearPlayer`, `isGazing`, `activeGaze`, `tick`.
- Tests laufen mit `./gradlew :<modul>:test --tests "<Klasse>"`.

## Review Focus

Diese Eingaben und Fehlerfälle deckt die Spec implizit ab, die Tabelle unter „Auslöser“ aber nicht ausdrücklich:

1. **Spieler verlässt das Survivor-Team** (Tod, Takeover, Quit), während eine Notiz offen ist. Erwartet: Das Display verschwindet sofort und bleibt nicht als Waise in der Welt. Test in Task 4: `survivorLeavingRemovesNoteImmediately`.
2. **Zwei Survivors lesen dieselbe Page.** Erwartet: zwei getrennte Displays mit je einem Viewer. Wer wegschaut, nimmt dem anderen die Notiz nicht weg. Test in Task 4: `twoReadersGetSeparateDisplays`.
3. **Die Page wird verschoben, während sie gelesen wird** (`relocate` nach TTL). Erwartet: Die Notiz verschwindet sofort und bleibt nicht am alten Ort stehen. Test in Task 4: `relocatedPageRemovesNoteImmediately`.
4. **`stopTask` während eine Notiz gerade ausblendet.** Erwartet: kein Fehler und kein Entity, das übrig bleibt. Test in Task 4: `stopTaskDuringFadeOutLeavesNoDisplay`.
5. **Page ohne Notiz-Modell** (`ItemStack` ohne `page_<n>`). Erwartet: Nie ein Display, auch nicht nach langem Verweilen. Test in Task 4: `pageWithoutNoteNeverShowsDisplay`.

---

### Task 1: `TooltipBox#centered()`

**Files:**
- Modify: `common/src/main/java/net/onelitefeather/cygnus/common/ui/TooltipBox.java` (Builder-Feld bei Zeile 147, `crosshairOffsetX` bei Zeile 205, `build()` bei Zeile 240–246, Klassen-`@version`)
- Test: `common/src/test/java/net/onelitefeather/cygnus/common/ui/TooltipBoxTest.java`

**Interfaces:**
- Consumes: nichts.
- Produces: `TooltipBox.Builder centered()`. Danach liefert `build()` ein Component ohne vorangestellte Leerstelle, das erste sichtbare Zeichen ist `TooltipBox.TOP_LEFT`.

- [ ] **Step 1: Write the failing test**

An das Ende der Klasse `TooltipBoxTest` anhängen:

```java
    @Test
    @DisplayName("Centered box has no leading space and starts with the top-left corner")
    void testCenteredBoxHasNoLeadingSpace() {
        Component centered = TooltipBox.builder().centered().line(Component.text("Help me")).build();
        String plain = PLAIN.serialize(centered);
        assertTrue(plain.startsWith(TooltipBox.TOP_LEFT),
                "a centered box must start with the box itself, got: " + plain.codePointAt(0));
    }

    @Test
    @DisplayName("Centered box is the default box without its leading space")
    void testCenteredBoxEqualsDefaultWithoutLeadingSpace() {
        Component line = Component.text("Don't look\nor it takes you");
        String centered = PLAIN.serialize(TooltipBox.builder().centered().line(line).build());
        String anchored = PLAIN.serialize(TooltipBox.builder().line(line).build());
        assertTrue(anchored.endsWith(centered), "only the leading space may differ");
        String leading = anchored.substring(0, anchored.length() - centered.length());
        assertFalse(leading.isEmpty(), "the default box must keep its crosshair offset");
        for (char c : leading.toCharArray()) {
            assertTrue(c >= '' && c <= '', "leading part must be positive space only");
        }
    }

    @Test
    @DisplayName("centered() ignores the crosshair offset")
    void testCenteredIgnoresCrosshairOffset() {
        Component line = Component.text("Help me");
        Component a = TooltipBox.builder().centered().crosshairOffsetX(40).line(line).build();
        Component b = TooltipBox.builder().centered().line(line).build();
        assertEquals(PLAIN.serialize(b), PLAIN.serialize(a));
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :common:test --tests "net.onelitefeather.cygnus.common.ui.TooltipBoxTest"`
Expected: FAIL, Kompilierfehler `cannot find symbol: method centered()`.

- [ ] **Step 3: Write minimal implementation**

Im `Builder` neben `crosshairOffsetX` ein Feld ergänzen:

```java
        private final List<Component> lines = new ArrayList<>();
        private int crosshairOffsetX = DEFAULT_CROSSHAIR_OFFSET;
        private boolean centered;
```

Direkt nach der Methode `crosshairOffsetX(int)` einfügen:

```java
        /**
         * Centers the box on the anchor of whatever renders it, instead of placing it beside the
         * crosshair. Meant for text displays in the world: they center each line on their own
         * position, so any leading space would push the box off the page. The crosshair offset
         * has no effect once this is set.
         *
         * @return this builder
         */
        @NotNull
        public Builder centered() {
            this.centered = true;
            return this;
        }
```

In `build()` den Block „Step 1“ ersetzen:

```java
            // Step 1: Leading space for crosshair offset. A centered box has none: its total
            // advance is exactly boxWidth, so it sits centered on the renderer's anchor.
            TextComponent.Builder root = Component.text().font(FONT);
            if (!this.centered) {
                String leadingSpaceStr = getPositiveSpace(boxWidth + (2 * this.crosshairOffsetX));
                if (!leadingSpaceStr.isEmpty()) {
                    root.append(Component.text(leadingSpaceStr).font(FONT));
                }
            }
```

Die Klassen-`@version` von `2.2.0` auf `2.3.0` erhöhen.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :common:test --tests "net.onelitefeather.cygnus.common.ui.TooltipBoxTest"`
Expected: PASS, alle Tests der Klasse grün, auch die bestehenden.

- [ ] **Step 5: Commit**

```bash
git add common/src/main/java/net/onelitefeather/cygnus/common/ui/TooltipBox.java common/src/test/java/net/onelitefeather/cygnus/common/ui/TooltipBoxTest.java
git commit -m "feat(ui): add centered variant to TooltipBox for world displays"
```

---

### Task 2: Welt-Component in `PageNote`, `PageEntity#getResource()` öffentlich

**Files:**
- Modify: `common/src/main/java/net/onelitefeather/cygnus/common/page/PageNote.java`
- Modify: `common/src/main/java/net/onelitefeather/cygnus/common/page/PageEntity.java:283` (`getResource()`)
- Test: `common/src/test/java/net/onelitefeather/cygnus/common/page/PageNoteTest.java`

**Interfaces:**
- Consumes: `TooltipBox.builder().centered()` aus Task 1.
- Produces:
  - `Component PageNote#getWorldComponent()`
  - `static Optional<Component> PageNote.worldComponentForItem(@Nullable ItemStack)`
  - `public PageResource PageEntity#getResource()`

Die Titel-Methoden (`getTooltipComponent`, `tooltipForCustomModel`, `tooltipForItem`) bleiben in diesem Task bestehen. `PageGazeService` nutzt sie noch, sie fallen erst in Task 4 weg.

- [ ] **Step 1: Write the failing test**

In `PageNoteTest` anhängen:

```java
    @Test
    void testPrebuiltWorldComponentIsCentered(Env ignored) {
        for (PageNote note : PageNote.values()) {
            Component world = note.getWorldComponent();
            assertNotNull(world, "Pre-built world component must not be null");
            assertTrue(PLAIN.serialize(world).startsWith(TooltipBox.TOP_LEFT),
                    "World component of " + note + " must be centered, without leading space");
        }
    }

    @Test
    void testWorldComponentForItem(Env ignored) {
        ItemStack item = ItemStack.builder(Material.PAPER)
                .set(DataComponents.ITEM_MODEL, Key.key("cygnus", "page_3").asString())
                .build();
        assertEquals(Optional.of(PageNote.CANT_RUN.getWorldComponent()), PageNote.worldComponentForItem(item));
        assertTrue(PageNote.worldComponentForItem(ItemStack.of(Material.PAPER)).isEmpty());
        assertTrue(PageNote.worldComponentForItem(null).isEmpty());
    }
```

Fehlende Imports ergänzen: `net.onelitefeather.cygnus.common.ui.TooltipBox` und, falls nicht vorhanden, `static org.junit.jupiter.api.Assertions.assertEquals`.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :common:test --tests "net.onelitefeather.cygnus.common.page.PageNoteTest"`
Expected: FAIL, Kompilierfehler `cannot find symbol: method getWorldComponent()`.

- [ ] **Step 3: Write minimal implementation**

In `PageNote` ein Feld und dessen Initialisierung ergänzen:

```java
    private final Component tooltipComponent;
    private final Component worldComponent;

    PageNote(int modelId, String defaultText) {
        this.modelId = modelId;
        String resolvedText = Holder.LOADED_TEXTS.getOrDefault(modelId, defaultText);
        this.text = resolvedText;
        this.component = Component.text(resolvedText, NamedTextColor.GRAY, TextDecoration.ITALIC);
        this.tooltipComponent = TooltipBox.of(this.component);
        this.worldComponent = TooltipBox.builder().centered().line(this.component).build();
    }
```

Nach `getTooltipComponent()` einfügen:

```java
    /**
     * Returns the pre-built note box for a text display in the world, centered on its anchor.
     *
     * @return the centered note box component
     */
    public Component getWorldComponent() {
        return worldComponent;
    }
```

Nach `tooltipForItem(ItemStack)` einfügen:

```java
    /**
     * Resolves the pre-built world note box for the given {@link ItemStack} if it represents a custom page model.
     *
     * @param itemStack the item stack to resolve the note for
     * @return an {@link Optional} containing the centered note box, or empty if not a custom page model item
     */
    public static Optional<Component> worldComponentForItem(@Nullable ItemStack itemStack) {
        return fromItem(itemStack).map(PageNote::getWorldComponent);
    }
```

`@version` von `PageNote` auf `1.1.0` erhöhen.

In `PageEntity` die Sichtbarkeit von `getResource()` ändern:

```java
    /**
     * Returns the {@link PageResource} the page currently stands on.
     *
     * @return the resource
     */
    public PageResource getResource() {
        return this.resource;
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :common:test --tests "net.onelitefeather.cygnus.common.page.PageNoteTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add common/src/main/java/net/onelitefeather/cygnus/common/page/PageNote.java common/src/main/java/net/onelitefeather/cygnus/common/page/PageEntity.java common/src/test/java/net/onelitefeather/cygnus/common/page/PageNoteTest.java
git commit -m "feat(page): pre-build centered world note component"
```

---

### Task 3: `PageNoteDisplay`

**Files:**
- Create: `game/src/main/java/net/onelitefeather/cygnus/page/PageNoteDisplay.java`
- Test: `game/src/test/java/net/onelitefeather/cygnus/page/PageNoteDisplayTest.java`

**Interfaces:**
- Consumes: `PageEntity#getResource()` und `PageNote#getWorldComponent()` aus Task 2.
- Produces (Paket `net.onelitefeather.cygnus.page`):
  - `static PageNoteDisplay spawn(Player viewer, PageEntity page, Component note)`
  - `void hide()`: blendet über `HIDE_TICKS` aus und entfernt danach. Mehrfacher Aufruf ist harmlos.
  - `void removeNow()`: entfernt sofort. Mehrfacher Aufruf ist harmlos, auch nach `hide()`.
  - `UUID pageId()`: UUID der `PageEntity` (`page.getUuid()`).
  - `Pos pagePosition()`: Position der Page beim Spawnen.
  - `Entity entity()` (package-private, für Tests).
  - Konstanten (package-private): `float NOTE_SCALE = 0.35f`, `int SHOW_TICKS = 4`, `int HIDE_TICKS = 3`, `double FACE_OFFSET = 0.15`, `double LIFT = 0.45`, `int LINE_WIDTH = 1000`, `int NOTE_LIGHT = 15`.

- [ ] **Step 1: Write the failing test**

```java
package net.onelitefeather.cygnus.page;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.utils.Direction;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.common.page.PageEntity;
import net.onelitefeather.cygnus.common.page.PageFactory;
import net.onelitefeather.cygnus.common.page.PageNote;
import net.onelitefeather.cygnus.common.page.PageResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MicrotusExtension.class)
class PageNoteDisplayTest {

    private static PageEntity placePage(Instance instance) {
        PageEntity page = PageFactory.createPage(new PageResource(new Pos(0, 40, 2), Direction.NORTH), 1);
        page.place(instance).join();
        return page;
    }

    @Test
    void spawnsTextDisplayVisibleOnlyToTheReader(Env env) {
        Instance instance = env.createFlatInstance();
        Player reader = env.createPlayer(instance, new Pos(0, 40, 0));
        Player other = env.createPlayer(instance, new Pos(1, 40, 0));
        PageEntity page = placePage(instance);

        PageNoteDisplay display = PageNoteDisplay.spawn(reader, page, PageNote.HELP_ME.getWorldComponent());

        assertEquals(EntityType.TEXT_DISPLAY, display.entity().getEntityType());
        assertTrue(display.entity().getViewers().contains(reader), "the reader must see the note");
        assertFalse(display.entity().getViewers().contains(other), "nobody else may see the note");
        assertEquals(page.getUuid(), display.pageId());
        assertEquals(page.getPosition(), display.pagePosition());
    }

    @Test
    void appliesTheAgreedMetadata(Env env) {
        Instance instance = env.createFlatInstance();
        Player reader = env.createPlayer(instance, new Pos(0, 40, 0));
        PageEntity page = placePage(instance);

        PageNoteDisplay display = PageNoteDisplay.spawn(reader, page, PageNote.HELP_ME.getWorldComponent());
        TextDisplayMeta meta = (TextDisplayMeta) display.entity().getEntityMeta();

        assertEquals(PageNote.HELP_ME.getWorldComponent(), meta.getText());
        assertEquals(0, meta.getBackgroundColor());
        assertFalse(meta.isShadow());
        assertFalse(meta.isSeeThrough());
        assertEquals(PageNoteDisplay.LINE_WIDTH, meta.getLineWidth());
        assertEquals(AbstractDisplayMeta.BillboardConstraints.CENTER, meta.getBillboardRenderConstraints());
        assertEquals(Vec.ZERO, meta.getScale(), "a note spawns collapsed and grows in on the next tick");
    }

    @Test
    void floatsInFrontOfAndAboveThePage(Env env) {
        Instance instance = env.createFlatInstance();
        Player reader = env.createPlayer(instance, new Pos(0, 40, 0));
        PageEntity page = placePage(instance);

        PageNoteDisplay display = PageNoteDisplay.spawn(reader, page, PageNote.HELP_ME.getWorldComponent());

        // NORTH faces -Z, so "in front" is towards -Z
        Pos expected = page.getPosition().add(0, PageNoteDisplay.LIFT, -PageNoteDisplay.FACE_OFFSET);
        assertTrue(expected.samePoint(display.entity().getPosition()),
                "expected " + expected + " but was " + display.entity().getPosition());
    }

    @Test
    void growsToNoteScaleOnTheNextTick(Env env) {
        Instance instance = env.createFlatInstance();
        Player reader = env.createPlayer(instance, new Pos(0, 40, 0));
        PageEntity page = placePage(instance);

        PageNoteDisplay display = PageNoteDisplay.spawn(reader, page, PageNote.HELP_ME.getWorldComponent());
        TextDisplayMeta meta = (TextDisplayMeta) display.entity().getEntityMeta();
        env.tick();

        assertEquals(new Vec(PageNoteDisplay.NOTE_SCALE), meta.getScale());
        assertEquals(PageNoteDisplay.SHOW_TICKS, meta.getTransformationInterpolationDuration());
    }

    @Test
    void hideShrinksThenRemoves(Env env) {
        Instance instance = env.createFlatInstance();
        Player reader = env.createPlayer(instance, new Pos(0, 40, 0));
        PageEntity page = placePage(instance);
        PageNoteDisplay display = PageNoteDisplay.spawn(reader, page, PageNote.HELP_ME.getWorldComponent());
        env.tick();

        display.hide();
        TextDisplayMeta meta = (TextDisplayMeta) display.entity().getEntityMeta();
        assertEquals(Vec.ZERO, meta.getScale());
        assertEquals(PageNoteDisplay.HIDE_TICKS, meta.getTransformationInterpolationDuration());
        assertFalse(display.entity().isRemoved(), "the fade-out must play before removal");

        env.tickWhile(() -> !display.entity().isRemoved(), Duration.ofSeconds(2));
        assertTrue(display.entity().isRemoved());
    }

    @Test
    void removeNowIsImmediateAndIdempotent(Env env) {
        Instance instance = env.createFlatInstance();
        Player reader = env.createPlayer(instance, new Pos(0, 40, 0));
        PageEntity page = placePage(instance);
        PageNoteDisplay display = PageNoteDisplay.spawn(reader, page, PageNote.HELP_ME.getWorldComponent());

        display.hide();
        display.removeNow();
        display.removeNow();
        display.hide();

        assertTrue(display.entity().isRemoved());
        env.tick();
        env.tick();
        env.tick();
        env.tick();
        assertTrue(display.entity().isRemoved());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :game:test --tests "net.onelitefeather.cygnus.page.PageNoteDisplayTest"`
Expected: FAIL, Kompilierfehler `cannot find symbol: class PageNoteDisplay`.

- [ ] **Step 3: Write minimal implementation**

```java
package net.onelitefeather.cygnus.page;

import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.utils.Direction;
import net.onelitefeather.cygnus.common.page.PageEntity;

import java.util.UUID;

/**
 * One handwritten note floating at one page, shown to exactly one player.
 *
 * <p>The note is a text display rather than a title: it stays where the page is, does not share the
 * title slot with anything else, and display entities have no hit box, so clicks still reach the
 * page's interaction entity right behind it.</p>
 *
 * <p>It fades by scale, since the client interpolates a display's transformation but not its text
 * opacity.</p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class PageNoteDisplay {

    static final float NOTE_SCALE = 0.35f;
    static final int SHOW_TICKS = 4;
    static final int HIDE_TICKS = 3;
    static final double FACE_OFFSET = 0.15;
    static final double LIFT = 0.45;
    static final int LINE_WIDTH = 1000;
    static final int NOTE_LIGHT = 15;

    private final Entity entity;
    private final UUID pageId;
    private final Pos pagePosition;
    private boolean hiding;

    private PageNoteDisplay(Entity entity, UUID pageId, Pos pagePosition) {
        this.entity = entity;
        this.pageId = pageId;
        this.pagePosition = pagePosition;
    }

    /**
     * Spawns the note at the given page for the given player and starts growing it in.
     *
     * @param viewer the only player who will see the note
     * @param page   the page the note belongs to
     * @param note   the note box, see {@code PageNote#getWorldComponent()}
     * @return the spawned note
     */
    public static PageNoteDisplay spawn(Player viewer, PageEntity page, Component note) {
        Entity entity = new Entity(EntityType.TEXT_DISPLAY);
        entity.setNoGravity(true);
        entity.setAutoViewable(false);

        TextDisplayMeta meta = (TextDisplayMeta) entity.getEntityMeta();
        meta.setText(note);
        meta.setBackgroundColor(0);
        meta.setShadow(false);
        meta.setSeeThrough(false);
        meta.setLineWidth(LINE_WIDTH);
        meta.setBillboardRenderConstraints(AbstractDisplayMeta.BillboardConstraints.CENTER);
        meta.setBrightness(NOTE_LIGHT, NOTE_LIGHT);
        meta.setScale(Vec.ZERO);

        Pos pagePosition = page.getPosition();
        Direction face = page.getResource().face();
        Pos position = pagePosition.add(
                face.normalX() * FACE_OFFSET,
                LIFT + face.normalY() * FACE_OFFSET,
                face.normalZ() * FACE_OFFSET
        );
        entity.setInstance(page.getInstance(), position).join();
        entity.addViewer(viewer);

        // A tick between spawn and target scale: set in the same tick, the client would take the
        // target without interpolating towards it.
        entity.scheduleNextTick(ignored -> {
            meta.setTransformationInterpolationStartDelta(0);
            meta.setTransformationInterpolationDuration(SHOW_TICKS);
            meta.setScale(new Vec(NOTE_SCALE));
        });
        return new PageNoteDisplay(entity, page.getUuid(), pagePosition);
    }

    /**
     * Shrinks the note away and removes it once the shrink has played.
     */
    public void hide() {
        if (this.hiding || this.entity.isRemoved()) {
            return;
        }
        this.hiding = true;
        TextDisplayMeta meta = (TextDisplayMeta) this.entity.getEntityMeta();
        meta.setTransformationInterpolationStartDelta(0);
        meta.setTransformationInterpolationDuration(HIDE_TICKS);
        meta.setScale(Vec.ZERO);
        this.entity.scheduler().buildTask(this::removeNow).delay(TaskSchedule.tick(HIDE_TICKS)).schedule();
    }

    /**
     * Removes the note at once, without any animation.
     */
    public void removeNow() {
        if (!this.entity.isRemoved()) {
            this.entity.remove();
        }
    }

    /**
     * Returns the id of the page this note belongs to.
     *
     * @return the page id
     */
    public UUID pageId() {
        return this.pageId;
    }

    /**
     * Returns where the page stood when the note was spawned.
     *
     * @return the page position at spawn time
     */
    public Pos pagePosition() {
        return this.pagePosition;
    }

    Entity entity() {
        return this.entity;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :game:test --tests "net.onelitefeather.cygnus.page.PageNoteDisplayTest"`
Expected: PASS.

Fallen `getViewers()`-Assertions durch, weil `addViewer` erst nach dem Laden des Chunks greift: Das `.join()` auf `setInstance` muss vor `addViewer` stehen, wie oben. Nicht durch `thenRun` ersetzen.

- [ ] **Step 5: Commit**

```bash
git add game/src/main/java/net/onelitefeather/cygnus/page/PageNoteDisplay.java game/src/test/java/net/onelitefeather/cygnus/page/PageNoteDisplayTest.java
git commit -m "feat(page): add PageNoteDisplay for in-world notes"
```

---

### Task 4: `PageGazeService` auf Displays mit Verweilen, Hysterese und Nachlauf umstellen

**Files:**
- Modify: `game/src/main/java/net/onelitefeather/cygnus/page/PageGazeService.java` (vollständig ersetzen, siehe unten)
- Modify: `common/src/main/java/net/onelitefeather/cygnus/common/page/PageNote.java` (Titel-Methoden entfernen)
- Modify: `common/src/test/java/net/onelitefeather/cygnus/common/page/PageNoteTest.java` (`testPrebuiltTooltipComponent` entfernen)
- Modify: `game/src/test/java/net/onelitefeather/cygnus/listener/page/PlayerPageInteractListenerTest.java` (`testPagePickupClearsGaze` umschreiben)
- Test: `game/src/test/java/net/onelitefeather/cygnus/page/PageGazeServiceTest.java` (vollständig ersetzen)

**Interfaces:**
- Consumes: `PageNoteDisplay` (Task 3), `PageNote.worldComponentForItem(ItemStack)` (Task 2).
- Produces, unverändert öffentlich: `startTask()`, `stopTask()`, `isRunning()`, `clearPlayer(Player)`, `isGazing(Player)`, `@Nullable UUID activeGaze(Player)`, `tick()`.
- Neu öffentlich: `STAY_GAZE_COSINE = 0.70D`, `STAY_GAZE_DISTANCE = 5.5D`, `DWELL_TICKS = 6`, `LINGER_TICKS = 10`.
- Neu package-private: `@Nullable PageNoteDisplay shownNote(Player)`.
- Entfällt: `TITLE_TIMES`, `PageNote#getTooltipComponent()`, `PageNote.tooltipForCustomModel(int)`, `PageNote.tooltipForItem(ItemStack)`.

Takt der Tests: Jeder Aufruf von `tick()` zählt `UPDATE_INTERVAL_TICKS = 2` Ticks. Eine Notiz erscheint daher beim **3.** aufeinanderfolgenden Treffer (`DWELL_TICKS = 6`). Nach dem Wegschauen verschwindet sie beim **6.** aufeinanderfolgenden Fehlschlag. Fehlschlag 1 setzt den Startpunkt, ab Fehlschlag 6 gilt `clock - outOfRangeSince = 10 ≥ LINGER_TICKS`.

- [ ] **Step 1: Write the failing tests**

`PageGazeServiceTest.java` vollständig ersetzen:

```java
package net.onelitefeather.cygnus.page;

import net.kyori.adventure.key.Key;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.SetTitleSubTitlePacket;
import net.minestom.server.utils.Direction;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.common.page.PageEntity;
import net.onelitefeather.cygnus.common.page.PageFactory;
import net.onelitefeather.cygnus.common.page.PageProvider;
import net.onelitefeather.cygnus.common.page.PageResource;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MicrotusExtension.class)
class PageGazeServiceTest {

    /** Ticks until a steady look shows the note: the third consecutive hit. */
    private static final int TICKS_TO_SHOW = 3;
    /** Consecutive misses that keep the note up: the sixth one takes it down. */
    private static final int MISSES_KEPT = 5;

    private static ItemStack pageItem(int modelId) {
        return ItemStack.builder(Material.PAPER)
                .set(DataComponents.ITEM_MODEL, Key.key("cygnus", "page_" + modelId).asString())
                .build();
    }

    /**
     * Places a page so that its centre is at the player's eye height, {@code distance} blocks in
     * front of a player standing at the origin. A NORTH page sits at +0.5/+0.5/+1.0 of its block.
     */
    private static PageEntity placePage(Instance instance, Player player, double distance, ItemStack item) {
        Pos block = new Pos(-0.5, player.getEyeHeight() - 0.5, distance - 1.0);
        PageEntity page = PageFactory.createPage(new PageResource(block, Direction.NORTH), 1);
        page.place(instance).join();
        try {
            var field = PageEntity.class.getDeclaredField("pageItem");
            field.setAccessible(true);
            field.set(page, item);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return page;
    }

    /** Points the player at the page, turned away by {@code yawOffset} degrees. */
    private static void aim(Player player, PageEntity page, float yawOffset) {
        Pos eye = player.getPosition().add(0, player.getEyeHeight(), 0);
        Vec toPage = Vec.fromPoint(page.getPosition().sub(eye));
        Pos look = Pos.ZERO.withDirection(toPage);
        player.teleport(player.getPosition().withView(look.yaw() + yawOffset, look.pitch())).join();
    }

    private static void ticks(PageGazeService service, int count) {
        for (int i = 0; i < count; i++) {
            service.tick();
        }
    }

    private static long textDisplays(Instance instance) {
        return instance.getEntities().stream().filter(e -> e.getEntityType() == EntityType.TEXT_DISPLAY).count();
    }

    @Test
    void steadyLookShowsNoteOnlyAfterDwell(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, Pos.ZERO);
        PageEntity page = placePage(instance, player, 2.0, pageItem(1));
        aim(player, page, 0);
        PageGazeService service = new PageGazeService(() -> List.of(player), () -> List.of(page));

        ticks(service, TICKS_TO_SHOW - 1);
        assertFalse(service.isGazing(player), "a glance shorter than the dwell must not show the note");
        assertEquals(0, textDisplays(instance));

        service.tick();
        assertTrue(service.isGazing(player));
        assertEquals(page.getUuid(), service.activeGaze(player));
        assertEquals(1, textDisplays(instance));
        assertTrue(service.shownNote(player).entity().getViewers().contains(player));
    }

    @Test
    void interruptedDwellStartsOver(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, Pos.ZERO);
        PageEntity page = placePage(instance, player, 2.0, pageItem(1));
        PageGazeService service = new PageGazeService(() -> List.of(player), () -> List.of(page));

        aim(player, page, 0);
        ticks(service, TICKS_TO_SHOW - 1);
        aim(player, page, 90);
        service.tick();
        aim(player, page, 0);
        ticks(service, TICKS_TO_SHOW - 1);

        assertFalse(service.isGazing(player), "looking away must reset the dwell");
        service.tick();
        assertTrue(service.isGazing(player));
    }

    @Test
    void noTitleIsSentAnymore(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, Pos.ZERO);
        PageEntity page = placePage(instance, player, 2.0, pageItem(1));
        aim(player, page, 0);
        PageGazeService service = new PageGazeService(() -> List.of(player), () -> List.of(page));

        Collector<SetTitleSubTitlePacket> subtitles = connection.trackIncoming(SetTitleSubTitlePacket.class);
        ticks(service, TICKS_TO_SHOW);

        assertTrue(service.isGazing(player));
        assertTrue(subtitles.collect().isEmpty());
    }

    @Test
    void wobblingInsideStayConeKeepsTheSameNote(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, Pos.ZERO);
        PageEntity page = placePage(instance, player, 2.0, pageItem(2));
        PageGazeService service = new PageGazeService(() -> List.of(player), () -> List.of(page));
        aim(player, page, 0);
        ticks(service, TICKS_TO_SHOW);
        PageNoteDisplay shown = service.shownNote(player);
        assertNotNull(shown);

        // cos(40°) ≈ 0.77: below the enter threshold, above the stay threshold
        for (int i = 0; i < 10; i++) {
            aim(player, page, i % 2 == 0 ? 40 : 20);
            service.tick();
            assertTrue(service.isGazing(player), "the note must not flicker at the cone edge");
        }
        assertEquals(shown, service.shownNote(player), "no new display may be spawned while wobbling");
        assertEquals(1, textDisplays(instance));
    }

    @Test
    void stayConeAloneDoesNotShowANote(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, Pos.ZERO);
        PageEntity page = placePage(instance, player, 2.0, pageItem(2));
        PageGazeService service = new PageGazeService(() -> List.of(player), () -> List.of(page));
        aim(player, page, 40);

        ticks(service, TICKS_TO_SHOW * 3);

        assertFalse(service.isGazing(player), "only the enter cone may start a note");
    }

    @Test
    void lookingAwayLingersThenHides(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, Pos.ZERO);
        PageEntity page = placePage(instance, player, 2.0, pageItem(3));
        PageGazeService service = new PageGazeService(() -> List.of(player), () -> List.of(page));
        aim(player, page, 0);
        ticks(service, TICKS_TO_SHOW);
        Entity entity = service.shownNote(player).entity();

        aim(player, page, 90);
        ticks(service, MISSES_KEPT);
        assertTrue(service.isGazing(player), "the note lingers for a moment after looking away");

        service.tick();
        assertFalse(service.isGazing(player));
        assertNull(service.activeGaze(player));
        env.tickWhile(() -> !entity.isRemoved(), Duration.ofSeconds(2));
        assertTrue(entity.isRemoved());
    }

    @Test
    void lookingBackDuringLingerKeepsTheNote(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, Pos.ZERO);
        PageEntity page = placePage(instance, player, 2.0, pageItem(3));
        PageGazeService service = new PageGazeService(() -> List.of(player), () -> List.of(page));
        aim(player, page, 0);
        ticks(service, TICKS_TO_SHOW);
        PageNoteDisplay shown = service.shownNote(player);

        aim(player, page, 90);
        ticks(service, MISSES_KEPT);
        aim(player, page, 0);
        ticks(service, MISSES_KEPT + 2);

        assertEquals(shown, service.shownNote(player));
    }

    @Test
    void walkingBeyondStayDistanceHidesAfterLinger(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, Pos.ZERO);
        PageEntity page = placePage(instance, player, 2.0, pageItem(4));
        PageGazeService service = new PageGazeService(() -> List.of(player), () -> List.of(page));
        aim(player, page, 0);
        ticks(service, TICKS_TO_SHOW);

        // 3 blocks back: about 5 blocks away, inside the stay distance
        player.teleport(player.getPosition().withZ(-3)).join();
        aim(player, page, 0);
        ticks(service, MISSES_KEPT + 2);
        assertTrue(service.isGazing(player), "5 blocks is still inside the stay distance");

        // 5 blocks back: about 7 blocks away
        player.teleport(player.getPosition().withZ(-5)).join();
        aim(player, page, 0);
        ticks(service, MISSES_KEPT + 1);
        assertFalse(service.isGazing(player));
    }

    @Test
    void pageBeyondEnterDistanceDoesNotShow(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, Pos.ZERO);
        PageEntity page = placePage(instance, player, 4.5, pageItem(3));
        aim(player, page, 0);
        PageGazeService service = new PageGazeService(() -> List.of(player), () -> List.of(page));

        ticks(service, TICKS_TO_SHOW * 2);

        assertFalse(service.isGazing(player));
        assertEquals(0, textDisplays(instance));
    }

    @Test
    void nonInteractablePageRemovesNoteImmediately(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, Pos.ZERO);
        PageEntity page = placePage(instance, player, 2.0, pageItem(4));
        aim(player, page, 0);
        PageGazeService service = new PageGazeService(() -> List.of(player), () -> List.of(page));
        ticks(service, TICKS_TO_SHOW);
        Entity entity = service.shownNote(player).entity();

        page.disableInteraction();
        service.tick();

        assertFalse(service.isGazing(player));
        assertTrue(entity.isRemoved(), "an expired page takes its note with it at once");
    }

    @Test
    void relocatedPageRemovesNoteImmediately(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, Pos.ZERO);
        PageEntity page = placePage(instance, player, 2.0, pageItem(4));
        aim(player, page, 0);
        PageGazeService service = new PageGazeService(() -> List.of(player), () -> List.of(page));
        ticks(service, TICKS_TO_SHOW);
        Entity entity = service.shownNote(player).entity();

        // Moved a hair, so it is still in the cone: only the position check can catch it
        page.teleport(page.getPosition().add(0, 0, 0.01)).join();
        service.tick();

        assertTrue(entity.isRemoved(), "a moved page must not leave its note at the old spot");
        assertFalse(service.isGazing(player));
    }

    @Test
    void pageLeavingSupplierRemovesNoteImmediately(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, Pos.ZERO);
        PageEntity page = placePage(instance, player, 2.0, pageItem(5));
        aim(player, page, 0);
        List<PageEntity> pages = new ArrayList<>(List.of(page));
        PageGazeService service = new PageGazeService(() -> List.of(player), () -> pages);
        ticks(service, TICKS_TO_SHOW);
        Entity entity = service.shownNote(player).entity();

        pages.clear();
        service.tick();

        assertFalse(service.isGazing(player));
        assertTrue(entity.isRemoved());
    }

    @Test
    void pageWithoutNoteNeverShowsDisplay(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, Pos.ZERO);
        PageEntity page = placePage(instance, player, 2.0, ItemStack.of(Material.PAPER));
        aim(player, page, 0);
        PageGazeService service = new PageGazeService(() -> List.of(player), () -> List.of(page));

        ticks(service, TICKS_TO_SHOW * 5);

        assertFalse(service.isGazing(player));
        assertEquals(0, textDisplays(instance));
    }

    @Test
    void twoReadersGetSeparateDisplays(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player first = env.createPlayer(instance, Pos.ZERO);
        Player second = env.createPlayer(instance, Pos.ZERO);
        PageEntity page = placePage(instance, first, 2.0, pageItem(6));
        aim(first, page, 0);
        aim(second, page, 0);
        PageGazeService service = new PageGazeService(() -> List.of(first, second), () -> List.of(page));
        ticks(service, TICKS_TO_SHOW);

        PageNoteDisplay a = service.shownNote(first);
        PageNoteDisplay b = service.shownNote(second);
        assertNotSame(a, b);
        assertEquals(Set.of(first), a.entity().getViewers());
        assertEquals(Set.of(second), b.entity().getViewers());

        aim(first, page, 90);
        ticks(service, MISSES_KEPT + 1);
        assertFalse(service.isGazing(first));
        assertTrue(service.isGazing(second), "one reader looking away must not close the other's note");
    }

    @Test
    void switchingToAnotherPageReplacesTheNoteAfterDwell(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, Pos.ZERO);
        PageEntity left = placePage(instance, player, 2.0, pageItem(1));
        PageEntity right = PageFactory.createPage(
                new PageResource(new Pos(-3.5, player.getEyeHeight() - 0.5, 1.0), Direction.NORTH), 1);
        right.place(instance).join();
        PageGazeService service = new PageGazeService(() -> List.of(player), () -> List.of(left, right));
        aim(player, left, 0);
        ticks(service, TICKS_TO_SHOW);
        Entity first = service.shownNote(player).entity();

        aim(player, right, 0);
        ticks(service, TICKS_TO_SHOW - 1);
        assertEquals(left.getUuid(), service.activeGaze(player), "the old note stays until the new dwell is done");

        service.tick();
        assertEquals(right.getUuid(), service.activeGaze(player));
        assertTrue(first.isRemoved(), "the old note goes at once when the new one appears");
    }

    @Test
    void survivorLeavingRemovesNoteImmediately(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, Pos.ZERO);
        PageEntity page = placePage(instance, player, 2.0, pageItem(1));
        aim(player, page, 0);
        List<Player> survivors = new ArrayList<>(List.of(player));
        PageGazeService service = new PageGazeService(() -> survivors, () -> List.of(page));
        ticks(service, TICKS_TO_SHOW);
        Entity entity = service.shownNote(player).entity();

        survivors.clear();
        service.tick();

        assertFalse(service.isGazing(player));
        assertTrue(entity.isRemoved());
    }

    @Test
    void clearPlayerRemovesNoteImmediately(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, Pos.ZERO);
        PageEntity page = placePage(instance, player, 2.0, pageItem(5));
        aim(player, page, 0);
        PageGazeService service = new PageGazeService(() -> List.of(player), () -> List.of(page));
        ticks(service, TICKS_TO_SHOW);
        Entity entity = service.shownNote(player).entity();

        service.clearPlayer(player);

        assertFalse(service.isGazing(player));
        assertNull(service.activeGaze(player));
        assertTrue(entity.isRemoved());
    }

    @Test
    void stopTaskRemovesAllNotes(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, Pos.ZERO);
        PageEntity page = placePage(instance, player, 2.0, pageItem(6));
        aim(player, page, 0);
        PageGazeService service = new PageGazeService(() -> List.of(player), () -> List.of(page));
        service.startTask();
        assertTrue(service.isRunning());
        ticks(service, TICKS_TO_SHOW);
        Entity entity = service.shownNote(player).entity();

        service.stopTask();

        assertFalse(service.isRunning());
        assertFalse(service.isGazing(player));
        assertTrue(entity.isRemoved());
    }

    @Test
    void stopTaskDuringFadeOutLeavesNoDisplay(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, Pos.ZERO);
        PageEntity page = placePage(instance, player, 2.0, pageItem(6));
        aim(player, page, 0);
        PageGazeService service = new PageGazeService(() -> List.of(player), () -> List.of(page));
        ticks(service, TICKS_TO_SHOW);
        PageNoteDisplay shown = service.shownNote(player);

        aim(player, page, 90);
        ticks(service, MISSES_KEPT + 1); // fade-out has started
        service.stopTask();
        for (int i = 0; i < PageNoteDisplay.HIDE_TICKS + 2; i++) {
            env.tick();
        }

        assertTrue(shown.entity().isRemoved());
        assertEquals(0, textDisplays(instance));
    }

    @Test
    void constructorWithPageProvider(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance, Pos.ZERO);

        PageProvider pageProvider = new PageProvider();
        Set<PageResource> resources = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            resources.add(new PageResource(new Pos(i, player.getEyeHeight(), 2.0), Direction.NORTH));
        }
        pageProvider.loadPageData(resources);
        pageProvider.collectStartPages(resources.size());
        pageProvider.spawn(instance);

        PageGazeService service = new PageGazeService(() -> List.of(player), pageProvider);
        service.tick();

        assertFalse(pageProvider.interactablePages().isEmpty());
    }
}
```

In `PlayerPageInteractListenerTest` den Test `testPagePickupClearsGaze` vollständig ersetzen. Der Reflection-Zugriff auf das Feld `activeGaze` funktioniert nicht mehr, weil das Feld wegfällt:

```java
    @Test
    void testPagePickupClearsGaze(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        CygnusPlayer player = (CygnusPlayer) env.createPlayer(instance);
        player.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);

        PageProvider pageProvider = new PageProvider();
        pageProvider.loadPageData(Set.of(new PageResource(Pos.ZERO, Direction.NORTH)));
        pageProvider.setMaxPageAmount(1);

        PageEntity pageEntity = PageFactory.createPage(new PageResource(Pos.ZERO, Direction.NORTH), 1);
        pageEntity.place(instance).join();
        UUID hitBoxUuid = pageEntity.getHitBoxUUID();
        seedActivePage(pageProvider, pageEntity);

        // The page sits at 0.5 / 0.5 / 1.0: stand two blocks in front of it at eye height, facing +Z
        player.teleport(new Pos(0.5, 0.5 - player.getEyeHeight(), -1.0, 0, 0)).join();
        PageGazeService gazeService = new PageGazeService(() -> List.of(player), () -> List.of(pageEntity));
        for (int i = 0; i < 3; i++) {
            gazeService.tick();
        }
        assertTrue(gazeService.isGazing(player), "Player must initially be reading the note");

        Entity target = new Entity(EntityType.INTERACTION);
        target.setTag(Tags.PAGE_TAG, hitBoxUuid);

        PlayerPageInteractListener listener = new PlayerPageInteractListener(pageProvider, gazeService);
        listener.accept(new PlayerEntityInteractEvent(player, target, PlayerHand.MAIN, Vec.ZERO));

        assertEquals(1, player.getPageFounds(), "Counter must increment upon successfully finding a page");
        assertFalse(gazeService.isGazing(player), "The note must close when the player picks up the page");
        assertTrue(instance.getEntities().stream().noneMatch(e -> e.getEntityType() == EntityType.TEXT_DISPLAY),
                "No note display may be left behind after the pickup");

        env.destroyInstance(instance, true);
    }
```

Danach die nicht mehr genutzten Imports in `PlayerPageInteractListenerTest` entfernen (`java.lang.reflect.Field`, `java.util.Map`, `net.minestom.server.entity.Player`, falls nur hier genutzt).

In `PageNoteTest` die Methode `testPrebuiltTooltipComponent` löschen.

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :game:test --tests "net.onelitefeather.cygnus.page.PageGazeServiceTest"`
Expected: FAIL, Kompilierfehler `cannot find symbol: method shownNote(Player)`.

- [ ] **Step 3: Write the implementation**

`PageGazeService.java` vollständig ersetzen:

```java
package net.onelitefeather.cygnus.page;

import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.time.TimeUnit;
import net.onelitefeather.cygnus.common.page.PageEntity;
import net.onelitefeather.cygnus.common.page.PageNote;
import net.onelitefeather.cygnus.common.page.PageProvider;
import net.onelitefeather.cygnus.utils.RepeatingTask;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Detects survivors looking at collectible pages and floats the page's handwritten note next to
 * it, for that survivor only.
 *
 * <p>Reading is meant to feel calm, so the note does not follow the gaze cone one to one. It only
 * appears after the survivor held the page in view for {@link #DWELL_TICKS}, so a glance while
 * running past shows nothing. Once up, it stays inside a wider cone and range than the one that
 * opened it, so a slight wobble does not make it flicker, and it lingers for {@link #LINGER_TICKS}
 * after the survivor looked away. A page that expires, moves or is picked up takes its note with it
 * at once.</p>
 *
 * @author theEvilReaper
 * @version 2.0.0
 * @since 2.15.0
 */
public final class PageGazeService {

    /**
     * Maximum distance in blocks at which looking at a page starts showing its note.
     */
    public static final double MAX_GAZE_DISTANCE = 4.0D;

    /**
     * Minimum cosine between the view vector and the page direction to start showing a note (~31°).
     */
    public static final double MIN_GAZE_COSINE = 0.85D;

    /**
     * Maximum distance in blocks at which a note that is already up stays up.
     */
    public static final double STAY_GAZE_DISTANCE = 5.5D;

    /**
     * Minimum cosine at which a note that is already up stays up (~46°).
     */
    public static final double STAY_GAZE_COSINE = 0.70D;

    /**
     * How long, in ticks, a page has to stay in view before its note appears.
     */
    public static final int DWELL_TICKS = 6;

    /**
     * How long, in ticks, a note stays up after its page left the stay cone.
     */
    public static final int LINGER_TICKS = 10;

    private static final int UPDATE_INTERVAL_TICKS = 2;
    private static final double DISTANCE_EPSILON = 1.0E-6D;

    private final RepeatingTask task = new RepeatingTask(this::tick);
    private final Supplier<Collection<Player>> survivorSupplier;
    private final Supplier<Collection<PageEntity>> pageSupplier;
    private final Map<Player, GazeState> states = new ConcurrentHashMap<>();
    private long clock;

    /**
     * Constructs a new {@link PageGazeService}.
     *
     * @param survivorSupplier supplies the survivors to monitor
     * @param pageSupplier     supplies the collectible pages
     */
    public PageGazeService(
            Supplier<Collection<Player>> survivorSupplier,
            Supplier<Collection<PageEntity>> pageSupplier
    ) {
        this.survivorSupplier = survivorSupplier;
        this.pageSupplier = pageSupplier;
    }

    /**
     * Constructs a new {@link PageGazeService} using a {@link PageProvider}.
     *
     * @param survivorSupplier supplies the survivors to monitor
     * @param pageProvider     the page provider supplying interactable pages
     */
    public PageGazeService(
            Supplier<Collection<Player>> survivorSupplier,
            PageProvider pageProvider
    ) {
        this(survivorSupplier, pageProvider::interactablePages);
    }

    /**
     * Starts the periodic gaze check task. Does nothing if already running.
     */
    public void startTask() {
        this.task.start(UPDATE_INTERVAL_TICKS, TimeUnit.SERVER_TICK);
    }

    /**
     * Stops the periodic gaze check task and removes every note at once.
     */
    public void stopTask() {
        this.task.stop();
        this.clearAll();
    }

    /**
     * Returns whether the gaze check task is currently running.
     *
     * @return {@code true} if running
     */
    public boolean isRunning() {
        return this.task.isRunning();
    }

    /**
     * Removes the note of the given player at once and forgets their gaze.
     *
     * @param player the player to clear
     */
    public void clearPlayer(Player player) {
        GazeState state = this.states.remove(player);
        if (state != null && state.shown != null) {
            state.shown.removeNow();
        }
    }

    /**
     * Returns whether the player currently has a note up, including while it lingers.
     *
     * @param player the player to check
     * @return {@code true} if a note is shown
     */
    public boolean isGazing(Player player) {
        return this.shownNote(player) != null;
    }

    /**
     * Returns the UUID of the page whose note the player currently has up, or {@code null} if none.
     *
     * @param player the player to check
     * @return the page UUID or {@code null}
     */
    public @Nullable UUID activeGaze(Player player) {
        PageNoteDisplay shown = this.shownNote(player);
        return shown == null ? null : shown.pageId();
    }

    @Nullable PageNoteDisplay shownNote(Player player) {
        GazeState state = this.states.get(player);
        return state == null ? null : state.shown;
    }

    /**
     * Performs a single gaze check for all supplied survivors. Each call counts as
     * {@value #UPDATE_INTERVAL_TICKS} ticks.
     */
    public void tick() {
        this.clock += UPDATE_INTERVAL_TICKS;
        Collection<Player> survivors = this.survivorSupplier.get();
        if (survivors == null || survivors.isEmpty()) {
            this.clearAll();
            return;
        }
        for (Player player : List.copyOf(this.states.keySet())) {
            if (!survivors.contains(player)) {
                this.clearPlayer(player);
            }
        }

        Collection<PageEntity> pages = this.pageSupplier.get();
        Collection<PageEntity> current = pages == null ? List.of() : pages;
        for (Player player : survivors) {
            this.update(player, current);
        }
    }

    private void update(Player player, Collection<PageEntity> pages) {
        GazeState state = this.states.computeIfAbsent(player, ignored -> new GazeState());

        if (state.shown != null) {
            this.checkShown(player, state, pages);
        }

        PageEntity best = bestPage(player, pages);
        UUID bestId = best == null ? null : best.getUuid();
        if (bestId == null || (state.shown != null && bestId.equals(state.shown.pageId()))) {
            state.candidate = null;
        } else {
            if (!bestId.equals(state.candidate)) {
                state.candidate = bestId;
                state.candidateSince = this.clock;
            }
            if (this.clock - state.candidateSince + UPDATE_INTERVAL_TICKS >= DWELL_TICKS) {
                Optional<Component> note = PageNote.worldComponentForItem(best.getPageItem());
                if (note.isPresent()) {
                    if (state.shown != null) {
                        state.shown.removeNow();
                    }
                    state.shown = PageNoteDisplay.spawn(player, best, note.get());
                    state.outOfRangeSince = -1;
                }
                state.candidate = null;
            }
        }

        if (state.shown == null && state.candidate == null) {
            this.states.remove(player);
        }
    }

    private void checkShown(Player player, GazeState state, Collection<PageEntity> pages) {
        PageEntity page = findPage(pages, state.shown.pageId());
        if (page == null
                || !page.isInteractable()
                || !page.getPosition().samePoint(state.shown.pagePosition())) {
            state.shown.removeNow();
            state.shown = null;
            state.outOfRangeSince = -1;
            return;
        }

        if (inView(player, page, STAY_GAZE_COSINE, STAY_GAZE_DISTANCE)) {
            state.outOfRangeSince = -1;
            return;
        }
        if (state.outOfRangeSince < 0) {
            state.outOfRangeSince = this.clock;
        }
        if (this.clock - state.outOfRangeSince >= LINGER_TICKS) {
            state.shown.hide();
            state.shown = null;
            state.outOfRangeSince = -1;
        }
    }

    private void clearAll() {
        for (GazeState state : this.states.values()) {
            if (state.shown != null) {
                state.shown.removeNow();
            }
        }
        this.states.clear();
    }

    private static @Nullable PageEntity findPage(Collection<PageEntity> pages, UUID pageId) {
        for (PageEntity page : pages) {
            if (page.getUuid().equals(pageId)) {
                return page;
            }
        }
        return null;
    }

    /**
     * Returns the page the player looks at most directly within the enter cone, or {@code null}.
     */
    private static @Nullable PageEntity bestPage(Player player, Collection<PageEntity> pages) {
        PageEntity bestPage = null;
        double bestCosine = MIN_GAZE_COSINE;
        double bestDistance = Double.MAX_VALUE;

        for (PageEntity page : pages) {
            if (!page.isInteractable() || page.getInstance() == null
                    || !page.getInstance().equals(player.getInstance())) {
                continue;
            }
            Vec toPage = toPage(player, page);
            double distance = toPage.length();
            if (distance > MAX_GAZE_DISTANCE) {
                continue;
            }
            double cosine = cosine(player, toPage, distance);
            if (cosine < MIN_GAZE_COSINE) {
                continue;
            }
            if (bestPage == null
                    || cosine > bestCosine
                    || (Math.abs(cosine - bestCosine) < DISTANCE_EPSILON && distance < bestDistance)) {
                bestPage = page;
                bestCosine = cosine;
                bestDistance = distance;
            }
        }
        return bestPage;
    }

    private static boolean inView(Player player, PageEntity page, double minCosine, double maxDistance) {
        if (page.getInstance() == null || !page.getInstance().equals(player.getInstance())) {
            return false;
        }
        Vec toPage = toPage(player, page);
        double distance = toPage.length();
        return distance <= maxDistance && cosine(player, toPage, distance) >= minCosine;
    }

    private static Vec toPage(Player player, PageEntity page) {
        Pos eye = player.getPosition().add(0, player.getEyeHeight(), 0);
        Pos pagePos = page.getPosition();
        return new Vec(pagePos.x() - eye.x(), pagePos.y() - eye.y(), pagePos.z() - eye.z());
    }

    private static double cosine(Player player, Vec toPage, double distance) {
        if (distance < DISTANCE_EPSILON) {
            return 1.0D;
        }
        return player.getPosition().direction().normalize().dot(toPage.div(distance));
    }

    /**
     * What the service knows about one survivor's gaze. Only touched from the gaze task.
     */
    private static final class GazeState {
        private @Nullable UUID candidate;
        private long candidateSince;
        private @Nullable PageNoteDisplay shown;
        private long outOfRangeSince = -1;
    }
}
```

In `PageNote` diese Methoden und das Feld entfernen: `tooltipComponent` (Feld und Zuweisung im Konstruktor), `getTooltipComponent()`, `tooltipForCustomModel(int)`, `tooltipForItem(ItemStack)`. Danach prüfen, dass niemand sie mehr nutzt:

Run: `grep -rn "getTooltipComponent\|tooltipForItem\|tooltipForCustomModel\|TITLE_TIMES" --include='*.java' common game setup`
Expected: keine Treffer.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :game:test --tests "net.onelitefeather.cygnus.page.*" --tests "net.onelitefeather.cygnus.listener.page.PlayerPageInteractListenerTest" :common:test --tests "net.onelitefeather.cygnus.common.page.PageNoteTest"`
Expected: PASS.

Wenn `relocatedPageRemovesNoteImmediately` scheitert, weil der Teleport um `0.01` den Cosinus über die Ausblende-Schwelle schiebt, liegt der Fehler nicht im Test. `checkShown` muss die Positionsprüfung **vor** der Kegelprüfung machen, wie oben.

- [ ] **Step 5: Run the full suite**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add game/src/main/java/net/onelitefeather/cygnus/page/PageGazeService.java \
        game/src/test/java/net/onelitefeather/cygnus/page/PageGazeServiceTest.java \
        game/src/test/java/net/onelitefeather/cygnus/listener/page/PlayerPageInteractListenerTest.java \
        common/src/main/java/net/onelitefeather/cygnus/common/page/PageNote.java \
        common/src/test/java/net/onelitefeather/cygnus/common/page/PageNoteTest.java
git commit -m "feat(page): show page notes in the world with dwell, hysteresis and linger"
```

---

### Task 5: Prüfung im Spiel

Nicht automatisierbar. Die Ergebnisse gehen als Konstanten-Anpassung in einen eigenen Commit.

- [ ] **Step 1:** Server mit Resource Pack starten, eine Runde mit mindestens einem Survivor beginnen.
- [ ] **Step 2: Größe und Lage.** Aus 2, 3 und 4 Blöcken auf eine Page schauen. Die Notiz muss gut lesbar sein und die Page nicht verdecken. Bei Bedarf `NOTE_SCALE`, `LIFT` und `FACE_OFFSET` in `PageNoteDisplay` anpassen.
- [ ] **Step 3: Interaktion.** Bei offener Notiz auf die Page klicken. Die Page muss aufgehoben werden und die Notiz sofort verschwinden.
- [ ] **Step 4: Nebel.** Auf einer Karte mit `DENSE_FOG`-Atmosphäre prüfen, ob die Notiz bei 3–4 Blöcken noch lesbar ist. Das Ergebnis in der Spec unter „Offene Prüfungen“ festhalten.
- [ ] **Step 5: Shader-Signal.** Mit offener Notiz prüfen, ob die Welt-Abdunklung von `BossBarGazeSignal` ungewollt auslöst, also ob sich die Welt verdunkelt, ohne dass der Slender schaut. Tritt das auf, die Shader-Erkennung im `cygnus-pack` eingrenzen, nicht die Notiz.
- [ ] **Step 6: Commit** (nur wenn Konstanten geändert wurden)

```bash
git add game/src/main/java/net/onelitefeather/cygnus/page/PageNoteDisplay.java docs/superpowers/specs/2026-10-03-page-note-world-display-design.md
git commit -m "tune(page): adjust note display scale and offset after in-game check"
```
