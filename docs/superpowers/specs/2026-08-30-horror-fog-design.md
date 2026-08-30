# Horror-Fog pro Karte

Status: Design abgenommen, Umsetzung ausstehend
Datum: 2026-08-30

## Ziel

Jede Cygnus-Karte bekommt eine eigene, statische Atmosphäre — Nebelfarbe,
Nebeldistanzen, Himmels- und Lichtfarben. Ein Map-Builder stellt sie im
Setup-Modus ein, sieht sie dort in einer Vorschau und speichert sie in die
`map.json` der Karte.

Nicht Ziel: Nebel, der zur Laufzeit auf das Spielgeschehen reagiert. Dichte,
die sich mit Slender-Nähe oder Seitenzahl ändert, bleibt Sache von
`TunnelVisionService` und `SlenderGazeService`.

## Ausgangslage

`common/.../dimension/` enthält bereits ein Preset-System (`DimensionPreset`,
zwölf `StaticDimensionPreset`s, `SeedDimensionPreset`) auf Basis der
Minestom-`EnvironmentAttribute`-API. Es kommt im Spiel aber nirgends an:
`CygnusLoader` registriert die Presets hinter `-Dcygnus.customDimension` global
als Dimension-Typen, während sowohl `GameMapProvider` als auch
`InstanceSetupData` ihre Instanzen auf `DimensionType.OVERWORLD` erzeugen.

## Entscheidungen

### Statisch pro Karte, keine Laufzeit-Dynamik

Dynamischer Nebel würde bedeuten, den Dimension-Typ eines Spielers zur Laufzeit
zu wechseln. Registry-Daten gehen nur in der Konfigurationsphase an den Client;
jede Änderung kostet also Konfigurationsphase plus Chunk-Reload. Für Dynamik
gibt es mit Tunnel Vision bereits einen Mechanismus, der ohne das auskommt.

### Dimension-Typen werden beim Boot registriert

`GameMapProvider` liest die Spielkarte im Konstruktor und registriert dort
`cygnus:map/<name>`. Das läuft in `new Cygnus()` und damit vor
`bootstrap.start()` — vor jedem Login, also erreichen die Registry-Daten jeden
Client in seiner Konfigurationsphase.

Verworfen: die Dimension erst in `loadGameMap()` zu registrieren und alle
Spieler beim Lobby-Wechsel durch eine Konfigurationsphase zu schicken. Das
erlaubt zwar Hot-Reload der Atmosphäre, macht aber ausgerechnet den
Spielstart-Moment fragiler. Unter CloudNet fährt ein Service ohnehin genau eine
Karte, ein Neustart nach Atmosphären-Änderung ist dort normal.

### Freie Werte, Preset nur als Startpunkt

Der Builder wählt ein Preset, um schnell in die Nähe zu kommen, und justiert
danach frei. Gespeichert wird immer der aufgelöste Vollsatz aus sieben Werten,
nie ein Preset-Name — die Karte bleibt damit unabhängig von späteren Änderungen
an den Presets.

## Datenmodell (`common`)

Das bestehende Interface wird geteilt:

- `DimensionAtmosphere` (neu) — die sieben Werte `fogColor`,
  `skyLightColor`, `skyColor`, `skyLightFactor`, `fogStartDistance`,
  `fogEndDistance`, `skyFogEndDistance`.
- `DimensionPreset extends DimensionAtmosphere` — ergänzt nur `getKey()`.
  `StaticDimensionPreset` und `SeedDimensionPreset` bleiben unverändert.
- `MapAtmosphere` (neu) — Record, implementiert `DimensionAtmosphere`, ohne
  Key. Der Registry-Key wird aus dem Kartennamen abgeleitet und nicht
  gespeichert.

`DimensionFactory` bekommt `create(Key registryKey, DimensionAtmosphere)` als
primäre Methode. Das bestehende `create(DimensionPreset)` bleibt als Delegation
mit `Key.key("cygnus", preset.getKey())` erhalten, damit vorhandene Aufrufer
unverändert weiterlaufen.

`MapAtmosphere` klemmt im Kompaktkonstruktor ab statt zu werfen:
`fogStartDistance >= 0`, `fogEndDistance > fogStartDistance`,
`skyFogEndDistance > 0`, `skyLightFactor` in `[0, 1]`. Eine kaputte `map.json`
darf den Service nicht am Start hindern; der abgeklemmte Wert wird geloggt.

### JSON

```json
"atmosphere": {
  "fogColor": "#0F6034",
  "skyLightColor": "#12693C",
  "skyColor": "#000000",
  "skyLightFactor": 0.008,
  "fogStartDistance": 0.0,
  "fogEndDistance": 48.0,
  "skyFogEndDistance": 32.0
}
```

Farben als Hex-Strings über einen neuen `ColorGsonAdapter`, registriert in
`GsonHelper`. Lesbar für Builder, saubere Git-Diffs.

Fehlt der `atmosphere`-Block, bleibt die Karte auf `OVERWORLD`. Alle
bestehenden Karten laufen unverändert weiter.

Das Feld gehört an `GameMap` (plus `GameMapBuilder` und `GameMapAdapter`), nicht
in eine Datei daneben: das Setup schreibt die `map.json` vollständig über
`GameMap` neu, ein Feld außerhalb ginge bei jedem Speichern verloren.

## Anwendung im Spiel (`game`)

