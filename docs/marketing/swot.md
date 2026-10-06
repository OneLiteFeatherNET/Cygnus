# Slender (Projekt Cygnus): SWOT, TOWS und Trailer-Ableitung

Stand: 06.10.2026. Grundlage: Konzept *Cygnus* (Outline, Konzepte - MiniGames), *PSR-CYGNUS* (Entwurf),
Blogpost *Halloween steht vor der Tür*, OLF-*Marketingkonzept* (Netzwerk-SWOT), dieses Repository und `cygnus-pack`.

Methode: Semantic Anchor [SWOT](https://llm-coding.github.io/Semantic-Anchors/anchor/swot) (Humphrey, SRI).
Stärken und Schwächen sind intern und beschreiben den Ist-Zustand; Chancen und Risiken sind extern und
beschreiben, was kommt. Der Anchor nennt als Hauptkritik lange, ungewichtete und nie geprüfte Listen
(Hill & Westbrook). Deshalb gilt hier:

- Jeder Punkt hat einen **Beleg** (Datei, Dokument) oder ist als **Annahme** markiert.
- Jeder Punkt hat ein **Gewicht** von 1 bis 3 (Wirkung auf Erstspieler, die eine zweite Runde spielen).
- Erst die **TOWS-Kreuzung** (SO, ST, WO, WT) erzeugt Maßnahmen. Jede Maßnahme verweist auf das Artefakt, das sie umsetzt.

## 1. SWOT

### Stärken (intern, Ist)

| # | Stärke | Beleg | Gewicht |
|---|--------|-------|---------|
| S1 | Horror-Atmosphäre ohne Mod: Pflicht-Resource-Pack mit Post-Effekten, Tunnel-Vision, VHS-Rauschen | `cygnus-pack`, `docs/post-effects.md` | 3 |
| S2 | Kurze Runden (unter zehn Minuten), ein Jäger gegen mehrere: passt zu Gruppen und Streams | Konzept, PSR Abschnitt 1 | 3 |
| S3 | Wiederspielwert: mehr Spawnpunkte als Seiten, Seiten rotieren per TTL | Konzept, Abschnitt *Seiten* | 2 |
| S4 | Open Source (AGPL-3.0) und eigene Telemetrie: Erfolg ist messbar, Tech ist vorzeigbar | `LICENSE`, `docs/telemetry.md` | 2 |
| S5 | Ehrliche Vorgeschichte: Version 2023 scheiterte an Balancing, die Neuauflage benennt das offen | Blogpost | 1 |
| S6 | Tote bleiben dabei: Zuschauermodus, die Leiche bleibt liegen | README, PSR Abschnitt 3 | 1 |
| S7 | Der Creek („Manfred“): eine zweite, vom Server gesteuerte Figur. Verfolgt einzelne Überlebende (nur das Opfer sieht ihn), bewegt sich bei der Jagd nur, wenn man wegschaut, wirft Gefangene in die Luft oder tauscht ihren Platz mit einem anderen. Die Mechanik lässt sich in einem Satz erklären und in Sekunden zeigen | `game/…/creek`, `config.properties.example` | 3 |

### Schwächen (intern, Ist)

| # | Schwäche | Beleg | Gewicht |
|---|----------|-------|---------|
| W1 | Braucht etwa vier Spieler gleichzeitig; Netzwerk-Spitze liegt bei rund 30 am Wochenende. Leere Lobby ist der wahrscheinlichste Grund, nicht wiederzukommen | PSR Abschnitt 4 | 3 |
| W2 | Nur Java Edition (Minestom, Java-Resource-Pack). Bedrock-Spieler des Netzwerks sind ausgeschlossen | `game/build.gradle.kts` | 2 |
| W3 | Kein Grund für Tag 2: keine Progression zwischen Runden | PSR Abschnitt 4 | 2 |
| W4 | Kleines Team, wenig Social-Media-Erfahrung, zwei aktive Streamer | Marketingkonzept | 2 |
| W5 | Rollen sind zufällig; wer zuerst als Jäger startet, versteht den Modus womöglich nicht | PSR Abschnitt 4 | 2 |
| W6 | Pack-Ablehnung führt zum Kick. Ohne Vorwarnung wirkt das wie ein Fehler | Blogpost | 2 |
| W7 | Bisher kein Gameplay-Material; Trailer laufen mit Platzhaltern. Ein Discord-Link für alle Kanäle (`1lf.link/discord`), Herkunft der Joins nicht unterscheidbar | Stand 06.10. | 2 |
| W8 | Keine festen Rundenzeiten (Entscheidung des Teams). Wer über einen Post kommt, trifft zufällig auf eine volle oder leere Lobby | Teamentscheidung 06.10. | 3 |

### Chancen (extern, Zukunft)

| # | Chance | Beleg | Gewicht |
|---|--------|-------|---------|
| O1 | Halloween-Saison: Nachfrage nach Horror-Content und Event-Modi im Oktober | Blogpost, Annahme zur Nachfrage | 3 |
| O2 | DACH-Micro-Creator (unter 1k Zuschauer) brauchen Gruppenformate und bringen ihre eigene Gruppe mit | Annahme | 3 |
| O3 | Short-Form-Video (TikTok, Shorts, Reels) belohnt einen Hook in den ersten zwei Sekunden; Found-Footage-Ästhetik funktioniert dort | Annahme | 2 |
| O4 | Tech-Publikum (r/admincraft, Minestom-Community, Mastodon) interessiert sich für Horror-Effekte, die nur mit einem Vanilla-Resource-Pack gebaut sind | Annahme | 1 |
| O5 | Video als Code (Remotion): Varianten pro Kanal, Sprache und Invite-Link kosten einen Render, keine Schnittstunde | dieses Repo, `marketing/trailer` | 2 |

### Risiken (extern, Zukunft)

| # | Risiko | Beleg | Gewicht |
|---|--------|-------|---------|
| T1 | Marken- und Urheberrecht an der Vorlage | PSR Abschnitt 2 | 3 |
| T2 | Jugendschutz und Minecraft Usage Guidelines: Horror-Werbung muss für alle Altersgruppen vertretbar sein; Plattformen schränken Schockinhalte ein | PSR Abschnitt 6 | 3 |
| T3 | Saisonalität: nach dem 31.10. fällt das Interesse, der Modus verwaist | Annahme | 2 |
| T4 | Große Netzwerke fahren eigene Halloween-Events und binden die Aufmerksamkeit | Annahme | 1 |
| T5 | Musik-Urheberrecht und Content-ID sperren oder demonetarisieren den Trailer | Annahme | 2 |
| T6 | Minecraft-Updates brechen Shader und Post-Effekte des Packs | `docs/post-effects.md` (Pack) | 1 |

## 2. TOWS: Maßnahmen

| Feld | Maßnahme | Aus | Umsetzung |
|------|----------|-----|-----------|
| **SO1** | Kernbotschaft: Gruppen-Horror ohne Mod, eine Runde unter zehn Minuten, zu Halloween | S1, S2 × O1 | Trailer: Cold Open, Endkarte (`src/props.ts`) |
| **SO2** | Varianten mit je eigenem Hook in den ersten zwei Sekunden, in 9:16, 1:1 und 16:9, gebaut aus echten Pack-Assets | S1, S7 × O3, O5 | `src/variants.json`, `npm run render:all` |
| **SO5** | Der Creek als Aufhänger: „Es bewegt sich nur, wenn du wegschaust“ ist eine Regel, die man nach einem Satz verstanden hat und selbst ausprobieren will | S7 × O3 | Varianten `creek`, `manfred` |
| **SO3** | Creator-Runde: Micro-Creator spielt mit der eigenen Community, wir stellen Moderation und beantworten Fragen im Chat | S2, S7 × O2 | Abschnitt 3, Supporting Play |
| **SO4** | Devlog für Tech-Kanäle: Horror-Effekte nur per Resource Pack, Code offen. Trailer als Aufmacher | S4 × O4 | Nach Launch, optional |
| **ST1** | Öffentlicher Name ist „Slender“ (Entscheidung des Teams, weicht von der PSR-Empfehlung ab). Risiko begrenzen: kein Logo, keine Grafik, kein Sound und kein Figurendesign aus dem Originalspiel, nicht dessen Titel verwenden; alles selbst gebaut. Der Name ist eine Prop und mit einem Render änderbar | T1 | Prop `title` in `src/props.ts` |
| **ST2** | Andeuten statt zeigen: kein Blut-Overlay, kein Gesicht, kein lauter Jumpscare im Trailer. Spannung über Ton, Rauschen, Silhouette für einzelne Frames | T2 | `scenes/Hunter.tsx`, `scenes/Spectate.tsx` |
| **ST3** | Verfügbarkeit als Zeitraum kommunizieren („Halloween 2026“), damit niemand im November einen leeren Modus erwartet | T3 | Prop `availability` |
| **ST4** | Musik selbst synthetisieren statt lizenzieren: gehört OLF, kein Content-ID-Risiko, passt per Schnittdatei exakt auf die Szenen | T5 | `scripts/generate_music.py`, `public/music/` |
| **WO1** | Ankünfte bündeln statt Termine setzen: Posts zur Hauptspielzeit (abends, Wochenende) veröffentlichen, damit Neue auf Spieler treffen; Discord als Ort, an dem man sich zum Spielen findet | W1, W8 × O1, O3 | Abschnitt 3, CTA `discordUrl` |
| **WO2** | Szenen funktionieren jetzt mit Platzhaltern und nehmen Aufnahmen auf, sobald es sie gibt | W7 × O5 | Prop `footage`, Shotlist im README |
| **WO3** | Pro Plattform ein eigener Kurzlink (z. B. `1lf.link/tiktok`), falls der Kurzlink-Dienst Klicks zählt, gerendert per `--props` | W7 × O5 | Prop `discordUrl`, Abschnitt 4 |
| **WT1** | Erwartungen setzen: „Java Edition“ und „Nur ein Resource Pack“ stehen im Trailer, damit Pack-Kick und Bedrock-Ausschluss nicht überraschen | W2, W6 × T2 | Endkarte, `facts` |
| **WT2** | Breit posten ist nur tragbar, weil Varianten nichts kosten außer dem Hochladen. Was nach zwei Wochen nichts bringt, fliegt raus | W4 × T3 | Abschnitt 3, Kill-Kriterien |

Nicht durch den Trailer lösbar und vor dem Launch zu klären: **W3** (Grund für Tag 2) und **W5** (Erstspieler als Jäger).
Beides ist Spieldesign. Ein Trailer, der Spieler in eine Runde schickt, in der sie als Jäger nichts verstehen, kostet D1.

## 3. Entscheidung: wohin der Aufwand geht

**Primary Bet: Short-Form breit streuen, mit Varianten statt Einzelvideo.** Das Team will Social Media breit bespielen.
Mit einem kleinen Team ist das sonst der klassische Fehler; hier ist der Aufwand pro Post aber nur das Hochladen, weil
`npm run render:all` jede Variante in jedem Format liefert. Kanäle: TikTok, YouTube Shorts, Instagram Reels (9:16),
Instagram-Feed und X/Mastodon (1:1), YouTube und Discord-Ankündigung (16:9).

| Variante | Hook | Aufhänger |
|----------|------|-----------|
| `creek` | Es bewegt sich nur, wenn du wegschaust. | Creek: Verfolgen, Jagd, Erwischt (S7) |
| `manfred` | Das ist Manfred. Hör nicht auf, ihn anzusehen. | Creek mit Namen, Meme-Tonfall |
| `hunter` | Du hast ihn nicht gesehen. Er dich schon. | Unsichtbarer Jäger (S2) |
| `pages` | Die Seite war gerade noch da. | Seiten-Rotation (S3) |
| `teaser` | Eine Runde. Unter zehn Minuten. | Alles, 31 s, für YouTube und Discord |

Rhythmus bis 31.10.: pro Plattform eine Variante alle zwei bis drei Tage, zur Hauptspielzeit (WO1), Reihenfolge `creek`, `hunter`,
`manfred`, `pages`, dann die beste wiederholen. Gleichzeitig dieselbe Variante auf allen Plattformen, damit die Plattformen vergleichbar bleiben.

**Supporting Play: Creator.** Drei bis fünf DACH-Micro-Creator anfragen, ob sie eine Runde mit ihrer Community spielen; der Creek ist
für Streams gemacht, weil der Chat sieht, was der Streamer nicht sieht. Anfragen bis 15.10.

Bewusst nicht: große Streamer, bezahlte Werbung, Gewinnspiele.

## 4. Messung

| Kennzahl | Ziel bis 02.11. | Quelle |
|----------|-----------------|--------|
| Erstspieler mit zweiter Runde (primär, PSR Abschnitt 5) | Baseline erheben, Ziel ab zweiter Saison | `cygnus.player.join` mit `cygnus.join.outcome=spawned`, je `cygnus.player.uuid` über `cygnus.round.id` zählen |
| Abbruch beim ersten Join (Pack abgelehnt, W6) | Baseline erheben | `cygnus.player.kick` mit `cygnus.kick.reason`, `cygnus.join.outcome=abandoned` |
| Runden mit mindestens vier Spielern pro Tag | steigend über den Oktober | `cygnus.round` und zugehörige Joins |
| Discord-Joins pro Plattform | je Plattform ausweisen | eigener Kurzlink pro Plattform (WO3) |
| Wiedergabe bis zum Ende, je Variante | über 40 % | Plattform-Statistik |

Voraussetzung: Der OpenTelemetry-Agent läuft in der Halloween-Instanz (`docs/telemetry.md`), sonst sind alle Spans No-ops.

**Kill-Kriterien:** Variante unter 20 % Wiedergabe bis zum Ende bei über 1.000 Aufrufen: nicht wiederholen. Plattform nach zwei Wochen unter
fünf messbaren Discord-Joins: dort aufhören. Variante mit dreifachem Durchschnitt: Hook-Abwandlungen davon rendern (neue Zeile in
`src/variants.json`, neuer Text in `src/props.ts`). Creator: nach zehn unbeantworteten Anfragen Ansatz ändern, nicht nachfassen.

**Review:** 02.11.2026. Ergebnis fließt in die nächste Fassung dieser Analyse und in PSR-CYGNUS Abschnitt 5.

## 5. Zeitplan bis Halloween

| Zeitraum | Schritt |
|----------|---------|
| bis 12.10. | Gameplay-Aufnahmen nach Shotlist (`marketing/trailer/README.md`), Kurzlinks je Plattform anlegen, Altersempfehlung entscheiden (PSR Abschnitt 6) |
| 13.10. | Alle Varianten neu rendern, `teaser` auf YouTube und im Discord, erste `creek`-Variante auf allen Plattformen |
| 13.–31.10. | Varianten im Wechsel, alle zwei bis drei Tage, zur Hauptspielzeit |
| bis 15.10. | Creator-Anfragen raus |
| 20.10. | Zwischenstand: Kill- und Double-down-Kriterien anwenden |
| 02.11. | Review nach Abschnitt 4 |

## 6. Offene Punkte

1. Kurzlinks je Plattform, falls `1lf.link` Klicks zählt; sonst ist die Herkunft der Joins nicht messbar.
2. Altersempfehlung und deren Kommunikation (PSR Abschnitt 6). Bis dahin bleibt der Trailer bei Andeutung statt Schock.
3. Namensentscheidung „Slender“ als ADR festhalten und PSR-CYGNUS Abschnitt 2 anpassen; der Record empfiehlt bisher das Gegenteil.
4. W3 und W5 im Spieldesign beantworten.
5. Ob „Manfred“ öffentlich genutzt werden soll; der Name steht bisher nur in `config.properties.example`.

## Prompt zum Fortschreiben

Die Analyse lässt sich mit einem LLM aktualisieren, indem man den Anchor beim Namen nennt:

> Aktualisiere die SWOT-Analyse (Semantic Anchor SWOT) für den Minecraft-Spielmodus Slender (Projekt Cygnus) anhand der beigefügten Messwerte.
> Halte Stärken und Schwächen intern und auf den Ist-Zustand bezogen, Chancen und Risiken extern und zukunftsgerichtet.
> Belege oder markiere jeden Punkt als Annahme, gewichte ihn von 1 bis 3 und leite per TOWS (SO, ST, WO, WT) Maßnahmen ab,
> die jeweils auf ein Artefakt oder eine Kennzahl verweisen.
