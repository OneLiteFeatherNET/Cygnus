# HUD System Refactoring Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor the HUD system in Cygnus to decouple single-player HUD components from multi-player `Joinable` methods, fix exception violations, and introduce a `PlayerHudContainer` allowing a player to manage multiple HUD components simultaneously.

**Architecture:** Refactor `HudComponent` to a clean root interface (`render()`, `hide()`, `isVisible()`), introduce `PersonalHudComponent` bound to a single `CygnusPlayer`, update `GlobalHudComponent` to cleanly implement `Joinable`, and introduce `PlayerHudContainer` for multi-component management.

**Tech Stack:** Java 21, Minestom, JUnit 5.

## Global Constraints

- Target Package: `net.onelitefeather.cygnus.hud` and `net.onelitefeather.cygnus.hud.player`
- Must compile cleanly with `./gradlew build`
- Must preserve backwards-compatibility for existing call sites where applicable — the one deliberate, user-approved exception is Task 3's removal of `GameView`/`GameViewImpl` in favor of `PageTimerHudComponent` (design doc Section 5.5); that call-site break is in scope, not a regression to avoid
- No Mockito in this project (not a dependency); tests needing a `CygnusPlayer` use the existing `CygnusPlayerTestBase` + Microtus (`Env`/`MicrotusExtension`) convention (see `game/src/test/java/net/onelitefeather/cygnus/stamina/StaminaFactoryTest.java`)

---

### Task 1: Refactor `HudComponent` Interface & Base Classes

**Files:**
- Modify: `game/src/main/java/net/onelitefeather/cygnus/hud/HudComponent.java`
- Create: `game/src/main/java/net/onelitefeather/cygnus/hud/player/PersonalHudComponent.java`
- Modify: `game/src/main/java/net/onelitefeather/cygnus/hud/GlobalHudComponent.java`
- Modify: `game/src/main/java/net/onelitefeather/cygnus/hud/player/PlayerHudComponent.java`
- Modify: `game/src/main/java/net/onelitefeather/cygnus/hud/player/PlayerPageComponent.java`
- Create: `game/src/test/java/net/onelitefeather/cygnus/hud/HudComponentTest.java`

**Interfaces:**
- Consumes: Minestom `Player`, `CygnusPlayer`, `Joinable`
- Produces: `HudComponent`, `PersonalHudComponent`, `GlobalHudComponent`

- [ ] **Step 1: Write unit test for PersonalHudComponent and GlobalHudComponent**

Create `game/src/test/java/net/onelitefeather/cygnus/hud/HudComponentTest.java`:

No Mockito in this project — use the existing `CygnusPlayerTestBase` + Microtus (`Env`/`MicrotusExtension`) convention (see `game/src/test/java/net/onelitefeather/cygnus/stamina/StaminaFactoryTest.java` for the established pattern) to get a real connected `CygnusPlayer` instead of a mock:

```java
package net.onelitefeather.cygnus.hud;

import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.hud.player.PersonalHudComponent;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HudComponentTest extends CygnusPlayerTestBase {

    private static Instance instance;
    private static CygnusPlayer player;

    @BeforeAll
    static void setup(@NotNull Env env) {
        instance = env.createFlatInstance();
        player = (CygnusPlayer) env.createPlayer(instance);
    }

    @AfterAll
    static void teardown(@NotNull Env env) {
        env.destroyInstance(instance, true);
        instance = null;
        player = null;
    }

    @Test
    void testPersonalHudComponentVisibility() {
        PersonalHudComponent component = new PersonalHudComponent(player) {
            @Override
            public void render() {}
            @Override
            public void hide() {
                visible = false;
            }
        };

        assertTrue(component.isVisible());
        assertEquals(player, component.getPlayer());
        component.hide();
        assertFalse(component.isVisible());
    }

    @Test
    void testGlobalHudComponentAddRemovePlayer() {
        GlobalHudComponent component = new GlobalHudComponent() {
            @Override
            public void render() {}
            @Override
            public void hide() {
                visible = false;
            }
        };

        assertTrue(component.getPlayers().isEmpty());
        component.addPlayer(player);
        assertTrue(component.getPlayers().contains(player));
        component.removePlayer(player);
        assertFalse(component.getPlayers().contains(player));
    }
}
```

