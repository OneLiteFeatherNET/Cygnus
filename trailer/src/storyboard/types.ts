import type {ClipId} from '../clips';

// A trailer is a list of beats played back to back. Beats are timed in seconds and
// converted to frames once, so the same storyboard renders at any frame rate.
export type Beat =
  | {kind: 'warning'; seconds: number}
  | {kind: 'clip'; clip: ClipId; seconds: number; from?: number; caption?: string}
  | {kind: 'text'; lines: string[]; seconds: number}
  | {kind: 'title'; seconds: number; subtitle?: string}
  | {kind: 'cta'; seconds: number}
  | {kind: 'end'; seconds: number};

export type Cut = 'cut' | 'glitch';

export type Storyboard = {
  // Every beat after the first one starts with this transition.
  beats: Array<Beat & {cut?: Cut}>;
  // 9:16 crops the footage to its centre and stacks the type higher.
  vertical: boolean;
  // A short has no time for a full-screen warning; it carries the warning as a
  // banner over the opening shot instead.
  warningAsBanner: boolean;
};
