# Horror-Fog pro Karte — Implementierungsplan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Jede Cygnus-Karte trägt ihre eigene statische Atmosphäre (Nebel, Himmel, Licht) in der `map.json`, die im Setup-Modus eingestellt und in einer Vorschau begutachtet werden kann.

**Architecture:** Die sieben Atmosphärenwerte werden aus `DimensionPreset` in ein neues Interface `DimensionAtmosphere` herausgelöst. `MapAtmosphere` ist die keylose Record-Implementierung, die in `GameMap` hängt und als Hex-JSON in der `map.json` landet. `GameMapProvider` registriert daraus beim Boot einen Dimension-Typ `cygnus:map/<name>` und erzeugt die Spielinstanz damit. Im Setup stellt eine Kette von Confirm-Dialogen die Werte ein; die Vorschau registriert eine Wegwerf-Dimension und schickt den Builder per Konfigurationsphase in eine Wegwerf-Instanz.

**Tech Stack:** Java 25, Minestom 2026.08.28-26.2 (`EnvironmentAttribute`, `DynamicRegistry`), Gson, Aves (Inventare, Maps), Guira (SetupData), Pica (Dialoge), Falco (Anvil-Loader), JUnit 5 + Cyano.

**Spec:** `docs/superpowers/specs/2026-08-30-horror-fog-design.md`

## Global Constraints

- Javadoc auf allen neuen Typen mit `@author`, `@version`, `@since`. `@since` ist `2.7.3`.
- Fehlt der `atmosphere`-Block in einer `map.json`, bleibt die Karte auf `DimensionType.OVERWORLD`. Bestehende Karten dürfen nicht brechen.
- `-Dminestom.registry.unsafe-ops` wird nicht gesetzt.
- Registry-Key-Format: `cygnus:map/<kartenname klein, nicht-alphanumerisch zu _>` bzw. `cygnus:preview/<uuid ohne bindestriche>/<n>`.
- Wertebereiche: `fogStartDistance >= 0`, `fogEndDistance > fogStartDistance`, `skyFogEndDistance > 0`, `skyLightFactor` in `[0, 1]`. Ungültige Werte werden abgeklemmt und geloggt, nie geworfen.
- Tests laufen mit `./gradlew :<modul>:test --tests "<Klasse>"`.

---

### Task 1: Atmosphäre vom Preset trennen

**Files:**
- Create: `common/src/main/java/net/onelitefeather/cygnus/common/dimension/DimensionAtmosphere.java`
- Modify: `common/src/main/java/net/onelitefeather/cygnus/common/dimension/DimensionPreset.java`
- Modify: `common/src/main/java/net/onelitefeather/cygnus/common/dimension/DimensionFactory.java`
- Modify: `game/src/main/java/net/onelitefeather/cygnus/CygnusLoader.java`

**Interfaces:**
- Produces: `DimensionAtmosphere` mit `RGBLike fogColor()`, `RGBLike skyLightColor()`, `RGBLike skyColor()`, `float skyLightFactor()`, `float fogStartDistance()`, `float fogEndDistance()`, `float skyFogEndDistance()`; `DimensionPreset extends DimensionAtmosphere` mit zusätzlich `String getKey()`; `DimensionFactory.create(Key, DimensionAtmosphere)` und `DimensionFactory.create(DimensionPreset)`.

- [ ] **Step 1: `DimensionAtmosphere` anlegen** — die sieben Getter mit dem Javadoc, das heute an `DimensionPreset` steht.
- [ ] **Step 2: `DimensionPreset` auf `extends DimensionAtmosphere` reduzieren** — behält nur `getKey()`.
- [ ] **Step 3: `DimensionFactory` umbauen** — `create(Key registryKey, DimensionAtmosphere atmosphere)` wird primär, `create(DimensionPreset)` delegiert mit `Key.key("cygnus", preset.getKey())`. Der fest verdrahtete `AMBIENT_PARTICLES`-Block mit `SOUL`/`SOUL_FIRE_FLAME` entfällt, `SUN_ANGLE`/`MOON_ANGLE` bleiben. `registerAll()` wird gelöscht.
- [ ] **Step 4: `CygnusLoader` entrümpeln** — der `cygnus.customDimension`-Block und der `DimensionFactory`-Import fliegen raus.
- [ ] **Step 5: Bestehende Tests laufen lassen**

