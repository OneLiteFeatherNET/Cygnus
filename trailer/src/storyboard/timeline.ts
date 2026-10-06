import type {Beat, Cut, Storyboard} from './types';

// The glitch cut flashes. Photosensitive-epilepsy guidance (WCAG 2.3.1, Harding
// test) caps flashes at three per second; two glitch cuts closer than this would
// break that, so a storyboard that tries is refused at load time instead of being
// rendered and published.
export const GLITCH_FRAMES = 8;
export const MIN_SECONDS_BETWEEN_GLITCHES = 1;

export type TimedBeat = {beat: Beat; cut: Cut; from: number; frames: number};

export const timeline = (board: Storyboard, fps: number): TimedBeat[] => {
  let from = 0;
  return board.beats.map((beat, index) => {
    const frames = Math.round(beat.seconds * fps);
    const timed = {beat, cut: index === 0 ? 'cut' : (beat.cut ?? 'cut'), from, frames} as TimedBeat;
    from += frames;
    return timed;
  });
};

export const totalFrames = (board: Storyboard, fps: number): number =>
  timeline(board, fps).reduce((sum, beat) => sum + beat.frames, 0);

export const validate = (name: string, board: Storyboard, fps: number): void => {
  const glitches = timeline(board, fps).filter((beat) => beat.cut === 'glitch');
  for (let i = 1; i < glitches.length; i++) {
    const gap = glitches[i].from - glitches[i - 1].from;
    if (gap < MIN_SECONDS_BETWEEN_GLITCHES * fps) {
      throw new Error(
        `${name}: glitch cuts at frame ${glitches[i - 1].from} and ${glitches[i].from} are ` +
          `${gap} frames apart, below ${MIN_SECONDS_BETWEEN_GLITCHES}s. Spread them out.`,
      );
    }
  }
  for (const beat of timeline(board, fps)) {
    if (beat.cut === 'glitch' && beat.frames < GLITCH_FRAMES) {
      throw new Error(`${name}: a beat at frame ${beat.from} is shorter than its glitch cut.`);
    }
  }
};
