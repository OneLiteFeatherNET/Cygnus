# Cygnus-Trailer

Trailer für Cygnus, gebaut mit [Remotion](https://www.remotion.dev): React-Komponenten, die zu
MP4 gerendert werden. Ein Storyboard, mehrere Formate, Termin und Link als Parameter.

| Komposition | Format | Länge | Zweck |
|-------------|--------|-------|-------|
| `CygnusTrailer` | 1920×1080 | ~64 s | YouTube, Blog, Website |
| `CygnusShort` | 1080×1920 | 30 s | TikTok, Shorts, Reels |
| `CygnusTeaser` | 1080×1920 | 15 s | Countdown, Stories |

Warum die Trailer so aufgebaut sind, steht in [`docs/swot.md`](docs/swot.md). Was aufgenommen
werden muss, in [`docs/shotlist.md`](docs/shotlist.md). Wo sie laufen und woran sie gemessen
werden, in [`docs/distribution.md`](docs/distribution.md).

## Loslegen

Node 20 oder neuer.

```
npm install
npm run pack-assets -- ../../cygnus-pack   # Pack-Sounds holen, Pfad zu einem cygnus-pack-Checkout
npm run studio                             # Vorschau im Browser, Props live editierbar
```

Fehlende Aufnahmen erscheinen als Platzhalter mit Beschreibung. Aufnahmen nach
`public/clips/<id>.mp4` legen, die IDs stehen in `src/clips.ts`.

## Rendern

```
npm run render                                   # alle drei nach out/
npm run render -- --props=props/tiktok.json      # mit Kanal-Link
npx remotion render src/index.ts CygnusShort out/short.mp4 --props=props/tiktok.json
```

Ohne Internetzugang für Chrome oder hinter einem Proxy einen vorhandenen Chromium mitgeben:
`--browser-executable=/pfad/zu/chrome-headless-shell`. Schriften liegen im Bundle, es wird zur
Renderzeit nichts nachgeladen.

## Aufbau

```
src/storyboard/   die drei Schnitte als Liste von Beats, plus Timing und Flacker-Prüfung
src/beats/        wie ein Beat aussieht: Clip, Text, Titel, Call to Action, End-Card
src/fx/           VHS-Rauschen, Glitch-Schnitt, Farbsaum auf Schrift
src/schema.ts     Props: Termin, Link, Texte, Musik
scripts/          Asset-Scan, Pack-Sounds kopieren, alle Varianten rendern
```

Neue Variante: Storyboard in `src/storyboard/` anlegen und in `src/Root.tsx` eintragen.

## Regeln, die der Code durchsetzt

- **Flackern:** Glitch-Schnitte müssen mindestens eine Sekunde auseinanderliegen und haben genau
  einen gedämpften Helligkeitspeak. Ein Storyboard, das das verletzt, lädt nicht
  (`src/storyboard/timeline.ts`). Die Warnung steht am Anfang jeder Variante.
- **Name:** öffentlich heißt der Modus nur Cygnus (PSR-CYGNUS, Abschnitt 2).
- **Marke:** OLF-Logo nur auf der End-Card, ohne Rauschen und Glitch darüber; Mojang-Disclaimer
  auf jeder End-Card.

Aufnahmen, Musik und die kopierten Pack-Sounds sind nicht im Repository (`.gitignore`).
