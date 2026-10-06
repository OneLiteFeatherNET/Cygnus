// Alles, was sich pro Kampagne ändert, steht hier und kann per `--props` überschrieben werden.
// Öffentlich heißt der Modus "Slender" (Prop `title`); Cygnus ist der Projektname im Code.
import type {HookId} from './variants';

export type Language = 'de' | 'en';

export type Footage = {
  // Pfade relativ zu public/, z. B. "footage/hunt.mp4". Fehlt ein Clip, rendert die Szene
  // einen prozeduralen Platzhalter, damit der Schnitt auch ohne Aufnahmen abnahmefähig bleibt.
  hunt?: string;
  pages?: string;
  stamina?: string;
  creekStalk?: string;
  creekHunt?: string;
  creekCatch?: string;
  spectate?: string;
};

export type TrailerProps = {
  language: Language;
  serverAddress: string;
  // Für Kanal-Zuordnung pro Plattform einen eigenen Kurzlink rendern, z. B. 1lf.link/tiktok.
  discordUrl: string;
  // Zeitraum, nicht Datum: der Modus ist laut Blogpost "im Halloween-Zeitraum spielbar". Leer = ausblenden.
  availability: string;
  footage: Footage;
  // Name des Modus in Titel und Thumbnail. Cygnus ist nur der Projektname.
  title: string;
  // Musik relativ zu public/. Ohne Angabe die selbst erzeugte Musik der Variante
  // (scripts/generate_music.py), null = ohne Musik. Fremde Musik nur mit Lizenz oder CC0 (Content-ID).
  music?: string | null;
  // Lautstärke der VHS-Rausch-Sounds aus dem Pack an den Schnitten, 0 = aus.
  staticVolume: number;
};

export const defaultProps: TrailerProps = {
  language: 'de',
  serverAddress: 'onelitefeather.net',
  discordUrl: '1lf.link/discord',
  availability: 'Halloween 2026',
  footage: {},
  title: 'SLENDER',
  staticVolume: 0.35,
};

type Lines = [string, string];

type Copy = {
  hooks: Record<HookId, string>;
  hunter: Lines;
  pages: Lines;
  stamina: Lines;
  rotation: Lines;
  creekStalk: Lines;
  creekHunt: Lines;
  creekCatch: Lines;
  spectate: Lines;
  claim: string;
  facts: string[];
  ctaServer: string;
  ctaDiscord: string;
};

// Jeder Satz geht auf ein belegtes Feature zurück (README, Konzept, game/…/creek). Keine Superlative.
export const copy: Record<Language, Copy> = {
  de: {
    hooks: {
      round: 'Eine Runde. Unter zehn Minuten.',
      hunter: 'Du hast ihn nicht gesehen.\nEr dich schon.',
      creek: 'Es bewegt sich nur,\nwenn du wegschaust.',
      manfred: 'Das ist Manfred.\nHör nicht auf, ihn anzusehen.',
      pages: 'Die Seite war gerade noch da.',
    },
    hunter: ['Einer jagt.', 'Unsichtbar.'],
    pages: ['Ihr sammelt Seiten.', 'Je mehr ihr seid, desto mehr.'],
    stamina: ['Rennen kostet Ausdauer.', 'Leer ist leer.'],
    rotation: ['Die Seiten wandern.', 'Auswendiglernen hilft nicht.'],
    creekStalk: ['Etwas folgt dir.', 'Nur du kannst es sehen.'],
    creekHunt: ['Schau hin, und es erstarrt.', 'Schau weg, und es kommt näher.'],
    creekCatch: ['Erwischt.', 'Ab in die Luft. Oder du tauschst den Platz.'],
    spectate: ['Wer stirbt, schaut zu.', 'Die Leiche bleibt liegen.'],
    claim: 'Asymmetrischer Horror in Minecraft',
    facts: ['Kein Mod. Nur ein Resource Pack.', 'Java Edition', 'Open Source'],
    ctaServer: 'Server',
    ctaDiscord: 'Discord',
  },
  en: {
    hooks: {
      round: 'One round. Under ten minutes.',
      hunter: 'You didn’t see him.\nHe saw you.',
      creek: 'It only moves\nwhen you look away.',
      manfred: 'This is Manfred.\nDon’t stop looking at him.',
      pages: 'The page was right there.',
    },
    hunter: ['One hunts.', 'Unseen.'],
    pages: ['You collect pages.', 'More players, more pages.'],
    stamina: ['Running costs stamina.', 'Empty means empty.'],
    rotation: ['The pages move.', 'Memorising won’t help.'],
    creekStalk: ['Something follows you.', 'Only you can see it.'],
    creekHunt: ['Look at it, and it freezes.', 'Look away, and it comes closer.'],
    creekCatch: ['Caught.', 'Up you go. Or you swap places.'],
    spectate: ['Die, and you watch.', 'Your body stays behind.'],
    claim: 'Asymmetric horror in Minecraft',
    facts: ['No mod. Just a resource pack.', 'Java Edition', 'Open source'],
    ctaServer: 'Server',
    ctaDiscord: 'Discord',
  },
};
