# Page-Notiz als TextDisplay in der Welt

Status: Design abgenommen, Umsetzung ausstehend
Datum: 2026-10-03
Branch: `feature/page-tooltip`

## Ziel

Die handschriftliche Notiz einer Page wird nicht mehr als Subtitle über den
Bildschirm gelegt. Sie erscheint direkt an der Page in der Welt, als
`TextDisplay` mit derselben Tooltip-Box (`cygnus:tooltip`-Glyphen, Ribbon,
mehrzeilig). Lesen soll sich ruhig anfühlen: kein Flackern an der Kante des
Blickkegels, kein Abschneiden nach fünf Sekunden, kein Auslösen beim bloßen
Vorbeischauen.

Das Aufheben einer Page bleibt unverändert und läuft weiter über die
`Interaction`-Hitbox von `PageEntity`.

## Nicht-Ziele

- Kein Buch, kein Dialog und keine andere Ansicht, die das Spiel unterbricht.
- Kein Raycast gegen die Hitbox und keine Prüfung auf Verdeckung durch Blöcke.
  Der Blickkegel bleibt die Erkennung und bekommt nur Hysterese.
- Keine neuen Texturen. Das Resource Pack bleibt unverändert, solange die
  Prüfung unter „Resource Pack“ nichts findet.
- Keine Anpassung an den Nebel der Karte. Wie gut die Notiz bei dichtem Nebel
  lesbar ist, wird im Spiel geprüft (siehe „Offene Prüfungen“).

## Ausgangslage

`PageGazeService` prüft alle 2 Ticks für jeden Survivor einen Kegel
(`MIN_GAZE_COSINE = 0.85`, `MAX_GAZE_DISTANCE = 4.0`) zur Mitte jeder
interagierbaren Page. Trifft er eine, zeigt er
`PageNote#getTooltipComponent()` als Subtitle mit 5 s Anzeigedauer. Daraus
entstehen diese Probleme:

1. Längere Notizen verschwinden nach 5 s mitten im Lesen.
2. Ein- und Ausblenden hängen an derselben Schwelle. Wer leicht schwankt, löst
   ständig `showTitle` und `clearTitle` aus.
3. Schon ein kurzer Blick löst den Text aus.
4. Der Titel-Slot wird mit anderen Titeln geteilt, die sich gegenseitig
   überschreiben.

## Entscheidungen

### TextDisplay statt Titel

Display-Entities haben keine anklickbare Hitbox, der Client ignoriert sie beim
Raycast. Klicks gehen also durch das `TextDisplay` hindurch zur bestehenden
`Interaction`-Hitbox, auch wenn das Display direkt davor schwebt. Am
Pickup-Pfad (`PlayerPageInteractListener`) ändert sich nichts.

Ein `TextDisplay` rendert dieselben Fonts wie ein Titel und zentriert jede
Zeile ebenso nach ihrer Gesamtbreite. Die Box aus `TooltipBox` lässt sich also
weiterverwenden.

### Ein Display pro Spieler und Page

Jeder Spieler, der eine Notiz liest, bekommt sein eigenes `TextDisplay`
(`setAutoViewable(false)`, einziger Viewer ist dieser Spieler). Andere
Survivors sehen nicht, was jemand gerade liest. Ein- und Ausblenden laufen
pro Spieler unabhängig, weil die Interpolation von `transformation` am Entity
hängt und nicht am Viewer. Bei der Zahl der Survivors pro Runde sind das
höchstens eine Handvoll Entities.

### Billboard `CENTER`

Das Display dreht sich immer zum Spieler. Die Notiz ist damit aus jedem Winkel
lesbar, auch schräg vor der Wand.

### Auslöser: Blickkegel mit Hysterese, Verweilen und Nachlauf

| Phase | Bedingung |
|---|---|
| Einblenden | Der Spieler hält Cosinus ≥ `0.85` und Abstand ≤ `4.0` zu derselben Page für mindestens `DWELL_TICKS = 6` (300 ms). |
| Sichtbar bleiben | Cosinus ≥ `0.70` und Abstand ≤ `5.5`. |
| Ausblenden | Die Bedingung zum Sichtbarbleiben ist seit `LINGER_TICKS = 10` (500 ms) ununterbrochen verletzt. |
| Sofort ausblenden | Die Page ist nicht mehr interagierbar, wurde verschoben, aufgehoben, der Spieler ist kein Survivor mehr, oder der Service stoppt. |

Kommt eine andere Page während des Verweilens besser in den Blick, beginnt das
Verweilen neu. Ist bereits eine Notiz sichtbar, wechselt sie erst, wenn die
neue Page die Einblende-Bedingung über die volle Verweildauer erfüllt. Die
alte Notiz wird dann sofort ausgeblendet.

