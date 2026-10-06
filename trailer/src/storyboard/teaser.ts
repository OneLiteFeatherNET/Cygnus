import type {Storyboard} from './types';

// 9:16, 15 seconds. Story/Status formats and the Discord countdown post.
export const teaser: Storyboard = {
  vertical: true,
  warningAsBanner: true,
  beats: [
    {kind: 'clip', clip: 'forest-walk', seconds: 2.5},
    {kind: 'clip', clip: 'gaze-glitch', seconds: 2, cut: 'glitch'},
    {kind: 'text', lines: ['Einer jagt.', 'Ihr sammelt.'], seconds: 2.5, cut: 'glitch'},
    {kind: 'title', seconds: 2.5, cut: 'glitch'},
    {kind: 'cta', seconds: 3, cut: 'cut'},
    {kind: 'end', seconds: 2.5, cut: 'cut'},
  ],
};
