# Cygnus: SWOT, TOWS und Trailer-Ableitung

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

### Schwächen (intern, Ist)

| # | Schwäche | Beleg | Gewicht |
|---|----------|-------|---------|
| W1 | Braucht etwa vier Spieler gleichzeitig; Netzwerk-Spitze liegt bei rund 30 am Wochenende. Leere Lobby ist der wahrscheinlichste Grund, nicht wiederzukommen | PSR Abschnitt 4 | 3 |
| W2 | Nur Java Edition (Minestom, Java-Resource-Pack). Bedrock-Spieler des Netzwerks sind ausgeschlossen | `game/build.gradle.kts` | 2 |
| W3 | Kein Grund für Tag 2: keine Progression zwischen Runden | PSR Abschnitt 4 | 2 |
| W4 | Kleines Team, wenig Social-Media-Erfahrung, zwei aktive Streamer | Marketingkonzept | 2 |
| W5 | Rollen sind zufällig; wer zuerst als Jäger startet, versteht den Modus womöglich nicht | PSR Abschnitt 4 | 2 |
| W6 | Pack-Ablehnung führt zum Kick. Ohne Vorwarnung wirkt das wie ein Fehler | Blogpost | 2 |
| W7 | Bisher kein Bewegtbild, keine Server-Adresse und kein Invite pro Kanal | Annahme, in Outline nicht gefunden | 2 |

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
| **SO2** | Hochkant-Short mit Hook in den ersten zwei Sekunden, gebaut aus echten Pack-Assets | S1 × O3, O5 | Komposition `CygnusShort` |
| **SO3** | Creator-Nacht: Micro-Creator spielt mit eigener Community, wir stellen den Termin und Moderation | S2 × O2 | Abschnitt 3, Supporting Play B |
| **SO4** | Devlog für Tech-Kanäle: Horror-Effekte nur per Resource Pack, Code offen. Trailer als Aufmacher | S4 × O4 | Nach Launch, optional |
| **ST1** | Öffentlich nur „Cygnus“. Die Vorlage wird weder genannt noch gehashtagt; die Spielidee wird beschrieben | T1 | Texte in `src/props.ts`, Hashtag `#OLFCygnus` |
| **ST2** | Andeuten statt zeigen: kein Blut-Overlay, kein Gesicht, kein lauter Jumpscare im Trailer. Spannung über Ton, Rauschen, Silhouette für einzelne Frames | T2 | `scenes/Hunter.tsx`, `scenes/Spectate.tsx` |
| **ST3** | Verfügbarkeit als Zeitraum kommunizieren („Halloween 2026“), damit niemand im November einen leeren Modus erwartet | T3 | Prop `availability` |
| **ST4** | Nur eigene oder CC0-Musik; ohne Musik trägt das Pack-Rauschen den Ton | T5 | Prop `music`, README |
| **WO1** | Feste Rundentermine statt Dauerbetrieb. Jeder Call-to-Action führt zum Termin, nicht zu „jetzt joinen“ | W1 × O1, O2 | Prop `sessionHint`, Primary Bet |
| **WO2** | Szenen funktionieren jetzt mit Platzhaltern und nehmen Aufnahmen auf, sobald es sie gibt | W7 × O5 | Prop `footage`, Shotlist im README |
| **WO3** | Pro Kanal ein eigener Discord-Invite, gerendert als eigene Variante | W7 × O5 | Prop `discordUrl`, Abschnitt 4 |
| **WT1** | Erwartungen setzen: „Java Edition“ und „Nur ein Resource Pack“ stehen im Trailer, damit Pack-Kick und Bedrock-Ausschluss nicht überraschen | W2, W6 × T2 | Endkarte, `facts` |
| **WT2** | Ein Hauptkanal plus zwei Nebenkanäle, kein Rundumschlag | W4 × T3 | Abschnitt 3 |

Nicht durch den Trailer lösbar und vor dem Launch zu klären: **W3** (Grund für Tag 2) und **W5** (Erstspieler als Jäger).
Beides ist Spieldesign. Ein Trailer, der Spieler in eine Runde schickt, in der sie als Jäger nichts verstehen, kostet D1.

## 3. Entscheidung: wohin der Aufwand geht

**Primary Bet: Cygnus-Nächte über Discord.** Feste Termine in der Halloween-Woche (Vorschlag: Fr 24.10., Sa 25.10., Fr 31.10., jeweils 20 Uhr).
Der Teaser (16:9) läuft als Ankündigung im Discord, auf YouTube und im Server-Listing; jeder Call-to-Action zeigt auf den Termin.
Begründung: W1 hat das höchste Gewicht. Mehr Reichweite in einen leeren Modus verbrennt Erstkontakte.

