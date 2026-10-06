import {useEffect, useRef} from 'react';
import {AbsoluteFill, useCurrentFrame, useVideoConfig} from 'remotion';

// VHS-Rauschen, deterministisch pro Frame (eigener PRNG mit Frame-Seed), damit jeder Render
// identisch ist. Remotions random() pro Pixel wäre bei 230k Pixeln pro Frame zu langsam.
const hash = (text: string) => {
  let h = 2166136261;
  for (let i = 0; i < text.length; i++) h = Math.imul(h ^ text.charCodeAt(i), 16777619);
  return h >>> 0;
};

const mulberry32 = (seed: number) => () => {
  seed = (seed + 0x6d2b79f5) | 0;
  let t = Math.imul(seed ^ (seed >>> 15), 1 | seed);
  t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
  return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
};

export const Noise: React.FC<{opacity?: number; seed?: string; grain?: number}> = ({opacity = 0.08, seed = 'cygnus', grain = 3}) => {
  const frame = useCurrentFrame();
  const {width, height} = useVideoConfig();
  const ref = useRef<HTMLCanvasElement>(null);
  const w = Math.round(width / grain);
  const h = Math.round(height / grain);

  useEffect(() => {
    const ctx = ref.current?.getContext('2d');
    if (!ctx) return;
    const next = mulberry32(hash(seed) + frame * 7919);
    const image = ctx.createImageData(w, h);
    for (let i = 0; i < w * h; i++) {
      const v = (next() * 255) | 0;
      image.data[i * 4] = v;
      image.data[i * 4 + 1] = v;
      image.data[i * 4 + 2] = v;
      image.data[i * 4 + 3] = 255;
    }
    ctx.putImageData(image, 0, 0);
  }, [frame, seed, w, h]);

  return (
    <AbsoluteFill style={{opacity, mixBlendMode: 'screen', pointerEvents: 'none'}}>
      <canvas ref={ref} width={w} height={h} style={{width: '100%', height: '100%'}} />
    </AbsoluteFill>
  );
};

export const Scanlines: React.FC<{opacity?: number}> = ({opacity = 0.08}) => (
  <AbsoluteFill
    style={{
      opacity,
      pointerEvents: 'none',
      backgroundImage: 'repeating-linear-gradient(0deg, rgba(0,0,0,0.9) 0px, rgba(0,0,0,0.9) 2px, transparent 2px, transparent 5px)',
    }}
  />
);
