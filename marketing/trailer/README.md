# Slender-Trailer (Remotion)

Trailer für den Spielmodus Slender (Projektname: Cygnus), als Code gebaut mit [Remotion](https://github.com/remotion-dev/remotion).
Botschaft, Szenenfolge und Kanalwahl sind aus [`docs/marketing/swot.md`](../../docs/marketing/swot.md) abgeleitet.

Fünf Varianten, jede in drei Formaten. Die Komposition heißt `<variante>-<format>`, z. B. `creek-vertical`.

| Variante | Länge | Hook (Cold Open) | Szenen |
|----------|-------|------------------|--------|
| `teaser` | 31 s | Eine Runde. Unter zehn Minuten. | Jäger, Seiten, Ausdauer, Creek verfolgt/jagt/erwischt, Zuschauer |
| `creek` | 18 s | Es bewegt sich nur, wenn du wegschaust. | Creek verfolgt, jagt, erwischt |
| `manfred` | 17,5 s | Das ist Manfred. Hör nicht auf, ihn anzusehen. | Creek jagt, erwischt, Seiten |
| `hunter` | 19,5 s | Du hast ihn nicht gesehen. Er dich schon. | Jäger, Seiten, Ausdauer, Zuschauer |
| `pages` | 20 s | Die Seite war gerade noch da. | Seiten, Rotation, Ausdauer, Zuschauer |

| Format | Größe | Für |
|--------|-------|-----|
| `vertical` | 1080×1920 | TikTok, YouTube Shorts, Instagram Reels und Stories |
| `square` | 1080×1080 | Instagram-Feed, X, Mastodon |
| `landscape` | 1920×1080 | YouTube, Discord-Ankündigung, Server-Listing |

Dazu `thumbnail-landscape`, `thumbnail-vertical` und `thumbnail-square` als Standbilder (Creek mit glimmenden Augen).
Jede Variante endet auf der Endkarte mit Server `onelitefeather.net` und Discord `1lf.link/discord`.

## Starten

Node 20 oder neuer.

```bash
cd marketing/trailer
npm ci
npm run studio                          # Vorschau im Browser, Props live editierbar
npm run render:all                      # alles nach out/<sprache>/<komposition>.mp4 bzw. .png
npm run render:all -- --lang=de --format=vertical --variant=creek
```

Remotion lädt beim ersten Render eine Headless-Chrome-Version herunter. Ohne Download-Zugang einen vorhandenen
Chromium angeben: `REMOTION_BROWSER_EXECUTABLE=/pfad/zu/headless_shell npm run render:all`.

## Neue Variante

1. Zeile in `src/variants.json`: `id`, `hook`, `hit` (Szene, auf der der Musik-Schlag liegt) und `scenes` mit Sekunden.
   Verfügbare Szenen: `coldOpen`, `hunter`, `pages`, `stamina`, `rotation`, `creekStalk`, `creekHunt`, `creekCatch`, `spectate`, `end`.
2. Neuer Hook: Text unter `hooks` in `src/props.ts` (Deutsch und Englisch), Schlüssel in `HookId` in `src/variants.ts`.
3. `npm run music` erzeugt die passende Musik, dann `npm run render:all -- --variant=<id>`.

## Pro Plattform rendern

Texte und Links stehen in `src/props.ts`. Wer messen will, welche Plattform Spieler bringt, rendert pro Plattform
einen eigenen Kurzlink:

```bash
echo '{"discordUrl":"1lf.link/tiktok"}' > tiktok.json
npm run render:all -- --format=vertical --props=tiktok.json
```

Weitere Props: `language` (`de`/`en`), `serverAddress`, `title` (Standard `SLENDER`), `availability` (leer = ausblenden),
`music` (`null` = ohne), `staticVolume` (0 = kein VHS-Rauschen), `footage`.

## Gameplay-Aufnahmen einsetzen

Ohne Aufnahmen rendern die Szenen einen prozeduralen Wald als Platzhalter. Aufnahmen kommen nach `public/footage/`
(nicht im Git, zu groß) und werden über `footage` eingebunden, z. B. `"footage":{"hunt":"footage/hunt.mp4"}`.

Shotlist, 1920×1080, 30 fps, mit aktivem Pack, Replay Mod oder OBS, je 5 bis 8 Sekunden:

| Slot | Inhalt |
|------|--------|
| `hunt` | Überlebender läuft durch enge Gassen, Kamera dreht sich um, nichts zu sehen |
| `pages` | Seite an einer Wand finden und einsammeln |
| `stamina` | Sprint, bis die XP-Leiste leer ist, Tunnel-Vision schließt sich |
| `creekStalk` | Der Creek in der Ferne zwischen Bäumen, beim Hinsehen verschwindet er |
| `creekHunt` | Der Creek bei der Jagd, Kamera schaut weg und wieder hin, er ist näher; Tab-Liste mit rotem ◆ offen |
| `creekCatch` | Erwischt werden: Wurf in die Luft (oder Platztausch) |
| `spectate` | Zuschauersicht über die Karte, die eigene Leiche im Bild |

Ton der Aufnahmen wird stummgeschaltet; die Tonspur mischt der Trailer selbst.

## Regeln für Änderungen

- **Creek:** im Trailer eine eigene, blockige Silhouette mit glimmenden Augen (`components/CreekFigure.tsx`), angelehnt an den Creaking, den das Spiel benutzt. „Manfred“ steht bisher nur in `config.properties.example`; die Variante `manfred` vor dem Posten freigeben lassen.
- **Name:** öffentlich „Slender“ (Prop `title`), Cygnus ist nur der Projektname. Kein Material aus dem Originalspiel (Logo, Grafiken, Sounds, Figurendesign) und nicht dessen Titel; alles im Trailer ist selbst gebaut. Risiko und Abwägung: SWOT T1/ST1.
- **Jugendschutz:** andeuten statt zeigen. Kein Blut-Overlay aus dem Pack, kein Gesicht, keine lauten Jumpscares. Bis die Altersempfehlung entschieden ist (PSR Abschnitt 6), bleibt das so.
- **Behauptungen:** jeder Satz muss sich auf ein vorhandenes Feature stützen. Keine Superlative. „Java Edition“ bleibt drin, solange es keinen Bedrock-Zugang gibt.
- **Marke:** Farben und Schrift aus `src/theme.ts` (OLF-Palette, Roboto). Das Feder-Logo liegt außerhalb aller Rausch- und Glitch-Ebenen: nicht verzerren, drehen, umfärben, weichzeichnen oder mit Schatten versehen.
- **Musik:** nur die selbst erzeugte Musik (siehe unten), eigene Produktionen oder CC0, sonst Content-ID-Sperren.

## Musik

Die Hintergrundmusik ist nicht heruntergeladen, sondern von `scripts/generate_music.py` synthetisiert: Drone mit
kleiner Sekunde und Tritonus, Wind, Spieluhr-Motiv, ein Herzschlag, der bis zum Schnitt in die Zuschauerszene
schneller wird, dann Stille und ein tiefer Schlag auf der `hit`-Szene. Sie gehört damit OneLiteFeather, ohne Lizenzbedingungen,
Namensnennung oder Content-ID-Risiko.

Jede Variante hat ihre eigene Musik, abgestimmt auf die Szenen aus `src/variants.json`. Nach einer Änderung dort
die Musik neu erzeugen (Python 3 mit numpy):

```bash
npm run music    # schreibt public/music/<variante>.ogg
```

Der Generator ist deterministisch, ein erneuter Lauf erzeugt dieselben Samples. `"music": null` rendert ohne Musik,
`"staticVolume": 0` schaltet das VHS-Rauschen an den Schnitten ab.

## Assets

`public/pack/` enthält Kopien aus `cygnus-pack` (Seiten-Texturen, Icons, Tunnel-Vision-Masken, Mond, VHS-Rauschen).
Nach Änderungen am Pack aktualisieren mit `npm run sync-pack -- /pfad/zu/cygnus-pack`.
`public/fonts/` enthält Roboto und Roboto Mono (SIL Open Font License, `OFL.txt`), lokal eingebunden, damit Renders offline und reproduzierbar laufen.
`public/brand/olf-logo.png` ist das OneLiteFeather-Feder-Logo.

## Lizenz von Remotion

Remotion ist für Einzelpersonen, gemeinnützige Projekte und Organisationen mit bis zu drei Mitarbeitenden kostenlos,
darüber braucht es eine [Company License](https://www.remotion.dev/license). OneLiteFeather als ehrenamtliches
Hobbyprojekt fällt in die kostenlose Stufe; bei einer Änderung der Organisationsform neu prüfen.