Run: `./gradlew :common:test --tests "*DimensionPreset*"`
Expected: PASS — `StaticDimensionPresetTest` und `SeedDimensionPresetTest` sind von der Trennung nicht betroffen.

- [ ] **Step 6: Commit** — `refactor(dimension): split atmosphere values out of DimensionPreset`

---

### Task 2: `MapAtmosphere`

**Files:**
- Create: `common/src/main/java/net/onelitefeather/cygnus/common/dimension/MapAtmosphere.java`
- Test: `common/src/test/java/net/onelitefeather/cygnus/common/dimension/MapAtmosphereTest.java`

**Interfaces:**
- Consumes: `DimensionAtmosphere` aus Task 1.
- Produces: `record MapAtmosphere(Color fogColor, Color skyLightColor, Color skyColor, float skyLightFactor, float fogStartDistance, float fogEndDistance, float skyFogEndDistance) implements DimensionAtmosphere`. Feldtyp ist bewusst `Color`, nicht `RGBLike` — Gson kann kein Interface deserialisieren.

- [ ] **Step 1: Failing test schreiben**

```java
@Test
void clampsNegativeFogStartToZero() {
    MapAtmosphere atmosphere = new MapAtmosphere(
            new Color(15, 96, 52), new Color(18, 105, 60), Color.BLACK,
            0.008f, -20f, 48f, 32f);

    assertEquals(0f, atmosphere.fogStartDistance());
}

@Test
void liftsFogEndAboveFogStart() {
    MapAtmosphere atmosphere = new MapAtmosphere(
            new Color(15, 96, 52), new Color(18, 105, 60), Color.BLACK,
            0.008f, 64f, 32f, 32f);

    assertTrue(atmosphere.fogEndDistance() > atmosphere.fogStartDistance());
}

@Test
void clampsSkyLightFactorIntoUnitRange() {
    MapAtmosphere atmosphere = new MapAtmosphere(
            new Color(15, 96, 52), new Color(18, 105, 60), Color.BLACK,
            5f, 0f, 48f, 32f);

    assertEquals(1f, atmosphere.skyLightFactor());
}

@Test
void keepsValidValuesUntouched() {
    MapAtmosphere atmosphere = new MapAtmosphere(
            new Color(15, 96, 52), new Color(18, 105, 60), Color.BLACK,
            0.008f, 0f, 48f, 32f);

    assertEquals(48f, atmosphere.fogEndDistance());
    assertEquals(0.008f, atmosphere.skyLightFactor());
}

@Test
void copiesEveryValueFromAPreset() {
    MapAtmosphere atmosphere = MapAtmosphere.from(StaticDimensionPreset.DENSE_FOG);

    assertEquals(StaticDimensionPreset.DENSE_FOG.fogEndDistance(), atmosphere.fogEndDistance());
    assertEquals(StaticDimensionPreset.DENSE_FOG.fogColor(), atmosphere.fogColor());
}
```

- [ ] **Step 2: Test laufen lassen, Fehlschlag prüfen**

Run: `./gradlew :common:test --tests "*MapAtmosphereTest"`
Expected: FAIL — `MapAtmosphere` existiert nicht.

