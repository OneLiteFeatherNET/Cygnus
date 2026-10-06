# Verteilung und Messung

Entscheidung aus der SWOT (WO W1+O2, WT W1+T5): Die Trailer verkaufen **feste Gruppenrunden**,
nicht den Modus an sich. Ein Kanal wird bespielt, zwei laufen mit, der Rest bleibt passiv.

| Rolle | Kanal | Variante | Slug |
|-------|-------|----------|------|
| Primär | TikTok, gespiegelt auf YouTube Shorts und Reels | `CygnusShort`, später `CygnusTeaser` als Countdown | `1lf.link/tk-cygnus-okt` |
| Begleitend | YouTube (Langform) und Blogartikel "Halloween steht vor der Tür" | `CygnusTrailer` eingebettet | `1lf.link/yt-cygnus-okt` |
| Begleitend | Discord-Ankündigung im eigenen Server, mit Event-Terminen | `CygnusTeaser` + Link auf YouTube | Event-Link, kein Slug nötig |

Discord bekommt den Link auf YouTube, nicht die Datei: die Master-Renders sind wegen des Rauschens
mehrere hundert MB groß.

## Slugs anlegen

Nach der Outline-Konvention *1lf.link Slug- und Tag-Konvention*, vor dem ersten Post:

| Slug | Long URL | Tags |
|------|----------|------|
| `tk-cygnus-okt` | `https://discord.onelitefeather.net/?utm_source=tiktok&utm_medium=video&utm_campaign=cygnus-okt` | `kanal:tiktok`, `funnel:trial`, `track:retention` |
| `yt-cygnus-okt` | `https://discord.onelitefeather.net/?utm_source=youtube&utm_medium=video&utm_campaign=cygnus-okt` | `kanal:youtube`, `funnel:trial`, `track:retention` |

Den Slug als Prop `link` in `props/<kanal>.json` setzen und je Kanal rendern:

```
npm run render -- --props=props/tiktok.json
```

`track:retention` ist eine Annahme: die Konvention kennt noch keinen Track für Spielmodi. Wer
`track:cygnus` anlegen will, ergänzt das zuerst im Outline-Dokument.

## Woran gemessen wird

Nicht Views, nicht Follower. Primäre Kennzahl aus PSR-CYGNUS: **Anteil der Erstspieler, die eine
zweite Runde spielen.** Dazu:

| Kennzahl | Quelle | Schwelle |
|----------|--------|----------|
| Termin-Auslastung: Runden mit mindestens vier Spielern pro angekündigtem Termin | Server-Log, manuell gezählt | unter 50 % nach zwei Wochenenden: weniger Termine, nicht mehr Werbung |
| Echte Klicks pro Slug | Shlink, nach Bot-Filter | unter 5 Discord-Joins pro Kanal nach zwei Wochen: Kanal stoppen |
| Zweite Runde von Erstspielern | Server-Daten, sobald erhoben | erste Messung ist die Baseline |

Rohzahlen aus Shlink ohne Bot-Filter werden nicht berichtet (rund 78 % Bots im Mai 2026).

## Ablauf bis Halloween

| Wann | Was |
|------|-----|
| bis 12.10. | Slugs anlegen, Termine im Discord als Events, Testrunde mit ReplayMod |
| bis 17.10. | Szenen aufnehmen, `npm run studio` bis kein Platzhalter mehr sichtbar ist |
| ab 19.10. | Teaser als Countdown, Short auf TikTok, Trailer auf YouTube und im Blog |
| 31.10. | Gruppenrunden laufen |
| 07.11. | Auswertung nach der Tabelle oben, Ergebnis ins PSR-CYGNUS, Abschnitt 5 |