- [ ] **Step 2: Run test to verify it fails compilation/execution**

Run: `./gradlew test --tests net.onelitefeather.cygnus.hud.HudComponentTest`  
Expected: Compilation failure or missing methods.

- [ ] **Step 3: Update `HudComponent.java`**

Update `game/src/main/java/net/onelitefeather/cygnus/hud/HudComponent.java`:

```java
package net.onelitefeather.cygnus.hud;

public interface HudComponent {

    void render();

    void hide();

    boolean isVisible();
}
```

- [ ] **Step 4: Create `PersonalHudComponent.java`**

Create `game/src/main/java/net/onelitefeather/cygnus/hud/player/PersonalHudComponent.java`:

```java
package net.onelitefeather.cygnus.hud.player;

import net.onelitefeather.cygnus.hud.HudComponent;
import net.onelitefeather.cygnus.player.CygnusPlayer;

public abstract class PersonalHudComponent implements HudComponent {

    protected final CygnusPlayer player;
    protected boolean visible = true;

    protected PersonalHudComponent(CygnusPlayer player) {
        this.player = player;
    }

    public CygnusPlayer getPlayer() {
        return player;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }
}
```

- [ ] **Step 5: Refactor `GlobalHudComponent.java`**

Update `game/src/main/java/net/onelitefeather/cygnus/hud/GlobalHudComponent.java`:

```java
package net.onelitefeather.cygnus.hud;

import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import net.theevilreaper.xerus.api.Joinable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public abstract class GlobalHudComponent implements HudComponent, Joinable {

    protected final Set<CygnusPlayer> players;
    protected boolean visible = true;

    protected GlobalHudComponent() {
        this.players = new HashSet<>();
    }

    protected GlobalHudComponent(Set<CygnusPlayer> players) {
        this.players = new HashSet<>(players);
    }

    @Override
    public void addPlayer(Player player, @Nullable Consumer<Player> consumer) {
        if (player instanceof CygnusPlayer cygnusPlayer) {
            players.add(cygnusPlayer);
            if (consumer != null) consumer.accept(player);
        }
    }

    @Override
    public void addPlayers(Collection<Player> players, @Nullable Consumer<Player> consumer) {
        for (Player p : players) {
            addPlayer(p, consumer);
        }
    }

    @Override
    public void removePlayer(Player player, @Nullable Consumer<Player> consumer) {
        if (player instanceof CygnusPlayer cygnusPlayer) {
            players.remove(cygnusPlayer);
            if (consumer != null) consumer.accept(player);
        }
    }

    @Override
    public void removePlayers(Collection<Player> players, @Nullable Consumer<Player> consumer) {
        for (Player p : players) {
            removePlayer(p, consumer);
        }
    }

    @Contract(pure = true)
    public Set<CygnusPlayer> getPlayers() {
        return Collections.unmodifiableSet(players);
    }

    @Override
    public boolean isVisible() {
        return visible;
    }
}
```

- [ ] **Step 6: Update `PlayerHudComponent.java` and `PlayerPageComponent.java`**

Update `game/src/main/java/net/onelitefeather/cygnus/hud/player/PlayerHudComponent.java`:

```java
package net.onelitefeather.cygnus.hud.player;

import net.onelitefeather.cygnus.player.CygnusPlayer;

public class PlayerHudComponent extends PersonalHudComponent {

    public PlayerHudComponent(CygnusPlayer player) {
        super(player);
    }

    @Override
    public void render() {

    }

    @Override
    public void hide() {
        this.visible = false;
    }
}
```