Alle Schwellen sind Konstanten in `PageGazeService`, wie bisher.

## Komponenten

### `TooltipBox` (common): zentrierte Variante

Heute stellt `build()` immer eine Leerstelle von `boxWidth + 2 · crosshairOffsetX`
voran, damit die Box rechts neben dem Fadenkreuz sitzt. Für die Welt kommt
`Builder#centered()` dazu. Damit fällt die vorangestellte Leerstelle weg, und
die Gesamtbreite des Components ist genau `boxWidth`. Die Box liegt also
mittig auf dem Ankerpunkt des Displays. `crosshairOffsetX` hat dann keine
Wirkung.

Sonst ändert sich nichts an `build()`. Bänder, Ribbon und Zeilen-Fonts bleiben
gleich.

### `PageNote` (common)

Bekommt neben `getTooltipComponent()` ein zweites, beim Start gebautes
Component `getWorldComponent()` (`TooltipBox.builder().centered()`).
`tooltipForItem` bekommt ein Gegenstück `worldComponentForItem(ItemStack)`.

`getTooltipComponent()` und `tooltipForItem` entfallen (siehe „Was entfällt“).

### `PageEntity` (common)

`getResource()` ist heute package-private. Weil `PageNoteDisplay` im Modul
`game` die Richtung der Page braucht, wird die Methode `public`.

### `PageNoteDisplay` (game, neu, `net.onelitefeather.cygnus.page`)

Kapselt genau ein `TextDisplay` für einen Spieler und eine Page:

- `static PageNoteDisplay spawn(Player viewer, PageEntity page, Component note)`:
  Erzeugt das Entity, setzt die Metadaten (siehe unten), setzt es in die
  Instanz der Page, fügt `viewer` als einzigen Viewer hinzu und startet das
  Einblenden.
- `void hide()`: Startet das Ausblenden und entfernt das Entity, sobald es
  abgeschlossen ist.
- `void removeNow()`: Entfernt das Entity sofort, ohne Animation. Wird beim
  Stoppen des Services und beim Aufheben genutzt.
- `UUID pageId()`: Für den Abgleich mit der aktuellen Page.

Metadaten des `TextDisplay`:

| Feld | Wert | Grund |
|---|---|---|
| `text` | `PageNote#getWorldComponent()` | Die Box aus dem Pack. |
| `backgroundColor` | `0x00000000` | Ohne das liegt der graue Standard-Hintergrund über der Box-Textur. |
| `shadow` | `false` | Ein Schatten würde die Glyphen doppelt zeichnen. |
| `seeThrough` | `false` | Die Notiz soll nicht durch Wände sichtbar sein. |
| `lineWidth` | `1000` | Die Box ist eine einzige lange Glyphen-Zeile, die nicht umgebrochen werden darf. |
| `billboard` | `CENTER` | Siehe Entscheidungen. |
| `brightness` | fest, `NOTE_BLOCK_LIGHT = 15`, `NOTE_SKY_LIGHT = 15` | Sonst übernimmt das Display das Licht der Umgebung und ist auf dunklen Karten kaum lesbar. |
| `scale` | `NOTE_SCALE = 0.35` | Ein Text-Pixel ist 1/40 Block. Eine 150 px breite Box ist damit etwa 1,3 Blöcke breit. Der Wert wird im Spiel feinjustiert. |
| Position | Position der Page − Richtungsvektor von `getResource().face()` · `0.15` + `0.45` nach oben | `face` ist die Blickrichtung beim Setzen der Page, die Page hängt also auf der gegenüberliegenden Blockseite. Gegen diese Richtung versetzt, schwebt die Box knapp vor und über der Page, ohne in der Wand zu stecken. Die Hitbox bleibt frei anklickbar. |

Ein- und Ausblenden laufen über die Skalierung, weil die Client-Interpolation
`transformation` unterstützt, die Text-Deckkraft aber nicht:

- **Einblenden:** Das Display wird mit Scale `0` gespawnt. Im nächsten Tick
  wird mit `transformationInterpolationDuration = 4` auf `NOTE_SCALE` gesetzt.
  Der Tick dazwischen ist nötig, weil der Client sonst den Zielwert ohne
  Übergang übernimmt.
- **Ausblenden:** Interpolation von `3` Ticks auf Scale `0`, danach
  `remove()`.

### `PageGazeService` (game)

Bleibt der einzige Ort, der entscheidet, wer welche Notiz sieht. Pro Spieler
hält er einen Zustand statt der bisherigen `Map<Player, UUID>`:

