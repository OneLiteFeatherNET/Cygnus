# Page-Tooltip im Minecraft-Client-Look

Status: Design abgenommen, Umsetzung ausstehend  
Datum: 2026-09-10  

## Ziel

Wenn ein Survivor im First-Person-Gameplay auf eine in der Welt platzierte Seite (*Page*) blickt, soll ein atmosphärischer Tooltip-Kasten eingeblendet werden. Dieser Kasten nutzt die exakte Optik des Minecraft-Client-Tooltips (dunkler, halbtransparenter Hintergrund mit lilafarbenem Rahmenverlauf) und zeigt die jeweilige Grusel-Notiz der Seite (z. B. *„Always watches, no eyes“*) an. Sobald der Spieler wegblickt oder die Seite eingesammelt wird, verschwindet der Tooltip wieder.

### Nicht-Ziele
* Kein Tutorial- oder Interaktions-HUD (weder Seitenzahl wie „Page: 1“ noch Klick-Hinweise wie „[Rechtsklick] Sammeln“).
* Keine GUI-Öffnung: Die Einblendung erfolgt nahtlos im First-Person-Modus (via Subtitle-HUD).

---

## Git Branches

Für die Umsetzung werden in beiden Repositories eigene Feature-Branches angelegt:
* **Cygnus:** `feature/page-tooltip`
* **cygnus-pack:** `feature/tooltip-font`

---

## Resource Pack (`cygnus-pack`)

Das Resource Pack liefert die modularen Kacheln und die Font-Konfiguration für das 9-Slice-Rendering.

### 1. Texturen (`assets/cygnus/textures/font/tooltip/`)
Basierend auf den bestehenden Vanilla-Sprites aus `assets/minecraft/textures/gui/sprites/tooltip/` (`frame.png` und `background.png`):
* **Obere Kante:**
  * `corner_tl.png`: Ecke oben links
  * `border_top.png`: Oberer Rand
  * `corner_tr.png`: Ecke oben rechts
* **Mittlere Kante (Body / Hintergrund):**
  * `border_left.png`: Linker Rand
  * `bg_fill.png`: Dunkler, halbtransparenter Hintergrund
  * `border_right.png`: Rechter Rand
* **Untere Kante:**
  * `corner_bl.png`: Ecke unten links
  * `border_bottom.png`: Unterer Rand
  * `corner_br.png`: Ecke unten rechts

### 2. Font-Definition (`assets/cygnus/font/tooltip.json`)
* **Bitmap-Provider:** Weist die Kachel-Grafiken privaten Unicode-Zeichen zu (z. B. `\uE100` bis `\uE108`):
  * Ecken & Kanten: `TL`, `T`, `TR`, `L`, `BG`, `R`, `BL`, `B`, `BR`
* **Space-Provider (`type: "space"`):** Stellt negative Abstände (`\uF801` = -1px, `\uF802` = -2px, `\uF804` = -4px, ..., bis `\uF880` = -128px) bereit, um nach dem Zeichnen der Hintergrund-Zeile den Textcursor um die Kastenbreite nach links zurückzusetzen.

---

## Code-Architektur (`Cygnus`)

### 1. `common`-Modul

#### A. Notizen-Zuordnung (`PageNote`)
Enum oder Resolver, der jedem Seiten-Modell (`page_1` bis `page_6`) seine stimmige Notiz zuweist:
* `page_1`: *„Always watches, no eyes“*
* `page_2`: *„Don't look or it takes you“*
* `page_3`: *„Can't run“*
* `page_4`: *„Leave me alone“*
* `page_5`: *„Help me“*
* `page_6`: *„No no no no...“*

#### B. `FontWidthHelper`
* Berechnet für beliebige Adventure-`Component`s die exakte Pixelbreite im Minecraft-Standard-Font.
* Berücksichtigt Standard-Zeichenbreiten, Leerzeichen und `TextDecoration.BOLD` (+1px pro Zeichen).

#### C. `TooltipBox`
* Modulare Builder-Klasse zum Erzeugen der Adventure-`Component`:
  1. Ermittelt die maximale Pixelbreite der Textzeilen + Padding.
  2. Baut den oberen Rand (`TL` + wiederholtes `T` + `TR`).
  3. Baut die Inhaltszeile(n):
     * Hintergrundzeile rendern (`L` + wiederholtes `BG` + `R`).
     * Negatives Spacing für `- (Breite - Padding)` einfügen.
     * Den eigentlichen Notiz-Text rendern.
  4. Baut den unteren Rand (`BL` + wiederholtes `B` + `BR`).
  5. Liefert ein fertiges Adventure-`Component` zurück.

---

### 2. `game`-Modul

#### A. `PageGazeService`
* Ticking-Service (z. B. alle 2 Ticks):
  * Filtert auf lebende Survivor (`TeamHelper.isSurvivorTeam(player)`).
  * Sucht nach aktiven `PageEntity`s im Umkreis von maximal 4 Blöcken.
  * Prüft mittels Richtungsvektor / Blickwinkel (analog zu `SlenderGaze`), ob das Fadenkreuz des Survivors die Interaction-Hitbox der Page fokussiert.
  * Prüft Sichtlinie (keine massiven Blöcke zwischen Kopf und Page).
* **Fokus erkannt:** Sendet via `player.showTitle(...)` die `TooltipBox`-Component als Subtitle mit dezentem Fade-In/Out (z. B. 100ms Fade-In, 500ms Stay, 150ms Fade-Out).
* **Fokus verloren / weggeschaut:** Cleart den Title sofort (`player.clearTitle()`).

#### B. Event-Handling
* **Page eingesammelt:** In `PlayerPageInteractListener` oder über ein Page-Claim-Event wird der Title des Spielers sofort gecleart, falls er die Page im Moment des Einsammelns noch anvisiert.
* **Spieler stirbt / Spectator:** Beim Wechsel ins Spectator-Team werden aktive Page-Gaze-Zustände sofort zurückgesetzt.

---

## Verifikationsplan

### Automatisierte Tests
* `FontWidthHelperTest`:
  * Pixelbreiten einzelner Zeichen und Wörter validieren.
  * Formatierungs-Zuschläge (Bold) prüfen.
* `TooltipBoxTest`:
  * Prüfen, dass die resultierende `Component` die korrekten Glyphen, negativen Spacer und Textinhalte in der richtigen Reihenfolge enthält.
* `PageNoteTest`:
  * Sicherstellen, dass jedes registrierte Seitenmodell eine gültige Notiz besitzt.
* `PageGazeServiceTest`:
  * Blickkontakt-Berechnung bei direktem Blick vs. abgewandtem Blick.
  * Reichweitenbegrenzung (> 4 Blöcke = kein Tooltip).

### Manuelle Verifikation
* Beide Repositories mit den neuen Branches bauen:
  * Resource Pack lokal aktualisieren/packen.
  * Server starten und Spielrunde starten.
* An eine Page herantreten:
  * Aus 5 Blöcken Distanz: Noch kein Tooltip.
  * Innerhalb 4 Blöcke direkt auf die Page schauen: Tooltip ploppt im authentischen Client-Look auf.
  * Wegdrehen: Tooltip verschwindet unverzüglich.
  * Seite einsammeln: Tooltip verschwindet sofort und bleibt weg.