Update `game/src/main/java/net/onelitefeather/cygnus/hud/player/PlayerPageComponent.java`:

```java
package net.onelitefeather.cygnus.hud.player;

import net.onelitefeather.cygnus.player.CygnusPlayer;

public class PlayerPageComponent extends PersonalHudComponent {

    public PlayerPageComponent(CygnusPlayer player) {
        super(player);
    }

    @Override
    public void render() {

    }

    @Override
    public void hide() {
        this.visible = false;
    }
}
```

- [ ] **Step 7: Run test to verify it passes**

Run: `./gradlew test --tests net.onelitefeather.cygnus.hud.HudComponentTest`  
Expected: PASS

- [ ] **Step 8: Commit Task 1**

```bash
git add game/src/main/java/net/onelitefeather/cygnus/hud/
git add game/src/test/java/net/onelitefeather/cygnus/hud/
git commit -m "refactor(hud): decouple HudComponent from Joinable and add PersonalHudComponent"
```

---

### Task 2: Implement `PlayerHudContainer` for Multi-Component Management

**Files:**
- Create: `game/src/main/java/net/onelitefeather/cygnus/hud/player/PlayerHudContainer.java`
- Create: `game/src/test/java/net/onelitefeather/cygnus/hud/player/PlayerHudContainerTest.java`

**Interfaces:**
- Consumes: `HudComponent`, `PersonalHudComponent`
- Produces: `PlayerHudContainer`

- [ ] **Step 1: Write unit test for `PlayerHudContainer`**

Create `game/src/test/java/net/onelitefeather/cygnus/hud/player/PlayerHudContainerTest.java`:

No Mockito — same `CygnusPlayerTestBase` + Microtus convention as `HudComponentTest` in Task 1:

```java
package net.onelitefeather.cygnus.hud.player;

import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerHudContainerTest extends CygnusPlayerTestBase {

    private static Instance instance;
    private static CygnusPlayer player;

    @BeforeAll
    static void setup(@NotNull Env env) {
        instance = env.createFlatInstance();
        player = (CygnusPlayer) env.createPlayer(instance);
    }

    @AfterAll
    static void teardown(@NotNull Env env) {
        env.destroyInstance(instance, true);
        instance = null;
        player = null;
    }

    static class DummyScoreboardComponent extends PersonalHudComponent {
        boolean rendered = false;
        boolean hidden = false;

        public DummyScoreboardComponent(CygnusPlayer player) {
            super(player);
        }

        @Override
        public void render() {
            rendered = true;
        }

        @Override
        public void hide() {
            hidden = true;
            visible = false;
        }
    }

    @Test
    void testRegisterGetAndRenderAll() {
        PlayerHudContainer container = new PlayerHudContainer();
        DummyScoreboardComponent scoreboard = new DummyScoreboardComponent(player);

        container.register(DummyScoreboardComponent.class, scoreboard);
        assertTrue(container.get(DummyScoreboardComponent.class).isPresent());
        assertEquals(scoreboard, container.get(DummyScoreboardComponent.class).get());

        container.renderAll();
        assertTrue(scoreboard.rendered);

        container.hideAll();
        assertTrue(scoreboard.hidden);
    }
}
```

- [ ] **Step 2: Run test to verify it fails compilation**

Run: `./gradlew test --tests net.onelitefeather.cygnus.hud.player.PlayerHudContainerTest`  
Expected: FAIL (Class `PlayerHudContainer` does not exist).

- [ ] **Step 3: Create `PlayerHudContainer.java`**

Create `game/src/main/java/net/onelitefeather/cygnus/hud/player/PlayerHudContainer.java`:

