# Shotlist

Jede Szene aus `src/clips.ts` wird einmal im Spiel aufgenommen und als
`public/clips/<id>.mp4` abgelegt. Solange die Datei fehlt, zeigt der Trailer an der Stelle einen
Platzhalter mit der Beschreibung. `npm run studio` ist damit gleichzeitig die Aufgabenliste.

## Aufnahme

- **Werkzeug:** ReplayMod (Fabric) für Kamerafahrten aus einer gespielten Runde, OBS für die
  Ego-Perspektive. Vorher in einer Testrunde prüfen, ob ReplayMod die Post-Effekte des Packs
  mitrendert; wenn nicht, die Glitch-Szenen mit OBS aufnehmen.
- **Format:** 1920×1080 oder größer, 60 fps, ohne HUD (F1), ohne Chat. Die 9:16-Varianten
  schneiden die Bildmitte aus, also das Wichtige mittig halten.
- **Länge:** pro Szene 8 bis 15 Sekunden Rohmaterial. Wo der Trailer einsetzt, steuert `from`
  (Sekunden) am Beat im Storyboard.
- **Mitspieler:** nur Team oder Leute, die zugestimmt haben, im Bild und mit Namen.

## Was nicht ins Bild kommt

Ergibt sich aus der SWOT (T1, T3, T4), nicht verhandelbar:

- Kein Blut-Overlay. Für die aufnehmende Person die Vollbild-Overlays abschalten oder Szenen
  ohne Treffer wählen.
- Keine Gore-Nahaufnahme, kein Schock-Standbild als Thumbnail.
- Der Begriff "Slender" nicht im Bild: keine Scoreboard-, Tab- oder Chat-Zeile, die ihn zeigt.
  Szenen mit sichtbarem Team-Tag (`font/tags/slender.png`) neu aufnehmen oder zuschneiden.
- Kein Minecraft-Logo, kein Mojang-Material.

## Szenen

| ID | Inhalt | Wo im Trailer |
|----|--------|---------------|
| `gaze-glitch` | Überlebender dreht sich um, der Jäger steht nah im Bild, der Glitch reißt das Bild auf | Hook in allen drei Varianten |
| `forest-walk` | Langsamer Gang durch Nebel, Taschenlampenkegel, nichts passiert | Einstieg Trailer und Teaser |
| `hunter-reveal` | Jäger wird kurz sichtbar zwischen Bäumen, verschwindet wieder | Trailer |
| `page-pickup` | Seite an einem Baum entdeckt und eingesammelt, Chime hörbar | Trailer, Short |
| `stamina-run` | Sprint, Ausdauerleiste läuft leer, Spieler wird langsam | Trailer, Short |
| `adrenaline` | Jäger taucht direkt vor einem Überlebenden auf, Herzschlag, Flucht | Trailer, Short |
| `creek-stalk` | Die zweite Gestalt folgt in Distanz, Spieler bemerkt sie spät | Trailer |
| `creek-throw` | Die zweite Gestalt erwischt einen Überlebenden und wirft ihn in die Luft | Trailer, Short |
| `corpse` | Zuschauerperspektive: der eigene Körper liegt dort, wo man gefallen ist | Trailer |
| `last-page` | Letzte Seite, alle Überlebenden rennen, Runde endet | Trailer |
| `group-lobby` | Gruppe von vier oder mehr Spielern in der Lobby, Countdown läuft | Trailer, vor dem Call to Action |

Die Trailer sind stumm geschnitten (`muted`); Ton kommt aus den Pack-Sounds und optional einer
Musikspur. Wer Originalton will, entfernt `muted` in `beats/Clip.tsx` und pegelt per Szene.

## Musik

Nur Material mit klarer Lizenz für Werbung auf allen Plattformen: selbst gemacht, CC0 oder eine
Lizenz, die kommerzielle Nutzung und Social Media abdeckt. Datei nach `public/audio/` und den
Pfad als Prop `music` setzen, z. B. `"music": "audio/theme.ogg"`.
