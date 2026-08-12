# HUD System Architecture Design

**Date:** 2026-08-12  
**Author:** theEvilReaper / Antigravity  
**Status:** Proposed  

## 1. Overview

The HUD (Heads-Up Display) system in Cygnus is responsible for managing visual game overlays sent to Minecraft clients connected to a Minestom server. These elements include Scoreboard sidebars, ActionBars, BossBars, Titles/Subtitles, and TabList headers/footers.

This document refactors the initial `net.onelitefeather.cygnus.hud` architecture to cleanly decouple single-player HUD components from shared/multi-player HUD components, resolving Liskov Substitution Principle (LSP) violations and providing a flexible container for players to manage multiple HUD elements simultaneously.

---

## 2. Architectural Design

### 2.1 Interface Hierarchy

```
                     +-------------------+
                     |   HudComponent    |
                     +-------------------+
                               |
            +------------------+------------------+
            |                                     |
+-----------------------+             +-----------------------+
| PersonalHudComponent  |             |  SharedHudComponent   |
+-----------------------+             +-----------------------+
            |                                     |
(Individual Player HUDs)             (Extends Joinable)
                                     (Global / Team HUDs)
```

1. **`HudComponent` (Base Interface)**:
   - Root interface for all HUD elements.
   - Defines common lifecycle operations: `render()`, `show()`, `hide()`, `isVisible()`.
   - **Crucially decoupled from `Joinable`** so single-player HUDs are not forced to implement multi-player management methods.

2. **`PersonalHudComponent` (Single-Player HUD)**:
   - Extends `HudComponent`.
   - Bound to a single `CygnusPlayer`.
   - Responsible for rendering per-player information (e.g. personal stats, quest progress, personal scoreboard).

3. **`SharedHudComponent` (Multi-Player / Global HUD)**:
   - Extends `HudComponent` AND `Joinable`.
   - Manages a set of viewers (`Set<CygnusPlayer>`).
   - Cleanly implements `addPlayer`, `addPlayers`, `removePlayer`, `removePlayers` from `Joinable` without throwing `UnsupportedOperationException`.
   - Useful for global event timers, shared game bossbars, or team-wide scoreboards.

4. **`PlayerHudContainer` (Player HUD Collection)**:
   - Holds all active `HudComponent` instances attached to a single `CygnusPlayer`.
   - Allows attaching multiple HUDs (e.g. 1 Sidebar Scoreboard, 1 ActionBar, 1 BossBar) simultaneously.
   - Handles bulk operations (`renderAll()`, `hideAll()`).

---

## 3. Package & File Specification

Target Package: `net.onelitefeather.cygnus.hud`

### 3.1 `HudComponent.java` (Interface)
```java
package net.onelitefeather.cygnus.hud;

public interface HudComponent {

    void render();

    void hide();

    boolean isVisible();
}
```

### 3.2 `PersonalHudComponent.java` (Abstract Class or Interface)
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