```java
package net.onelitefeather.cygnus.hud.player;

import net.onelitefeather.cygnus.hud.HudComponent;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerHudContainer {

    private final Map<Class<? extends HudComponent>, HudComponent> components = new ConcurrentHashMap<>();

    public <T extends HudComponent> void register(Class<T> clazz, T component) {
        components.put(clazz, component);
    }

    @SuppressWarnings("unchecked")
    public <T extends HudComponent> Optional<T> get(Class<T> clazz) {
        return Optional.ofNullable((T) components.get(clazz));
    }

    public void remove(Class<? extends HudComponent> clazz) {
        HudComponent removed = components.remove(clazz);
        if (removed != null) {
            removed.hide();
        }
    }

    public void renderAll() {
        components.values().stream()
                .filter(HudComponent::isVisible)
                .forEach(HudComponent::render);
    }

    public void hideAll() {
        components.values().forEach(HudComponent::hide);
    }

    public Collection<HudComponent> getComponents() {
        return Collections.unmodifiableCollection(components.values());
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests net.onelitefeather.cygnus.hud.player.PlayerHudContainerTest`  
Expected: PASS

- [ ] **Step 5: Run full project build and test suite**

Run: `./gradlew build`  
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit Task 2**

```bash
git add game/src/main/java/net/onelitefeather/cygnus/hud/player/PlayerHudContainer.java
git add game/src/test/java/net/onelitefeather/cygnus/hud/player/PlayerHudContainerTest.java
git commit -m "feat(hud): add PlayerHudContainer to manage multiple HUD components per player"
```

---

### Task 3: Add `HudSegment`, `PageCountHudComponent` & `PageTimerHudComponent`, retire `GameView`

See design doc Section 5 (`docs/superpowers/specs/2026-08-12-hud-system-design.md`) for full rationale.

**Files:**
- Create: `common/src/main/java/net/onelitefeather/cygnus/common/text/HudSegment.java`
- Create: `common/src/test/java/net/onelitefeather/cygnus/common/text/HudSegmentTest.java`
- Create: `game/src/main/java/net/onelitefeather/cygnus/hud/PageCountHudComponent.java`
- Create: `game/src/main/java/net/onelitefeather/cygnus/hud/PageTimerHudComponent.java`
- Create: `game/src/test/java/net/onelitefeather/cygnus/hud/PageCountHudComponentTest.java`
- Create: `game/src/test/java/net/onelitefeather/cygnus/hud/PageTimerHudComponentTest.java`
- Modify: `game/src/main/java/net/onelitefeather/cygnus/listener/view/ViewUpdateListener.java`
- Modify: `game/src/main/java/net/onelitefeather/cygnus/Cygnus.java`
- Modify: `game/src/main/java/net/onelitefeather/cygnus/phase/GamePhase.java`
- Modify: `game/src/main/java/net/onelitefeather/cygnus/phase/WaitingPhase.java`
- Delete: `game/src/main/java/net/onelitefeather/cygnus/view/GameView.java`
- Delete: `game/src/main/java/net/onelitefeather/cygnus/view/GameViewImpl.java`
- Modify/Rename: `game/src/test/java/net/onelitefeather/cygnus/view/GameViewIntegrationTest.java` → `PageTimerHudComponentIntegrationTest.java`
- Modify: `game/src/test/java/net/onelitefeather/cygnus/listener/PlayerChatListenerTest.java`
- Modify: `game/src/test/java/net/onelitefeather/cygnus/listener/PlayerQuitListenerTest.java`
- Modify: `game/src/test/java/net/onelitefeather/cygnus/listener/PlayerLoginListenerTest.java`

**Interfaces:**
- Consumes: `BackgroundBar`, `SpaceFont`, `TextWidth`, `GlobalHudComponent`, `PageProvider`, `ViewUpdateEvent`
- Produces: `HudSegment`, `PageCountHudComponent`, `PageTimerHudComponent`

- [ ] **Step 1: Write unit test for `HudSegment`**

Create `common/src/test/java/net/onelitefeather/cygnus/common/text/HudSegmentTest.java` asserting `TextWidth.widthOf(HudSegment.segment(text, iconWidthPx, paddingPx, tint))` equals `iconWidthPx + 2 * paddingPx + TextWidth.widthOf(text)`.

