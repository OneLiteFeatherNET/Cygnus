import type {Storyboard} from './types';

// 16:9, about a minute. YouTube, Discord announcement, website.
// Order follows AIDA: the first shot has to hold someone (Attention), the rules
// make it understandable (Interest), the surprises make it wanted (Desire), and
// the end asks for exactly one thing (Action).
export const mainTrailer: Storyboard = {
  vertical: false,
  warningAsBanner: false,
  beats: [
    {kind: 'warning', seconds: 3},
    // Attention
    {kind: 'clip', clip: 'forest-walk', seconds: 4, cut: 'cut'},
    {kind: 'clip', clip: 'gaze-glitch', seconds: 2.5, cut: 'glitch'},
    {kind: 'text', lines: ['Einer jagt.', 'Unsichtbar.'], seconds: 3, cut: 'glitch'},
    // Interest
    {kind: 'clip', clip: 'hunter-reveal', seconds: 4, cut: 'cut'},
    {kind: 'text', lines: ['Ihr sammelt Seiten.'], seconds: 2.5, cut: 'cut'},
    {kind: 'clip', clip: 'page-pickup', seconds: 4, caption: 'Folgt dem Klang.', cut: 'cut'},
    {kind: 'clip', clip: 'stamina-run', seconds: 4, caption: 'Rennen kostet Ausdauer.', cut: 'cut'},
    // Desire
    {kind: 'text', lines: ['Er hört jede Seite,', 'die verschwindet.'], seconds: 3, cut: 'glitch'},
    {kind: 'clip', clip: 'adrenaline', seconds: 3.5, cut: 'cut'},
    {kind: 'text', lines: ['Und er ist', 'nicht allein.'], seconds: 3, cut: 'glitch'},
    {kind: 'clip', clip: 'creek-stalk', seconds: 3, cut: 'cut'},
    {kind: 'clip', clip: 'creek-throw', seconds: 3, cut: 'cut'},
    {kind: 'clip', clip: 'corpse', seconds: 3.5, caption: 'Wer fällt, bleibt liegen.', cut: 'glitch'},
    {kind: 'clip', clip: 'last-page', seconds: 3, cut: 'cut'},
    // Action
    {kind: 'title', seconds: 3.5, subtitle: 'Asymmetrischer Horror auf OneLiteFeather', cut: 'glitch'},
    {kind: 'clip', clip: 'group-lobby', seconds: 2.5, cut: 'cut'},
    {kind: 'cta', seconds: 4.5, cut: 'cut'},
    {kind: 'end', seconds: 4, cut: 'cut'},
  ],
};