### 3.3 `GlobalHudComponent.java` (Abstract Class)
```java
package net.onelitefeather.cygnus.hud;

import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import net.theevilreaper.xerus.api.Joinable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

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
    public void removePlayer(Player player, @Nullable Consumer<Player> consumer) {
        if (player instanceof CygnusPlayer cygnusPlayer) {
            players.remove(cygnusPlayer);
            if (consumer != null) consumer.accept(player);
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

### 3.4 `PlayerHudContainer.java` (New Class)
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

---

## 4. Verification & Testing Strategy

1. **Unit Verification**:
   - Ensure `PersonalHudComponent` can be created and managed per player without throwing `UnsupportedOperationException`.
   - Ensure `GlobalHudComponent` safely adds and removes players.
   - Verify `PlayerHudContainer` correctly manages multiple active HUD components per player.
2. **Build Verification**:
   - Run `./gradlew build` to confirm compilation across all submodules.

---

## 5. Concrete Global HUD Components (Pages & Timer)

This section mirrors the finished `net.theevilreaper.manis.hud` pattern from the sibling Manis project (sealed `HudComponent` → `AbstractHudComponent` holding a single `BossBar` → concrete components using `BackgroundBar`/`SpaceFont`/`TextWidth` glyph segments), but implemented on top of the `GlobalHudComponent` base kept from Section 2 rather than introducing a parallel sealed hierarchy.

### 5.1 `HudSegment` (Rendering Primitive)

Target: `common/src/main/java/net/onelitefeather/cygnus/common/text/HudSegment.java` (same package as `BackgroundBar`/`SpaceFont`/`TextWidth`, pure rendering logic with no game dependencies).

Builds one reusable "icon + background-wrapped text" segment: a fixed pixel offset reserved for an icon glyph (not yet assigned — deferred to a later iteration), followed by `BackgroundBar.wrap(text, paddingPx, tint)`.

```java
public static Component segment(Component text, int iconWidthPx, int paddingPx, TextColor tint) {
    return SpaceFont.positive(iconWidthPx)
            .append(BackgroundBar.wrap(text, paddingPx, tint));
}
```

The `tint` parameter doubles as the shader marker color (see Manis's `AbstractHudComponent`/`HealthComponent` convention: a near-white, per-component-unique `TextColor` that a resource-pack shader keys off to position the bar on screen). Cygnus does not yet have reserved marker colors for these two components, so placeholder constants are used (`MARKER_PAGES = TextColor.color(254, 254, 250)`, `MARKER_TIMER = TextColor.color(254, 254, 249)`) — easy to swap once the pack defines real slots.

### 5.2 `PageCountHudComponent` (standalone, one segment)

Target: `game/src/main/java/net/onelitefeather/cygnus/hud/PageCountHudComponent.java`.

A `GlobalHudComponent` holding its own `BossBar` (same construction pattern as the current `GameViewImpl`: `BossBar.bossBar(Component.empty(), 1f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS)`). Exposes an `update(Component pageStatus)` method that rebuilds the bar's name from a single `HudSegment.segment(pageStatus, ICON_WIDTH_PX, PADDING_PX, MARKER_PAGES)`. `pageStatus` is sourced from `PageProvider.getPageStatus()` (already the live `"x / y"` component, updated on every page find).

### 5.3 `PageTimerHudComponent` (composite, two segments)

Target: `game/src/main/java/net/onelitefeather/cygnus/hud/PageTimerHudComponent.java`.

A `GlobalHudComponent` that **replaces `GameView`/`GameViewImpl`** (functionally identical role: the single combined time+pages BossBar shown to survivors during a round). Exposes `update(int ticks, Component pageStatus)`, which formats `ticks` via the existing `Strings.getTimeString(TimeFormat.MM_SS, ticks)` helper and joins two `HudSegment.segment(...)` calls (pages first, then time, using `MARKER_PAGES`/`MARKER_TIMER` respectively) with a `SpaceFont.positive(GAP_PX)` gap between them:

```java
public void update(int ticks, Component pageStatus) {
    Component time = Component.text(Strings.getTimeString(TimeFormat.MM_SS, ticks));
    bossBar.name(HudSegment.segment(pageStatus, ICON_WIDTH_PX, PADDING_PX, MARKER_PAGES)
            .append(SpaceFont.positive(GAP_PX))
            .append(HudSegment.segment(time, ICON_WIDTH_PX, PADDING_PX, MARKER_TIMER)));
}
```

### 5.4 Data Flow (No New Timer/Event Needed)

Cygnus already ticks a round countdown every second: `GamePhase.onUpdate()` (via the Xerus `TimedPhase` base) calls `EventDispatcher.call(new ViewUpdateEvent(getCurrentTicks()))` once per second, currently consumed by `ViewUpdateListener`, which reads `pageProvider.getPageStatus()` and calls `gameView.updateView(...)`. This listener is repointed at the two new components instead:

```java
@Override
public void accept(ViewUpdateEvent event) {
    Component pageStatus = this.pageProvider.getPageStatus();
    this.pageTimerHudComponent.update(event.ticks(), pageStatus);
    this.pageCountHudComponent.update(pageStatus);
}
```

No new event, listener, or polling task is introduced — page-count changes are picked up on the next per-second tick, matching the existing precedent (the current combined time+pages bar already only refreshes once per second).

### 5.5 Migration of `GameView`/`GameViewImpl`

`GameView` (interface) and `GameViewImpl` are deleted; `PageTimerHudComponent` takes over their role one-for-one:

- `Cygnus.java` (constructs `new GameViewImpl()`) constructs `PageTimerHudComponent` and `PageCountHudComponent` instead.
- `GamePhase`/`WaitingPhase` (constructor parameter `GameView gameView`, used only for `addPlayers`/`removePlayers` on round start/end) take `PageTimerHudComponent` instead — `GlobalHudComponent` already implements `Joinable` (Section 2), so the call sites (`this.gameView.addPlayers(...)` / `.removePlayers(...)`) are unchanged apart from the type.
- `ViewUpdateListener` takes both new components instead of `GameView` (Section 5.4).
- `PageCountHudComponent` shares `PageTimerHudComponent`'s visibility lifecycle: `GamePhase`/`WaitingPhase` take a second constructor parameter for it and call `addPlayers`/`removePlayers` on both components together (same player set, same call sites) — both bars appear and disappear for survivors at the same moments `GameView` used to.
- Tests referencing `GameViewImpl` (`GameViewIntegrationTest`, `PlayerChatListenerTest`, `PlayerQuitListenerTest`, `PlayerLoginListenerTest`) are updated to construct `PageTimerHudComponent` instead; `GameViewIntegrationTest` is renamed/migrated to `PageTimerHudComponentIntegrationTest`.

### 5.6 Verification & Testing (Additive to Section 4)

1. `HudSegmentTest` (common): the icon offset is prepended correctly and the segment's total measured width (`TextWidth.widthOf`) matches `iconWidthPx + 2*paddingPx + textWidth`.
2. `PageCountHudComponentTest` / `PageTimerHudComponentTest` (game): `update(...)` produces the expected `BossBar` name content; `addPlayer`/`removePlayer` show/hide the bar correctly.
3. `PageTimerHudComponentIntegrationTest` replaces `GameViewIntegrationTest`.
4. `./gradlew build` stays green after the `GameView`/`GameViewImpl` removal (no leftover references).