- [ ] **Step 3: Record implementieren** — Kompaktkonstruktor klemmt ab (`Math.clamp` für `skyLightFactor`, `Math.max(0f, …)` für `fogStartDistance`, `fogEndDistance` mindestens `fogStartDistance + 1f`), jede Korrektur wird über einen `Logger` auf `warn` gemeldet. `static MapAtmosphere from(DimensionAtmosphere)` baut aus einem Preset; `RGBLike` wird dabei über `new Color(rgb.red(), rgb.green(), rgb.blue())` in `Color` überführt.
- [ ] **Step 4: Test laufen lassen** → PASS
- [ ] **Step 5: Commit** — `feat(dimension): add MapAtmosphere value type`

---

### Task 3: Farben als Hex in JSON

**Files:**
- Create: `common/src/main/java/net/onelitefeather/cygnus/common/util/ColorGsonAdapter.java`
- Modify: `common/src/main/java/net/onelitefeather/cygnus/common/util/GsonHelper.java`
- Test: `common/src/test/java/net/onelitefeather/cygnus/common/util/ColorGsonAdapterTest.java`

**Interfaces:**
- Produces: `ColorGsonAdapter implements JsonSerializer<Color>, JsonDeserializer<Color>`, registriert in `GsonHelper.GSON` für `Color.class`. Format `#RRGGBB`, Großschreibung beim Schreiben, beim Lesen case-insensitiv und mit optionalem `#`.

- [ ] **Step 1: Failing test schreiben**

```java
private static final Gson GSON = GsonHelper.GSON;

@Test
void writesColorsAsHex() {
    assertEquals("\"#0F6034\"", GSON.toJson(new Color(15, 96, 52), Color.class));
}

@Test
void readsHexBackIntoTheSameColor() {
    assertEquals(new Color(15, 96, 52), GSON.fromJson("\"#0F6034\"", Color.class));
}

@Test
void acceptsLowerCaseAndMissingHash() {
    assertEquals(new Color(15, 96, 52), GSON.fromJson("\"0f6034\"", Color.class));
}

@Test
void rejectsMalformedHex() {
    assertThrows(JsonParseException.class, () -> GSON.fromJson("\"#xyz\"", Color.class));
}
```

- [ ] **Step 2: Test laufen lassen, Fehlschlag prüfen**

Run: `./gradlew :common:test --tests "*ColorGsonAdapterTest"`
Expected: FAIL — Gson serialisiert `Color` als Objekt mit `red`/`green`/`blue`.

- [ ] **Step 3: Adapter implementieren und in `GsonHelper` registrieren** (`.registerTypeAdapter(Color.class, new ColorGsonAdapter())`).
- [ ] **Step 4: Test laufen lassen** → PASS
- [ ] **Step 5: Commit** — `feat(util): serialize colors as hex strings`

---

### Task 4: Atmosphäre an der Karte

**Files:**
- Modify: `common/src/main/java/net/onelitefeather/cygnus/common/map/GameMap.java`
- Modify: `common/src/main/java/net/onelitefeather/cygnus/common/map/GameMapBuilder.java`
- Modify: `common/src/main/java/net/onelitefeather/cygnus/common/map/adapter/GameMapAdapter.java`
- Test: `common/src/test/java/net/onelitefeather/cygnus/common/map/adapter/GameMapAdapterTest.java`

**Interfaces:**
- Consumes: `MapAtmosphere` (Task 2), `ColorGsonAdapter` (Task 3).
- Produces: `GameMap#getAtmosphere()` → `@Nullable MapAtmosphere`; `GameMapBuilder#setAtmosphere(@Nullable MapAtmosphere)` und `#getAtmosphere()`; der `GameMap`-Konstruktor bekommt `@Nullable MapAtmosphere atmosphere` als letzten Parameter.

- [ ] **Step 1: Failing test in `GameMapAdapterTest` ergänzen**

