import {AbsoluteFill, random, useCurrentFrame} from 'remotion';

// Horizontale Bildstörungen in Bandstreifen, wie ein beschädigtes VHS-Band.
export const Glitch: React.FC<{intensity?: number; seed?: string}> = ({intensity = 1, seed = 'glitch'}) => {
  const frame = useCurrentFrame();
  const bands = Math.round(6 * intensity);
  return (
    <AbsoluteFill style={{pointerEvents: 'none'}}>
      {new Array(bands).fill(0).map((_, i) => {
        const top = random(`${seed}-${frame}-t-${i}`) * 100;
        const h = 0.5 + random(`${seed}-${frame}-h-${i}`) * 4;
        const shift = (random(`${seed}-${frame}-s-${i}`) - 0.5) * 80 * intensity;
        return (
          <div
            key={i}
            style={{
              position: 'absolute',
              top: `${top}%`,
              left: shift,
              width: '100%',
              height: `${h}%`,
              background: i % 2 ? 'rgba(236,0,139,0.06)' : 'rgba(39,169,225,0.06)',
              mixBlendMode: 'screen',
              backdropFilter: 'invert(0.08)',
            }}
          />
        );
      })}
    </AbsoluteFill>
  );
};
