// The shot list. Every entry is a recording the team makes in game and drops into
// public/clips/<id>.mp4. Until a file exists the trailer draws a placeholder carrying
// the description below, so the cut can be timed before anything is recorded.
//
// Rules for every shot, see docs/shotlist.md for the why:
// - no blood overlay in frame (cygnus.overlays off for the recording player)
// - no HUD, no chat, no name tags of real players without their consent
// - 1920x1080 or higher, 60 fps, so both 16:9 and the 9:16 crop stay sharp
export const clips = {
  'gaze-glitch': 'Überlebender dreht sich um, der Jäger steht nah im Bild, der Glitch reißt das Bild auf',
  'forest-walk': 'Langsamer Gang durch Nebel, Taschenlampenkegel, nichts passiert',
  'hunter-reveal': 'Jäger wird kurz sichtbar zwischen Bäumen, verschwindet wieder',
  'page-pickup': 'Seite an einem Baum entdeckt und eingesammelt, Chime hörbar',
  'stamina-run': 'Sprint, Ausdauerleiste läuft leer, Spieler wird langsam',
  'adrenaline': 'Jäger taucht direkt vor einem Überlebenden auf, Herzschlag, Flucht',
  'creek-throw': 'Die zweite Gestalt erwischt einen Überlebenden und wirft ihn in die Luft',
  'creek-stalk': 'Die zweite Gestalt folgt in Distanz, Spieler bemerkt sie spät',
  'corpse': 'Zuschauerperspektive: der eigene Körper liegt dort, wo man gefallen ist',
  'group-lobby': 'Gruppe von 4+ Spielern in der Lobby, Countdown läuft',
  'last-page': 'Letzte Seite, alle Überlebenden rennen, Runde endet',
} as const;

export type ClipId = keyof typeof clips;
