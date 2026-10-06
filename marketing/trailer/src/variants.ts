import data from './variants.json';
import {FPS} from './theme';

export type SceneId =
  | 'coldOpen'
  | 'hunter'
  | 'pages'
  | 'stamina'
  | 'rotation'
  | 'creekStalk'
  | 'creekHunt'
  | 'creekCatch'
  | 'spectate'
  | 'end';

export type HookId = 'round' | 'hunter' | 'creek' | 'manfred' | 'pages';

export type Variant = {
  id: string;
  hook: HookId;
  hit: SceneId;
  scenes: [SceneId, number][];
};

export const variants = data.variants as Variant[];

export type Format = {id: 'vertical' | 'square' | 'landscape'; width: number; height: number};

// 9:16 für TikTok, Shorts, Reels, Stories; 1:1 für Instagram-Feed, X, Mastodon; 16:9 für YouTube, Discord.
export const formats: Format[] = [
  {id: 'vertical', width: 1080, height: 1920},
  {id: 'square', width: 1080, height: 1080},
  {id: 'landscape', width: 1920, height: 1080},
];

export const frames = (seconds: number) => Math.round(seconds * FPS);

export const variantDuration = (variant: Variant) =>
  variant.scenes.reduce((sum, [, seconds]) => sum + frames(seconds), 0);

// Startframe einer Szene innerhalb der Variante.
export const sceneStart = (variant: Variant, scene: SceneId) => {
  let at = 0;
  for (const [id, seconds] of variant.scenes) {
    if (id === scene) return at;
    at += frames(seconds);
  }
  return -1;
};
