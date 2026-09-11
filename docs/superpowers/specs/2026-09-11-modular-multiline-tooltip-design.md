# Modular Multi-Line Tooltip & Inline Bookmark Ribbon

Status: Entwurf / Review bereit  
Datum: 2026-09-11  

---

## 1. Übersicht & Zielsetzung

Der Page-Tooltip im Minecraft-Client-Look (Magenta-Ecken, graue Umrandung, dunkle Füllung) soll um zwei wesentliche Features erweitert werden:
1. **Inline-Lesezeichen (Ribbon):** Ein kompaktes Pixelart-Fähnchen (`4x6 px`) in Akzent-Magenta (`#A81894`), das links vor dem Text auf gleicher Baseline sitzt.
2. **Modulare Mehrzeiligkeit (Multi-Line Box):** Unterstützung für mehrzeilige Notizen (über Zeilenumbrüche `\n` im Text). Die Box passt sich dynamisch an die Anzahl der Zeilen an (1, 2, 3... Zeilen), ohne dass für jede Zeilenanzahl neue statische Texturen im Resource Pack erstellt werden müssen. Alle vertikalen Abschnitte schließen nahtlos mit 0 Pixel Abstand aneinander an.

### Nicht-Ziele
* Kein horizontales Umbrechen im Minecraft-Client zur Laufzeit: Zeilenumbrüche werden explizit über `\n` im Notiztext oder über separate `line(...)`-Aufrufe im Builder gesteuert.
* Keine dynamische Größenänderung während der Anzeige: Die Boxgröße wird beim Erzeugen des Components anhand des Textinhalts fest berechnet.

---

## 2. Git-Branches
* **Cygnus:** `feature/page-tooltip`
* **cygnus-pack:** `feature/tooltip-font`

---

## 3. Resource Pack (`cygnus-pack`)

### A. Texturen (`pack/assets/cygnus/textures/font/tooltip/`)
Statt vollständiger, starrer Boxen wird die Box in 3 wiederverwendbare vertikale Abschnitte zerlegt:

1. **Header-Band (Höhe 4px):**
   * `box_top_left.png` (5x4 px): Obere linke Magenta-Ecke mit 4px Armen, grauer Außenkante und dunkler Füllung.
   * `box_top_middle.png` (1x4 px): Oberer 1px grauer Rand mit 3px dunkler Füllung darunter.
   * `box_top_right.png` (5x4 px): Obere rechte Magenta-Ecke mit 4px Armen, grauer Außenkante und dunkler Füllung.

2. **Zeilen-Body-Band (Höhe 8px):**
   * `box_row_left.png` (5x8 px): Linke 1px graue Außenlinie mit 4px dunkler Füllung.
   * `box_row_middle.png` (1x8 px): Durchgehend dunkle Füllung (1x8 px).
   * `box_row_right.png` (5x8 px): Rechte 1px graue Außenlinie mit 4px dunkler Füllung.

3. **Footer-Band (Höhe 4px):**
   * `box_bottom_left.png` (5x4 px): Untere linke Magenta-Ecke mit 4px Armen, grauer Außenkante und dunkler Füllung.
   * `box_bottom_middle.png` (1x4 px): Unterer 1px grauer Rand mit 3px dunkler Füllung darüber.
   * `box_bottom_right.png` (5x4 px): Untere rechte Magenta-Ecke mit 4px Armen, grauer Außenkante und dunkler Füllung.

4. **Lesezeichen-Ribbon (`ribbon.png`):**
   * 4px breit, 6px hoch.
   * Magenta-Körper (`#A81894`) mit dunklerem Schattenrand (`#730E64`) und einer stilvollen V-Einkerbung an der Unterseite.

---

### B. Font-Konfiguration (`pack/assets/cygnus/font/`)

Da Minecraft-Subtitles intern keine automatischen `\n`-Zeilenumbrüche unterstützen, wird jede Textzeile über Font-Provider mit um jeweils 8px dekrementiertem `ascent` realisiert.

#### 1. Glyphen-Zuordnung in `cygnus:tooltip`
* `\uE103`: Lesezeichen-Ribbon (`ribbon.png`), `height: 6`, `ascent: 4` (auf Text-Baseline von Zeile 1).
* **Header-Band:**
  * `\uE110`: `box_top_left.png`, `height: 4`, `ascent: 8`
  * `\uE111`: `box_top_middle.png`, `height: 4`, `ascent: 8`
  * `\uE112`: `box_top_right.png`, `height: 4`, `ascent: 8`
* **Zeile 1 (Text & Body):**
  * Text Zeile 1: `minecraft:font/ascii.png`, `height: 6`, `ascent: 4`
  * Body Zeile 1: `box_row_left.png`, `box_row_middle.png`, `box_row_right.png`, `height: 8`, `ascent: 4`
* **Footer-Band (für 1-Zeiler):**
  * `box_bottom_left.png`, `box_bottom_middle.png`, `box_bottom_right.png`, `height: 4`, `ascent: -4`
  * Gesamthöhe 1-Zeiler: $4\text{px (Header)} + 8\text{px (Row 1)} + 4\text{px (Footer)} = 16\text{px}$.

#### 2. Mehrzeilige Fonts (`cygnus:tooltip_line2`, `cygnus:tooltip_line3`)
* **Zeile 2 (`cygnus:tooltip_line2`):**
  * Text: `minecraft:font/ascii.png`, `height: 6`, `ascent: -4` (exakt 8px unter Zeile 1).
  * Body Zeile 2: `box_row_*`, `height: 8`, `ascent: -4`.
  * Footer nach Zeile 2: `box_bottom_*`, `height: 4`, `ascent: -12`.
  * Gesamthöhe 2-Zeiler: $4 + 8 + 8 + 4 = 24\text{px}$.