**Supporting Play A: Short-Form.** `CygnusShort` auf genau einer Plattform (Vorschlag: YouTube Shorts, weil der Kanal ohnehin für
den Teaser gebraucht wird), zwei Varianten pro Woche bis 31.10. Variiert wird nur der Hook (`shortHook`), damit der Vergleich aussagekräftig bleibt.

**Supporting Play B: Creator-Nacht.** Drei bis fünf DACH-Micro-Creator anfragen, Ziel: ein Creator-Abend mit eigener Gruppe.
Anfragen bis 15.10., sonst ist der Termin vor Halloween nicht mehr planbar.

Bewusst nicht: TikTok, Instagram und Reddit parallel, große Streamer, bezahlte Werbung, Gewinnspiele.

## 4. Messung

| Kennzahl | Ziel bis 02.11. | Quelle |
|----------|-----------------|--------|
| Erstspieler mit zweiter Runde (primär, PSR Abschnitt 5) | Baseline erheben, Ziel ab zweiter Saison | `cygnus.player.join` mit `cygnus.join.outcome=spawned`, je `cygnus.player.uuid` über `cygnus.round.id` zählen |
| Abbruch beim ersten Join (Pack abgelehnt, W6) | Baseline erheben | `cygnus.player.kick` mit `cygnus.kick.reason`, `cygnus.join.outcome=abandoned` |
| Runden mit mindestens vier Spielern pro Cygnus-Nacht | mindestens 3 Runden je Termin | `cygnus.round` und zugehörige Joins |
| Discord-Joins pro Kanal | Teaser und Short einzeln ausweisen | eigener Invite pro Variante |
| Short: Anteil komplett gesehen | über 40 % | Plattform-Statistik |

Voraussetzung: Der OpenTelemetry-Agent läuft in der Halloween-Instanz (`docs/telemetry.md`), sonst sind alle Spans No-ops.

**Kill-Kriterien:** Short-Variante unter 20 % Wiedergabe bis zum Ende bei über 1.000 Aufrufen: Hook tauschen. Nach zwei Varianten unter fünf
messbaren Discord-Joins: Short-Form für diese Saison einstellen. Creator: nach zehn unbeantworteten Anfragen Ansatz ändern, nicht nachfassen.

**Review:** 02.11.2026. Ergebnis fließt in die nächste Fassung dieser Analyse und in PSR-CYGNUS Abschnitt 5.

## 5. Zeitplan bis Halloween

| Zeitraum | Schritt |
|----------|---------|
| bis 12.10. | Gameplay-Aufnahmen nach Shotlist (`marketing/trailer/README.md`), Server-Adresse und Invites je Kanal festlegen, Altersempfehlung entscheiden (PSR Abschnitt 6) |
| 13.10. | Teaser rendern, im Discord und auf YouTube veröffentlichen, Thumbnail aus `CygnusThumbnail` |
| 13.–31.10. | Short-Varianten zweimal pro Woche |
| bis 15.10. | Creator-Anfragen raus |
| 24., 25., 31.10. | Cygnus-Nächte |
| 02.11. | Review nach Abschnitt 4 |

## 6. Offene Punkte

1. Server-Adresse und Discord-Invites je Kanal (Props `serverAddress`, `discordUrl`).
2. Altersempfehlung und deren Kommunikation (PSR Abschnitt 6). Bis dahin bleibt der Trailer bei Andeutung statt Schock.
3. Namensentscheidung als ADR festhalten (PSR Abschnitt 2).
4. W3 und W5 im Spieldesign beantworten.
5. Musik: eigene Produktion oder CC0, sonst ohne.

## Prompt zum Fortschreiben

Die Analyse lässt sich mit einem LLM aktualisieren, indem man den Anchor beim Namen nennt:

> Aktualisiere die SWOT-Analyse (Semantic Anchor SWOT) für den Minecraft-Spielmodus Cygnus anhand der beigefügten Messwerte.
> Halte Stärken und Schwächen intern und auf den Ist-Zustand bezogen, Chancen und Risiken extern und zukunftsgerichtet.
> Belege oder markiere jeden Punkt als Annahme, gewichte ihn von 1 bis 3 und leite per TOWS (SO, ST, WO, WT) Maßnahmen ab,
> die jeweils auf ein Artefakt oder eine Kennzahl verweisen.