- [ ] **Step 2: Implement `HudSegment.segment(...)`** per design doc Section 5.1.

- [ ] **Step 3: Run `HudSegmentTest`, confirm PASS.**

- [ ] **Step 4: Write unit tests for `PageCountHudComponent` and `PageTimerHudComponent`**

Assert `update(...)` sets the expected `BossBar` name (compare against `HudSegment.segment(...)` output built the same way in the test), and that `addPlayer`/`removePlayer` show/hide the bar for the given player (mock `Player`).

- [ ] **Step 5: Implement `PageCountHudComponent`** per design doc Section 5.2 (placeholder `MARKER_PAGES = TextColor.color(254, 254, 250)`).

- [ ] **Step 6: Implement `PageTimerHudComponent`** per design doc Section 5.3 (placeholder `MARKER_TIMER = TextColor.color(254, 254, 249)`).

- [ ] **Step 7: Run the two new component tests, confirm PASS.**

- [ ] **Step 8: Repoint `ViewUpdateListener` at the new components** per design doc Section 5.4 — replace the `GameView gameView` field/constructor param with `PageTimerHudComponent`/`PageCountHudComponent`.

- [ ] **Step 9: Repoint `Cygnus.java`, `GamePhase.java`, `WaitingPhase.java`** per design doc Section 5.5 — construct/inject `PageTimerHudComponent`/`PageCountHudComponent` in place of `GameView`/`GameViewImpl`.

- [ ] **Step 10: Delete `GameView.java` and `GameViewImpl.java`.**

- [ ] **Step 11: Migrate tests** — rename `GameViewIntegrationTest` to `PageTimerHudComponentIntegrationTest` (construct `PageTimerHudComponent` instead of `GameViewImpl`); update `PlayerChatListenerTest`, `PlayerQuitListenerTest`, `PlayerLoginListenerTest` to construct `PageTimerHudComponent` instead of `GameViewImpl`.

- [ ] **Step 12: Run full project build and test suite**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL, no leftover `GameView`/`GameViewImpl` references.

- [ ] **Step 13: Commit Task 3**

```bash
git add common/src/main/java/net/onelitefeather/cygnus/common/text/HudSegment.java
git add common/src/test/java/net/onelitefeather/cygnus/common/text/HudSegmentTest.java
git add game/src/main/java/net/onelitefeather/cygnus/hud/PageCountHudComponent.java
git add game/src/main/java/net/onelitefeather/cygnus/hud/PageTimerHudComponent.java
git add game/src/test/java/net/onelitefeather/cygnus/hud/PageCountHudComponentTest.java
git add game/src/test/java/net/onelitefeather/cygnus/hud/PageTimerHudComponentTest.java
git add game/src/main/java/net/onelitefeather/cygnus/listener/view/ViewUpdateListener.java
git add game/src/main/java/net/onelitefeather/cygnus/Cygnus.java
git add game/src/main/java/net/onelitefeather/cygnus/phase/GamePhase.java
git add game/src/main/java/net/onelitefeather/cygnus/phase/WaitingPhase.java
git add game/src/test/java/net/onelitefeather/cygnus/listener/PlayerChatListenerTest.java
git add game/src/test/java/net/onelitefeather/cygnus/listener/PlayerQuitListenerTest.java
git add game/src/test/java/net/onelitefeather/cygnus/listener/PlayerLoginListenerTest.java
git rm game/src/main/java/net/onelitefeather/cygnus/view/GameView.java
git rm game/src/main/java/net/onelitefeather/cygnus/view/GameViewImpl.java
git mv game/src/test/java/net/onelitefeather/cygnus/view/GameViewIntegrationTest.java game/src/test/java/net/onelitefeather/cygnus/hud/PageTimerHudComponentIntegrationTest.java
git commit -m "feat(hud): add PageCountHudComponent and PageTimerHudComponent, retire GameView"
```