```java
private record GazeState(
        @Nullable UUID candidate,      // Page, auf der das Verweilen läuft
        long candidateSince,           // Tick, an dem das Verweilen begann
        @Nullable PageNoteDisplay shown,
        long outOfRangeSince           // -1, solange die Sichtbar-Bedingung erfüllt ist
) {}
```

`tick()` wertet für jeden Survivor die Tabelle unter „Auslöser“ aus. Die
Tick-Zeit kommt aus einem Zähler, der pro Aufruf um `UPDATE_INTERVAL_TICKS`
erhöht wird. So lassen sich die Tests ohne echte Server-Zeit fahren.

Die öffentliche API bleibt gleich: `startTask`, `stopTask`, `isRunning`,
`clearPlayer`, `isGazing`, `activeGaze`, `tick`. Ihre Bedeutung ändert sich
leicht:

- `isGazing(player)` ist `true`, solange für den Spieler eine Notiz sichtbar
  ist, also auch während des Nachlaufs.
- `clearPlayer(player)` ruft `removeNow()` auf. Der Aufheben-Pfad entfernt
  die Notiz damit sofort.
- `stopTask()` ruft für alle Displays `removeNow()` auf.

Sicherheitsnetz für verschobene Pages: Weicht die Position der angezeigten
Page von der Position beim Einblenden ab, oder ist die Page nicht mehr in der
Liste von `pageSupplier`, wird die Notiz sofort ausgeblendet.
`PageProvider#relocate` braucht dafür keinen Callback.

## Was entfällt

- `TITLE_TIMES` und jeder Aufruf von `showTitle` und `clearTitle` in
  `PageGazeService`.
- `PageNote#getTooltipComponent()` und `tooltipForItem`. Einziger Aufrufer
  ist heute `PageGazeService`. `PageNoteTest` prüft künftig
  `getWorldComponent()`. `TooltipBox` behält die Ausrichtung am Fadenkreuz als
  Standard, weil sie im Builder nichts kostet und für andere HUD-Texte nützlich
  bleibt.

## Resource Pack

Am Pack ändert sich nichts. Vor dem Merge muss aber geprüft werden, ob das
Signal von `BossBarGazeSignal` im Text-Shader (`rendertype_text`) die
Box-Glyphen der Notiz fälschlich als Signal erkennt. Text in der Welt läuft
durch dieselben Shader-Typen wie der Bossbar-Titel. Das Signal hängt an einer
eigenen Glyphe mit RGB-Farbe. Die Box nutzt `WHITE` und andere Glyphen, ein
Treffer ist also unwahrscheinlich, aber ungeprüft.

## Tests

Mit Cyano (`MicrotusExtension`), wie die bestehenden `PageGazeServiceTest`:

- **Verweilen:** Ein Blick, der kürzer als `DWELL_TICKS` dauert, erzeugt kein
  Display. Ein Blick über die volle Dauer erzeugt genau eins.
- **Nur der Lesende sieht die Notiz:** Das Display hat den lesenden Spieler
  als einzigen Viewer. Ein zweiter Survivor daneben sieht es nicht.
- **Hysterese:** Ein Wechsel zwischen Cosinus `0.8` und `0.9` hält die Notiz
  stabil, ohne neues Display.
- **Nachlauf:** Nach dem Wegschauen bleibt die Notiz `LINGER_TICKS` lang
  sichtbar und wird danach entfernt.
- **Sofort ausblenden:** Die Notiz verschwindet sofort, wenn die Page nicht
  mehr interagierbar ist oder verschoben wurde, und bei `clearPlayer` und
  `stopTask`.
- **Pickup bleibt intakt:** Der bestehende `PlayerPageInteractListenerTest`
  läuft weiter. Er prüft zusätzlich, dass nach dem Aufheben kein Display des
  Spielers mehr existiert.
- **`TooltipBox#centered()`:** Die Gesamtbreite des Components entspricht
  genau `boxWidth`, ohne vorangestellte Leerstelle. Berechnet wird sie mit
  `FontWidthHelper`, wie im bestehenden Test.

Die bestehenden Tests, die Titel prüfen, werden auf Displays umgeschrieben.

## Offene Prüfungen im Spiel

Diese Punkte lassen sich nicht automatisiert testen. Sie werden nach der
Umsetzung im Client geprüft und die Konstanten gegebenenfalls angepasst:

- `NOTE_SCALE` und der Versatz zur Page: Ist die Notiz bei 2–4 Blöcken gut
  lesbar und verdeckt sie die Page nicht?
- Lesbarkeit bei dichten Nebel-Presets (`DENSE_FOG`).
- Das Shader-Signal aus „Resource Pack“.
