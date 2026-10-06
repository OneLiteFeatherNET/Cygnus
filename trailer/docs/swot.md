# SWOT: Cygnus-Trailer, Halloween 2026

Gegenstand ist nicht OneLiteFeather als Ganzes (dafür gibt es die SWOT im Outline-Dokument
*Marketingkonzept*), sondern eine Frage: **Wie bringen Trailer für Cygnus im Halloween-Fenster
2026 Spieler auf den Server, die eine zweite Runde spielen?**

Methode nach dem Semantic Anchor [SWOT](https://llm-coding.github.io/Semantic-Anchors/anchor/swot):
Stärken und Schwächen sind intern und beschreiben den Ist-Zustand, Chancen und Risiken sind extern
und blicken nach vorn. Der Anker nennt die Kritik gleich mit (Hill & Westbrook 1997: lange,
ungewichtete, nie geprüfte Listen, aus denen nie eine Handlung folgt). Deshalb hier:

- **Gewicht** 1 bis 3, wie stark der Punkt den Erfolg der Trailer beeinflusst.
- **Beleg**: woher die Aussage stammt. Ohne Beleg steht der Punkt als *Annahme* da.
- **TOWS** am Ende: jede Strategie leitet sich aus einem Quadranten-Paar ab und landet als
  konkrete Entscheidung im Code oder im Plan. Ein Punkt, aus dem nichts folgt, fliegt raus.

Stand: 06.10.2026, gut drei Wochen vor Halloween.

## Stärken (intern, jetzt)

| ID | Stärke | Gew. | Beleg |
|----|--------|------|-------|
| S1 | Eigene, sichtbare Atmosphäre: Post-Processing-Shader, VHS-Rauschen, Gaze-Glitch, Nebel pro Karte. Das ist Bildmaterial, das in den ersten Sekunden eines Videos trägt | 3 | `cygnus-pack/docs/post-effects.md`, Changelog 2.9.0 bis 2.14.0 |
| S2 | Überraschungsmomente, die sich filmen lassen: eine zweite Gestalt, die Überlebende verfolgt, in die Luft wirft oder mit anderen tauscht; Adrenalin mit Herzschlag; die Leiche bleibt liegen | 3 | `config.properties.example` (creek, adrenaline), Changelog 2.10.0 |
| S3 | Wiederspielbar: Seiten haben mehr Spawnpunkte als gefunden werden müssen und rotieren, Auswendiglernen trägt nicht | 2 | PSR-CYGNUS, Abschnitt 1 |
| S4 | Kein Mod nötig, nur das Resource-Pack, das der Server selbst ausliefert | 2 | `config.properties.example` (resourcePackUrl) |
| S5 | Open Source (AGPL-3.0), Code öffentlich. Glaubwürdig für Tech-Kanäle und als Beleg für "kein P2W" | 1 | `README.md`, `LICENSE` |
| S6 | Team mit Entwicklerprofil: Trailer als Code (Remotion) passt zu den vorhandenen Fähigkeiten statt Videoschnitt-Know-how vorauszusetzen | 2 | OLF-SWOT "breit gefächertes Wissen", "junges IT-erfahrenes Team" |

## Schwächen (intern, jetzt)

| ID | Schwäche | Gew. | Beleg |
|----|----------|------|-------|
| W1 | Rundenmodus mit Gruppenbedarf bei kleiner Spielerbasis. Wer allein in der Lobby steht, kommt nicht wieder | 3 | PSR-CYGNUS, Abschnitt 4 (Wochenendspitzen um 30 Spieler) |
| W2 | Es gibt noch kein Gameplay-Material. Jede Szene muss erst aufgenommen werden | 3 | Kein Video im Repo, im Pack oder im Outline gefunden |
| W3 | Wenig Erfahrung mit Social Media und Video | 2 | OLF-SWOT, Schwächen |
| W4 | Kein Grund für Tag 2: eine Runde lässt nichts zurück, das wartet | 2 | PSR-CYGNUS, Abschnitt 4 |
| W5 | Pflicht-Resource-Pack, Ablehnen führt zum Kick. Reibung beim ersten Join | 1 | Blogentwurf "Halloween steht vor der Tür", `config.properties.example` |
| W6 | Messung halb fertig: 1lf.link existiert, Plan-Plugin und Wochenreport noch nicht | 2 | Outline "1lf.link Slug- und Tag-Konvention", Folgeschritte |
| W7 | Rundenlänge widersprüchlich: PSR sagt "unter zehn Minuten", `gameTime` steht auf 900 s | 1 | PSR-CYGNUS vs. `config.properties.example` |

## Chancen (extern, nach vorn)

| ID | Chance | Gew. | Beleg |
|----|--------|------|-------|
| O1 | Halloween: im Oktober suchen Leute gezielt nach Horror-Inhalten, Mojang liefert selbst nichts | 3 | Blogentwurf "Halloween steht vor der Tür" |
| O2 | Horror mit Freunden ist ein etabliertes Stream-Format; kleine DACH-Creator brauchen im Oktober Inhalte | 2 | *Annahme*, vor Outreach mit 5 Creator-Kanälen prüfen |
| O3 | Kurzvideo-Plattformen belohnen einen starken ersten Moment, nicht Produktionsbudget | 2 | *Annahme*, siehe Skill-Referenz `dach-platforms.md` |
| O4 | Remotion rendert aus einer Quelle beliebig viele Varianten (Format, Sprache, Termin, Link) ohne Schnittprogramm | 2 | `remotion` 4.0, dieses Verzeichnis |
| O5 | Vorhandene Infrastruktur: 1lf.link mit Kanal-Tags, Blogentwurf, Discord | 2 | Outline "1lf.link Slug- und Tag-Konvention" |

## Risiken (extern, nach vorn)

| ID | Risiko | Gew. | Beleg |
|----|--------|------|-------|
| T1 | Namens- und Markenrecht: "Slender" und die Figur gehören Dritten. Ein Trailer mit dem Begriff kann gesperrt werden | 3 | PSR-CYGNUS, Abschnitt 2 |
| T2 | Fotosensitive Epilepsie: Glitch-Effekte flackern. Ohne Warnung und Begrenzung Gesundheitsrisiko und Plattform-Problem | 3 | Spiel warnt selbst (Changelog 2.11.0, `EpilepsyDisclaimer`); WCAG 2.3.1 |
| T3 | Jugendschutz und Minecraft Usage Guidelines: Werbung muss für alle Altersgruppen geeignet sein, Blut und Schock-Momente sind heikel | 3 | PSR-CYGNUS, Abschnitt 6 |
| T4 | Markenauftritt gegenüber Mojang: Werbung darf keine offizielle Verbindung andeuten | 2 | Minecraft Usage Guidelines (Disclaimer-Pflicht) |
| T5 | Im Oktober konkurrieren sehr viele Halloween-Inhalte um dieselbe Aufmerksamkeit | 2 | *Annahme* |
| T6 | Messwerte verzerrt: rund 78 % der 1lf.link-Aufrufe waren Bots | 2 | Outline "1lf.link …", Abschnitt Bot-Realität |

## TOWS: was daraus folgt

| Paar | Strategie | Umsetzung |
|------|-----------|-----------|
| **SO** S1 + O3 | Mit dem Glitch eröffnen, nicht mit Logo oder Titelkarte | `storyboard/short.ts` beginnt mit `gaze-glitch`, die Warnung läuft als Banner darüber |
| **SO** S2 + O1 | Die Überraschungen zeigen, nicht die Regeln erklären. Regeln in drei Sätzen, Rest Momente | Storyboards: "Einer jagt. Unsichtbar." / "Ihr sammelt Seiten." / "Er ist nicht allein." |
| **SO** S6 + O4 | Trailer als Code: Termin, Link und Texte sind Props, jede Variante ist ein Render-Befehl | `src/schema.ts`, `npm run render -- --props=…` |
| **ST** S1 + T2 | Flackern technisch begrenzen statt auf Disziplin zu hoffen: höchstens ein Helligkeitspeak pro Glitch, Glitches mindestens 1 s auseinander, Warnung am Anfang | `storyboard/timeline.ts` (`validate`), `fx/GlitchIn.tsx` |
| **ST** S3 + T1 | Die Mechanik beschreiben, die Herkunft nicht benennen. "Cygnus" ist der einzige Name, "Slender" kommt in keinem Text vor | Alle Texte in `storyboard/`, `schema.ts` |
| **ST** S5 + T4 | Am Ende: OLF signiert, Open-Source-Link, Mojang-Disclaimer | `beats/Beats.tsx` (`EndCard`) |
| **WO** W1 + O2 | Nicht "Komm vorbei", sondern feste Termine verkaufen und Gruppen ansprechen: "Fr & Sa 20 Uhr. Bring drei Freunde mit." | Prop `slot`, `callToAction` |
| **WO** W2 + O4 | Platzhalter statt warten: der Schnitt steht vor den Aufnahmen, jede fehlende Szene ist im Preview als Aufgabe sichtbar | `beats/Clip.tsx`, `docs/shotlist.md` |
| **WO** W6 + O5 | Pro Kanal ein eigener 1lf.link-Slug im Video, damit Klicks dem Kanal zugeordnet werden können | `docs/distribution.md` |
| **WT** W3 + T3 | Kein Blut, keine Gore-Nahaufnahme, keine Schock-Thumbnails. Spannung über Ton und Andeutung | `docs/shotlist.md`, Regeln oben |
| **WT** W1 + T5 | Keine breite Kampagne ohne Termine. Lieber wenige Kanäle und volle Runden als Reichweite auf leere Lobbys | `docs/distribution.md` |

## Offen, nicht hier zu entscheiden

- Namensentscheidung Cygnus gegen Slender als ADR festhalten (PSR-CYGNUS, offener Punkt 1).
  Die Trailer setzen die Empfehlung des PSR bereits um.
- Altersempfehlung für den Modus und wie sie im Trailer erscheint (PSR-CYGNUS, Abschnitt 6).
- Rundenlänge (W7) klären, bevor eine Zahl in einem Text auftaucht. Die Trailer nennen deshalb keine.