```java
@Test
void readsTheAtmosphereBlock() {
    String json = """
            {"name":"Granskoga","atmosphere":{"fogColor":"#0F6034","skyLightColor":"#12693C",
            "skyColor":"#000000","skyLightFactor":0.008,"fogStartDistance":0.0,
            "fogEndDistance":48.0,"skyFogEndDistance":32.0}}""";

    GameMap map = GsonHelper.GSON.fromJson(json, GameMap.class);

    assertNotNull(map.getAtmosphere());
    assertEquals(new Color(15, 96, 52), map.getAtmosphere().fogColor());
    assertEquals(48f, map.getAtmosphere().fogEndDistance());
}

@Test
void leavesTheAtmosphereNullWhenAbsent() {
    GameMap map = GsonHelper.GSON.fromJson("{\"name\":\"Granskoga\"}", GameMap.class);

    assertNull(map.getAtmosphere());
}

@Test
void survivesARoundTripThroughJson() {
    MapAtmosphere atmosphere = MapAtmosphere.from(StaticDimensionPreset.DENSE_FOG);
    GameMap map = new GameMap("Granskoga", null, null, Set.of(), Set.of(), List.of(), atmosphere);

    GameMap parsed = GsonHelper.GSON.fromJson(GsonHelper.GSON.toJson(map), GameMap.class);

    assertEquals(atmosphere, parsed.getAtmosphere());
}
```

- [ ] **Step 2: Test laufen lassen, Fehlschlag prüfen**

Run: `./gradlew :common:test --tests "*GameMapAdapterTest"`
Expected: FAIL — `GameMap` hat kein `getAtmosphere()`.

- [ ] **Step 3: Feld durchziehen** — `GameMap` bekommt das nullable Feld plus Getter, `GameMapBuilder` Setter/Getter und die Übergabe in `build()` sowie im `GameMap`-Konstruktor, `GameMapAdapter` liest `ATMOSPHERE_KEY = "atmosphere"` über `deserializeOrDefault(object, ATMOSPHERE_KEY, MapAtmosphere.class, context, null)`.
- [ ] **Step 4: Test laufen lassen** → PASS
- [ ] **Step 5: Commit** — `feat(map): carry a per-map atmosphere in map.json`

---

### Task 5: Dimension im Spiel anwenden

**Files:**
- Modify: `game/src/main/java/net/onelitefeather/cygnus/map/GameMapProvider.java`
- Test: `game/src/test/java/net/onelitefeather/cygnus/map/GameMapProviderIntegrationTest.java`

**Interfaces:**
- Consumes: `DimensionFactory.create(Key, DimensionAtmosphere)` (Task 1), `GameMap#getAtmosphere()` (Task 4).
- Produces: `GameMapProvider#getGameDimension()` → `RegistryKey<DimensionType>`, entweder die registrierte Karten-Dimension oder `DimensionType.OVERWORLD`.

- [ ] **Step 1: Failing test ergänzen** — eine Karte mit `atmosphere` in der `map.json` erzeugt eine Instanz, deren `getDimensionType()` nicht `DimensionType.OVERWORLD` ist und deren Key mit `cygnus:map/` beginnt; eine Karte ohne `atmosphere` bleibt auf `OVERWORLD`.
- [ ] **Step 2: Test laufen lassen, Fehlschlag prüfen**

Run: `./gradlew :game:test --tests "*GameMapProviderIntegrationTest"`
Expected: FAIL — die Instanz läuft auf `OVERWORLD`.

- [ ] **Step 3: Provider umbauen** — die Spielkarte wird im Konstruktor geladen (der `MapEntry`-Lookup wandert aus `loadGameMap()` in ein Feld), aus `getAtmosphere()` wird die Dimension registriert und in `this.gameDimension` gehalten; `loadGameMap()` ruft `createInstanceContainer(this.gameDimension)`. Der Kartenname wird für den Key auf `[a-z0-9_]` normalisiert.
- [ ] **Step 4: Test laufen lassen** → PASS
- [ ] **Step 5: Volle Modul-Tests** — `./gradlew :common:test :game:test`
- [ ] **Step 6: Commit** — `feat(game): run the game map on its own dimension`

