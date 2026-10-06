import type {Storyboard} from './types';

// 9:16, 30 seconds. TikTok, YouTube Shorts, Instagram Reels.
// The first second decides whether anyone stays, so it opens on the glitch, not on
// a logo or a warning screen.
export const shortTrailer: Storyboard = {
  vertical: true,
  warningAsBanner: true,
  beats: [
    {kind: 'clip', clip: 'gaze-glitch', seconds: 2.5, caption: 'Schau ihn nicht an.'},
    {kind: 'text', lines: ['Einer jagt.', 'Unsichtbar.'], seconds: 2.5, cut: 'glitch'},
    {kind: 'clip', clip: 'page-pickup', seconds: 3.5, caption: 'Ihr sammelt Seiten.', cut: 'cut'},
    {kind: 'clip', clip: 'stamina-run', seconds: 3, caption: 'Rennen kostet Ausdauer.', cut: 'cut'},
    {kind: 'clip', clip: 'adrenaline', seconds: 3, cut: 'glitch'},
    {kind: 'text', lines: ['Er ist', 'nicht allein.'], seconds: 2.5, cut: 'glitch'},
    {kind: 'clip', clip: 'creek-throw', seconds: 3, cut: 'cut'},
    {kind: 'title', seconds: 3, cut: 'glitch'},
    {kind: 'cta', seconds: 3.5, cut: 'cut'},
    {kind: 'end', seconds: 3, cut: 'cut'},
  ],
};
