// Alles, was sich pro Kampagne ändert, steht hier und kann per `--props` überschrieben werden.
// Öffentlich heißt der Modus "Slender" (Prop `title`); Cygnus ist der Projektname im Code.

export type Language = 'de' | 'en';

export type Footage = {
  // Pfade relativ zu public/, z. B. "footage/hunt.mp4". Fehlt ein Clip, rendert die Szene
  // einen prozeduralen Platzhalter, damit der Schnitt auch ohne Aufnahmen abnahmefähig bleibt.
  hunt?: string;
  pages?: string;
  stamina?: string;
  spectate?: string;
};

export type TrailerProps = {
  language: Language;
  // Pro Kanal eigene Adresse/Invite, damit Discord-Joins dem Kanal zugeordnet werden können
  // (z. B. discord.gg/xyz?utm_source=tiktok&utm_campaign=cygnus-halloween).
  serverAddress: string;
  discordUrl: string;
  // Zeitraum, nicht Datum: der Modus ist laut Blogpost "im Halloween-Zeitraum spielbar".
  availability: string;
  footage: Footage;
  // Name des Modus in Titel und Thumbnail. Cygnus ist nur der Projektname.
  title: string;
  // Musik relativ zu public/, null = ohne. Standard ist die selbst erzeugte Musik aus
  // scripts/generate_music.py; fremde Musik nur mit eigener Lizenz oder CC0 (Content-ID).
  music: string | null;
  // Lautstärke der VHS-Rausch-Sounds aus dem Pack an den Schnitten, 0 = aus.
  staticVolume: number;
  // Rundentermine statt Dauerbetrieb (PSR-CYGNUS, Abschnitt 4: leere Lobby ist der D1-Killer).
  sessionHint: string;
};

export const defaultProps: TrailerProps = {
  language: 'de',
  serverAddress: 'nachzutragen.onelitefeather.net',
  discordUrl: 'discord.gg/nachzutragen',
  availability: 'Halloween 2026',
  footage: {},
  title: 'SLENDER',
  music: 'music/cygnus-teaser.ogg',
  staticVolume: 0.35,
  sessionHint: 'Feste Runden-Termine im Discord',
};

type Copy = {
  coldOpen: string;
  hunter: [string, string];
  pages: [string, string];
  stamina: [string, string];
  rotation: [string, string];
  spectate: [string, string];
  claim: string;
  facts: string[];
  ctaServer: string;
  ctaDiscord: string;
  shortHook: string;
};

// Jeder Satz geht auf ein belegtes Feature zurück (README, Konzept, PSR). Keine Superlative.
export const copy: Record<Language, Copy> = {
  de: {
    coldOpen: 'Eine Runde. Unter zehn Minuten.',
    hunter: ['Einer jagt.', 'Unsichtbar.'],
    pages: ['Ihr sammelt Seiten.', 'Je mehr ihr seid, desto mehr.'],
    stamina: ['Rennen kostet Ausdauer.', 'Leer ist leer.'],
    rotation: ['Die Seiten wandern.', 'Auswendiglernen hilft nicht.'],
    spectate: ['Wer stirbt, schaut zu.', 'Die Leiche bleibt liegen.'],
    claim: 'Asymmetrischer Horror in Minecraft',
    facts: ['Kein Mod. Nur ein Resource Pack.', 'Java Edition', 'Open Source'],
    ctaServer: 'Server',
    ctaDiscord: 'Discord',
    shortHook: 'Du hast ihn nicht gesehen.\nEr dich schon.',
  },
  en: {
    coldOpen: 'One round. Under ten minutes.',
    hunter: ['One hunts.', 'Unseen.'],
    pages: ['You collect pages.', 'More players, more pages.'],
    stamina: ['Running costs stamina.', 'Empty means empty.'],
    rotation: ['The pages move.', 'Memorising won’t help.'],
    spectate: ['Die, and you watch.', 'Your body stays behind.'],
    claim: 'Asymmetric horror in Minecraft',
    facts: ['No mod. Just a resource pack.', 'Java Edition', 'Open source'],
    ctaServer: 'Server',
    ctaDiscord: 'Discord',
    shortHook: 'You didn’t see him.\nHe saw you.',
  },
};