* **Zeile 3 (`cygnus:tooltip_line3`):**
  * Text: `ascent: -12`.
  * Body Zeile 3: `ascent: -12`.
  * Footer nach Zeile 3: `ascent: -20`.
  * Gesamthöhe 3-Zeiler: $4 + 8 + 8 + 8 + 4 = 32\text{px}$.

---

## 4. Code-Architektur (`Cygnus`)

### A. Geometrie & Layout in `TooltipBox.java`

1. **Konstanten:**
   * `CAP_WIDTH = 5` (Breite der linken/rechten Box-Kappe)
   * `RIBBON_WIDTH = 4` (Breite der Lesemarke)
   * `RIBBON_GAP = 3` (Abstand zwischen Lesemarke und Text)
   * `INNER_LEFT_PADDING = 3` (Abstand zwischen linker Kappe und Lesemarke)
   * `INNER_RIGHT_PADDING = 4` (Abstand zwischen Textende und rechter Kappe)
   * `TEXT_LEFT_INDENT = INNER_LEFT_PADDING + RIBBON_WIDTH + RIBBON_GAP` ($3 + 4 + 3 = 10\text{px}$)

2. **Verarbeitung von Zeilenumbrüchen:**
   * Alle eingehenden Komponenten/Strings werden auf `\n` überprüft und in eine Zeilenliste `List<String>` zerlegt.
   * Zeile 1 erhält das Ribbon-Präfix.
   * Zeilen $2..N$ werden mit einem horizontalen Versatz von `TEXT_LEFT_INDENT` (10px) eingerückt, damit sie bündig unter dem Text von Zeile 1 stehen.

3. **Breitenberechnung:**
   * $\text{contentWidth}_1 = \text{TEXT_LEFT_INDENT} + \text{width}(\text{line}_1)$
   * $\text{contentWidth}_i = \text{TEXT_LEFT_INDENT} + \text{width}(\text{line}_i)$ für $i \ge 2$
   * $\text{maxContentWidth} = \max(\text{MIN_CONTENT_WIDTH}, \max_i(\text{contentWidth}_i))$
   * $\text{middleTiles} = \text{maxContentWidth} + \text{INNER_RIGHT_PADDING}$
   * $\text{boxWidth} = \text{CAP_WIDTH} + \text{middleTiles} + \text{CAP_WIDTH}$

4. **Component-Aufbau:**
   * **Schritt 1:** Führender Zentrierungs-Shift (`leadingSpace = boxWidth + 2 * crosshairOffsetX`).
   * **Schritt 2:** Header-Band zeichnen (`top_left + top_middle * middleTiles + top_right`).
   * **Schritt 3:** Für jede Zeile $k \in [1..N]$:
     * Cursor per negativer Verschiebung um `boxWidth` zurücksetzen.
     * Body-Band für Zeile $k$ zeichnen (`row_left + row_middle * middleTiles + row_right`).
   * **Schritt 4:** Footer-Band zeichnen (Cursor um `boxWidth` zurücksetzen, `bottom_left + bottom_middle * middleTiles + bottom_right`).
   * **Schritt 5:** Ribbon & Text zeichnen:
     * Cursor auf Text-Startpunkt zurücksetzen.
     * Bei Zeile 1: Ribbon zeichnen + 3px Lücke + Zeile 1 Text.
     * Bei Zeile 2+: Cursor um Zeilenbreite zurücksetzen, 10px einrücken, Zeile 2+ Text im jeweiligen Zeilen-Font zeichnen.
   * **Schritt 6:** Abschließender Ausgleichs-Shift zur Erhaltung der Gesamtbreite.

---

### B. `PageNote.java`
Aktualisierung der Notizen mit Zeilenumbrüchen für optimale Lesbarkeit:
* `ALWAYS_WATCHES`: `"Always watches,\nno eyes"`
* `DONT_LOOK`: `"Don't look\nor it takes you"`
* `CANT_RUN`: `"Can't run"` (bleibt einzeilig)
* `LEAVE_ME_ALONE`: `"Leave me alone"` (bleibt einzeilig)
* `HELP_ME`: `"Help me"` (bleibt einzeilig)
* `NO_NO_NO`: `"No no no\nno..."`

---

## 5. Verifikationsplan

### Automatisierte Tests
1. **`TooltipBoxTest.java`:**
   * Test für 1-zeilige Notiz (stellt sicher, dass Header, 1x Body, Footer, Ribbon und Zeile 1 korrekt generiert werden).
   * Test für 2-zeilige Notiz mit `\n` (prüft, ob beide Body-Bänder und Zeilen-Fonts korrekt eingebunden sind).
   * Test für 3-zeilige Notiz (prüft Skalierung auf 32px Box).
   * Test für exakte Breiten- und Zentrierungs-Berechnungen mit eingerückter Folgezeile.
2. **`FontWidthHelperTest.java`:**
   * Messung von mehrzeiligen Komponenten und Strings mit `\n`.
3. **Vollständiger Build:**
   * `./gradlew check` über alle Module (`:common`, `:game`, `:setup`).

### Manuelle Verifikation im Spiel
* Starten des Servers und Betrachten von einzeiligen Seiten (*"Help me"*) und zweizeiligen Seiten (*"Always watches,\nno eyes"*).
* Visuelle Prüfung auf:
  * 0 Pixel Nahtstellen zwischen Header, Body und Footer.
  * Korrekte Platzierung und Farbe des Lesezeichen-Ribbons.
  * Saubere vertikale Einrückung der zweiten Zeile.
  * Angenehme Abstände auf allen Seiten.