---

### Task 6: Atmosphäre im Setup-Inventar

**Files:**
- Create: `setup/src/main/java/net/onelitefeather/cygnus/setup/inventory/slot/AtmosphereSlot.java`
- Modify: `setup/src/main/java/net/onelitefeather/cygnus/setup/map/MapDataCategory.java`
- Modify: `setup/src/main/java/net/onelitefeather/cygnus/setup/inventory/view/InventoryMode.java`
- Modify: `setup/src/main/java/net/onelitefeather/cygnus/setup/inventory/view/MapDataOverviewInventory.java`
- Modify: `setup/src/main/java/net/onelitefeather/cygnus/setup/event/dialog/DialogTarget.java`

**Interfaces:**
- Produces: `MapDataCategory.ATMOSPHERE` (`Material.CAMPFIRE`, `NamedTextColor.GRAY`); `DialogTarget.ATMOSPHERE_PRESET` und `DialogTarget.ATMOSPHERE_VALUES`; `AtmosphereSlot(MapDataCategory, @Nullable MapAtmosphere)`.

- [ ] **Step 1: `MapDataCategory.ATMOSPHERE` ergänzen** — ans Ende der Enum, damit die `EnumSet`-Reihenfolge der bestehenden Kategorien unverändert bleibt.
- [ ] **Step 2: `InventoryMode.GAME` auf fünf Slots erweitern** — `new int[]{10, 11, 13, 15, 16}` plus `MapDataCategory.ATMOSPHERE`.
- [ ] **Step 3: `AtmosphereSlot` anlegen** — zeigt bei gesetzter Atmosphäre Fog-Ende und Fog-Farbe als Lore, sonst „nicht gesetzt"; Linksklick feuert `new DialogRequestEvent(player, DialogTarget.ATMOSPHERE_PRESET)`, Rechtsklick `new PlayerRemoveDataEvent(player, MapDataCategory.ATMOSPHERE)`.
- [ ] **Step 4: `MapDataOverviewInventory#getDataSlot` erweitern** — `case ATMOSPHERE -> new AtmosphereSlot(MapDataCategory.ATMOSPHERE, ((GameMapBuilder) mapBuilder).getAtmosphere())`.
- [ ] **Step 5: Kompilieren** — `./gradlew :setup:compileJava` → BUILD SUCCESSFUL
- [ ] **Step 6: Commit** — `feat(setup): show the map atmosphere in the setup inventory`

---

### Task 7: Dialoge zum Einstellen

**Files:**
- Create: `setup/src/main/java/net/onelitefeather/cygnus/setup/dialog/AtmosphereDialogs.java`
- Create: `setup/src/main/java/net/onelitefeather/cygnus/setup/dialog/handler/AtmospherePresetHandler.java`
- Create: `setup/src/main/java/net/onelitefeather/cygnus/setup/dialog/handler/AtmosphereValuesHandler.java`
- Modify: `setup/src/main/java/net/onelitefeather/cygnus/setup/listener/dialog/DialogRequestListener.java`
- Modify: `setup/src/main/java/net/onelitefeather/cygnus/setup/listener/dialog/DialogPayloadListener.java`
- Modify: `setup/src/main/java/net/onelitefeather/cygnus/setup/event/dialog/DialogContext.java`

**Interfaces:**
- Consumes: `MapAtmosphere` (Task 2), `DialogTarget` (Task 6).
- Produces: `AtmosphereDialogs.PRESET_KEY` / `VALUES_KEY`; `AtmosphereDialogs.openPresetDialog(Player)`; `AtmosphereDialogs.openValueDialog(Player, MapAtmosphere)`; `DialogContext.AtmosphereContext(MapAtmosphere atmosphere)`.

