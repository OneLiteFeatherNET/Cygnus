# Slender-Trailer (Remotion)

Trailer für den Spielmodus Slender (Projektname: Cygnus), als Code gebaut mit [Remotion](https://github.com/remotion-dev/remotion).
Botschaft, Szenenfolge und Kanalwahl sind aus [`docs/marketing/swot.md`](../../docs/marketing/swot.md) abgeleitet.

| Komposition | Format | Länge | Einsatz |
|-------------|--------|-------|---------|
| `CygnusTeaser` | 1920×1080 | 27 s | Discord-Ankündigung, YouTube, Server-Listing |
| `CygnusShort` | 1080×1920 | 19,5 s | YouTube Shorts (eine Plattform, siehe SWOT Abschnitt 3) |
| `CygnusThumbnail` | 1920×1080, PNG | Standbild | YouTube-Thumbnail, Listing-Banner |

Szenen: Cold Open → Jäger → Seiten → Ausdauer → Seiten-Rotation (nur Teaser) → Zuschauer → Endkarte.

## Starten

Node 20 oder neuer.

```bash
cd marketing/trailer
npm ci
npm run studio          # Vorschau im Browser, Props live editierbar
npm run render:all      # out/cygnus-teaser-16x9.mp4, out/cygnus-short-9x16.mp4, out/cygnus-thumbnail.png
```

Remotion lädt beim ersten Render eine Headless-Chrome-Version herunter. Ohne Download-Zugang einen vorhandenen
Chromium angeben, z. B. `REMOTION_BROWSER_EXECUTABLE=/pfad/zu/headless_shell npm run render:teaser`.

## Pro Kanal rendern

Alle Texte und Links stehen in `src/props.ts` und lassen sich pro Render überschreiben. Für jede Plattform einen
eigenen Discord-Invite rendern, sonst ist nicht messbar, woher die Joins kommen:

```bash
npx remotion render CygnusShort out/short-yt.mp4 \
  --props='{"language":"de","serverAddress":"play.example.net","discordUrl":"discord.gg/abc123","availability":"Halloween 2026","sessionHint":"Slender-Nächte: 24., 25. und 31.10., 20 Uhr","footage":{}}'
```

`language: "en"` schaltet alle Texte auf Englisch.

## Gameplay-Aufnahmen einsetzen

Ohne Aufnahmen rendern die Szenen einen prozeduralen Wald als Platzhalter. Aufnahmen kommen nach `public/footage/`
(nicht im Git, zu groß) und werden über `footage` eingebunden, z. B. `"footage":{"hunt":"footage/hunt.mp4"}`.

Shotlist, 1920×1080, 30 fps, mit aktivem Pack, Replay Mod oder OBS, je 5 bis 8 Sekunden:

| Slot | Inhalt |
|------|--------|
| `hunt` | Überlebender läuft durch enge Gassen, Kamera dreht sich um, nichts zu sehen |
| `pages` | Seite an einer Wand finden und einsammeln |
| `stamina` | Sprint, bis die XP-Leiste leer ist, Tunnel-Vision schließt sich |
| `spectate` | Zuschauersicht über die Karte, die eigene Leiche im Bild |

Ton der Aufnahmen wird stummgeschaltet; die Tonspur mischt der Trailer selbst.

## Regeln für Änderungen

- **Name:** öffentlich „Slender“ (Prop `title`), Cygnus ist nur der Projektname. Kein Material aus dem Originalspiel (Logo, Grafiken, Sounds, Figurendesign) und nicht dessen Titel; alles im Trailer ist selbst gebaut. Risiko und Abwägung: SWOT T1/ST1.
- **Jugendschutz:** andeuten statt zeigen. Kein Blut-Overlay aus dem Pack, kein Gesicht, keine lauten Jumpscares. Bis die Altersempfehlung entschieden ist (PSR Abschnitt 6), bleibt das so.
- **Behauptungen:** jeder Satz muss sich auf ein vorhandenes Feature stützen. Keine Superlative. „Java Edition“ bleibt drin, solange es keinen Bedrock-Zugang gibt.
- **Marke:** Farben und Schrift aus `src/theme.ts` (OLF-Palette, Roboto). Das Feder-Logo liegt außerhalb aller Rausch- und Glitch-Ebenen: nicht verzerren, drehen, umfärben, weichzeichnen oder mit Schatten versehen.
- **Musik:** nur die selbst erzeugte Musik (siehe unten), eigene Produktionen oder CC0, sonst Content-ID-Sperren.

## Musik

Die Hintergrundmusik ist nicht heruntergeladen, sondern von `scripts/generate_music.py` synthetisiert: Drone mit
kleiner Sekunde und Tritonus, Wind, Spieluhr-Motiv, ein Herzschlag, der bis zum Schnitt in die Zuschauerszene
schneller wird, dann Stille und ein tiefer Schlag. Sie gehört damit OneLiteFeather, ohne Lizenzbedingungen,
Namensnennung oder Content-ID-Risiko.

Die Schnittzeiten kommen aus `src/cuts.json`, das auch `Trailer.tsx` nutzt. Nach einer Änderung an den Szenenlängen
die Musik neu erzeugen (Python 3 mit numpy):

```bash
npm run music    # schreibt public/music/cygnus-teaser.ogg und cygnus-short.ogg
```

Der Generator ist deterministisch, ein erneuter Lauf erzeugt dieselben Samples. `"music": null` rendert ohne Musik,
`"staticVolume": 0` schaltet das VHS-Rauschen an den Schnitten ab.

## Assets

`public/pack/` enthält Kopien aus `cygnus-pack` (Seiten-Texturen, Icons, Tunnel-Vision-Masken, VHS-Rauschen).
Nach Änderungen am Pack aktualisieren mit `npm run sync-pack -- /pfad/zu/cygnus-pack`.
`public/fonts/` enthält Roboto und Roboto Mono (SIL Open Font License, `OFL.txt`), lokal eingebunden, damit Renders offline und reproduzierbar laufen.
`public/brand/olf-logo.png` ist das OneLiteFeather-Feder-Logo.

## Lizenz von Remotion

Remotion ist für Einzelpersonen, gemeinnützige Projekte und Organisationen mit bis zu drei Mitarbeitenden kostenlos,
darüber braucht es eine [Company License](https://www.remotion.dev/license). OneLiteFeather als ehrenamtliches
Hobbyprojekt fällt in die kostenlose Stufe; bei einer Änderung der Organisationsform neu prüfen.