`GameMapProvider` lädt die Spielkarte im Konstruktor statt in `loadGameMap()`
und registriert die Dimension dort. `loadGameMap()` erzeugt die Instanz danach
mit `createInstanceContainer(key, chunkLoader)` und bleibt idempotent. Ohne
Atmosphäre in der `map.json` wird `DimensionType.OVERWORLD` verwendet.

`CygnusLoader` verliert `-Dcygnus.customDimension` und
`DimensionFactory.registerAll()`. Beides ist mit per-Karte-Atmosphäre
gegenstandslos — registriert wird künftig genau die Dimension, die die geladene
Karte braucht.

In `DimensionFactory.create()` entfällt der fest verdrahtete
`AMBIENT_PARTICLES`-Block mit `SOUL` und `SOUL_FIRE_FLAME`. Er galt bisher für
alle Presets und würde sonst für jede Karte gelten; glühende Seelenpartikel
widersprechen realistischem Nebel. `SUN_ANGLE` und `MOON_ANGLE` auf `180f`
bleiben.

Die Builder-Defaults von `DimensionType.builder()` sind `minY = -64` und
`height = 384` und damit identisch zur Overworld — die Anvil-Chunks der Karte
werden von einer eigenen Dimension korrekt dargestellt. `FalcoAnvilLoader` wird
weiterhin mit `DimensionType.OVERWORLD.key()` erzeugt.

Die Lobby bleibt außen vor. Sie wird als Aves-`BaseMap` geladen und über
`LobbyData` gespeichert; ein Atmosphären-Feld bräuchte eine Änderung an einer
fremden Klasse. Der Kontrast aus heller Lobby und nebliger Spielkarte ist
zudem erwünscht.

## Setup mit Live-Vorschau (`setup`)

Neue `MapDataCategory.ATMOSPHERE` mit Campfire-Item, dazu ein `AtmosphereSlot`
im Setup-Inventar.

Pica bietet nur `DialogType.confirm(Key)`, also Zwei-Button-Dialoge. Der Ablauf
ist deshalb eine Kette von Confirm-Dialogen statt eines Dialogs mit drei
Knöpfen:

1. **Preset-Dialog** — `SingleOptionTemplate` über die zwölf
   `StaticDimensionPreset`s plus "aktuelle Werte behalten".
2. **Feintuning-Dialog** — vier `RangeTemplate`-Slider für Fog-Start `0–128`,
   Fog-Ende `8–384`, Sky-Fog-Ende `8–256` und Sky-Light-Faktor `0–100`
   (intern durch 1000 geteilt), dazu drei Textfelder für die Hex-Farben.
   Ja startet die Vorschau, Nein verwirft.
3. **Vorschau** — `AtmospherePreviewService` registriert
   `cygnus:preview/<uuid>/<n>`, schickt den Builder über
   `Player#startConfigurationPhase()` und setzt ihn in eine separate
   Wegwerf-Instanz auf demselben Weltverzeichnis.
4. **Bestätigungs-Dialog** — speichern und zurück in die echte Setup-Instanz,
   oder zurück zu Schritt 2 mit den aktuellen Werten.

Die Vorschau ist bewusst eine eigene Instanz: der lebende Setup-Zustand in
`InstanceSetupData` (Instanz plus offener `FalcoAnvilLoader`) kann so gar nicht
beschädigt werden. Setup-Marker und -Entities sind in der Vorschau nicht
sichtbar, und jede Vorschau kostet zwei Ladebildschirme. Für die Beurteilung
von Nebel ist beides vertretbar.

Jede Vorschau belegt einen neuen Registry-Eintrag. `DynamicRegistry.remove`
ist hinter `-Dminestom.registry.unsafe-ops=true` gesperrt; diese Property wird
nicht gesetzt, nur um hier aufzuräumen. Ein Eintrag ist wenige hundert Byte
groß. Ab 50 Vorschauen je Session wird gewarnt.

`Player#setPendingOptions(Instance, boolean)` ist der wahrscheinliche
Mechanismus, um nach der Konfigurationsphase in der richtigen Instanz zu
landen. Das ist in der Umsetzung zu verifizieren; die Alternative ist
`AsyncPlayerConfigurationEvent#setSpawningInstance`.

## Betroffene Dateien

`common`
- neu: `dimension/DimensionAtmosphere.java`, `dimension/MapAtmosphere.java`,
  `util/ColorGsonAdapter.java`
- geändert: `dimension/DimensionPreset.java`, `dimension/DimensionFactory.java`,
  `map/GameMap.java`, `map/GameMapBuilder.java`,
  `map/adapter/GameMapAdapter.java`, `util/GsonHelper.java`

`game`
- geändert: `CygnusLoader.java`, `map/GameMapProvider.java`

`setup`
- neu: `dialog/AtmosphereDialogs.java`, `dialog/handler/AtmosphereHandler.java`,
  `inventory/slot/AtmosphereSlot.java`,
  `atmosphere/AtmospherePreviewService.java`
- geändert: `map/MapDataCategory.java`, `data/GameData.java`,
  `listener/dialog/DialogPayloadListener.java`

## Tests

- `common`: `MapAtmosphere` klemmt ungültige Werte ab; `ColorGsonAdapter`
  Hex-Roundtrip inklusive ungültiger Eingabe; `GameMapAdapter` mit fehlendem,
  unvollständigem und vollständigem `atmosphere`-Block.
- `game`: Cyano-Test, dass die Spielinstanz auf der registrierten Dimension
  läuft und ohne `atmosphere` auf `OVERWORLD` bleibt.

Javadoc nach Projektstandard mit `@author`, `@version` und `@since` auf allen
neuen Typen.

## Offene Punkte

Keine.