- [ ] **Step 1: `AtmosphereDialogs.openPresetDialog`** — `DialogType.confirm(PRESET_KEY)` mit `SingleOptionTemplate` `preset`, Optionen aus `StaticDimensionPreset.getValues()` plus `keep` für „aktuelle Werte behalten".
- [ ] **Step 2: `AtmosphereDialogs.openValueDialog`** — vier `RangeTemplate`-Slider (`fog_start` 0–128 Schritt 1, `fog_end` 8–384 Schritt 1, `sky_fog_end` 8–256 Schritt 1, `sky_light` 0–100 Schritt 1) mit den Werten der übergebenen Atmosphäre als `initial`, dazu drei Textfelder `fog_color`, `sky_light_color`, `sky_color` mit `maxLength(7)`. Ja-Button „Vorschau", Nein-Button „Abbrechen".
- [ ] **Step 3: `AtmospherePresetHandler`** — liest `preset`, baut daraus per `MapAtmosphere.from(...)` die Startwerte (bei `keep` die vorhandenen oder `MapAtmosphere.from(StaticDimensionPreset.DENSE_FOG)`) und öffnet den Wertedialog.
- [ ] **Step 4: `AtmosphereValuesHandler`** — liest die sieben Eingaben, `sky_light` wird durch 1000 geteilt, Farben über `ColorGsonAdapter`-kompatibles Hex-Parsing; schreibt das Ergebnis in den `GameMapBuilder` und startet die Vorschau.
- [ ] **Step 5: Listener verdrahten** — `DialogRequestListener` um die beiden neuen Targets, `DialogPayloadListener` um die beiden neuen Handler-Einträge.
- [ ] **Step 6: Kompilieren** — `./gradlew :setup:compileJava` → BUILD SUCCESSFUL
- [ ] **Step 7: Commit** — `feat(setup): configure the map atmosphere through dialogs`

---

### Task 8: Live-Vorschau

**Files:**
- Create: `setup/src/main/java/net/onelitefeather/cygnus/setup/atmosphere/AtmospherePreviewService.java`
- Modify: `setup/src/main/java/net/onelitefeather/cygnus/setup/SetupExtension.java`

**Interfaces:**
- Consumes: `DimensionFactory.create(Key, DimensionAtmosphere)` (Task 1), `MapAtmosphere` (Task 2).
- Produces: `AtmospherePreviewService#preview(Player, MapAtmosphere, Path worldRoot)`, `#leave(Player)`, `#pendingInstance(Player)` → `@Nullable Instance`.

- [ ] **Step 1: Service anlegen** — hält je Spieler eine Session aus Vorschau-Instanz, `FalcoAnvilLoader`, Ursprungsinstanz, Ursprungsposition und Zähler.
- [ ] **Step 2: `preview(...)` implementieren** — Key `cygnus:preview/<uuid ohne bindestriche>/<n>` registrieren, Instanz mit `createInstanceContainer(dimension)` und eigenem `FalcoAnvilLoader` auf demselben Weltverzeichnis bauen, Ursprung merken, `player.setPendingOptions(previewInstance, false)` und `player.startConfigurationPhase()`. Ab dem 50. Aufruf je Spieler eine `warn`-Zeile über die wachsende Registry.
- [ ] **Step 3: `leave(...)` implementieren** — zurück auf die Ursprungsinstanz und -position über denselben Weg, danach Vorschau-Instanz abmelden und Loader schließen.
- [ ] **Step 4: Konfigurations-Listener anpassen** — der Lambda in `SetupExtension` fragt zuerst `previewService.pendingInstance(event.getPlayer())` und fällt auf `instanceSupplier.get()` zurück.
- [ ] **Step 5: Disconnect absichern** — der bestehende `PlayerDisconnectEvent`-Listener räumt zusätzlich eine offene Vorschau-Session ab.
- [ ] **Step 6: Kompilieren und volle Tests** — `./gradlew build`
- [ ] **Step 7: Commit** — `feat(setup): preview a map atmosphere live`
