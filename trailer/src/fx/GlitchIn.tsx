import React from 'react';
import {AbsoluteFill, interpolate, random, useCurrentFrame} from 'remotion';
import {GLITCH_FRAMES} from '../storyboard/timeline';
import {colors} from '../theme';

// The cut into a beat, drawn the way the game's gaze glitch tears the screen.
// Exactly one brightness peak per cut (frame 1), held below full white and kept off
// saturated red, so a cut counts as a single flash; storyboard/timeline.ts keeps
// cuts at least a second apart.
export const GlitchIn: React.FC<{children: React.ReactNode; seed: string}> = ({children, seed}) => {
  const frame = useCurrentFrame();
  if (frame >= GLITCH_FRAMES) {
    return <AbsoluteFill>{children}</AbsoluteFill>;
  }

  const strength = interpolate(frame, [0, GLITCH_FRAMES], [1, 0], {extrapolateRight: 'clamp'});
  const shift = (random(`${seed}-x-${frame}`) - 0.5) * 80 * strength;
  const peak = frame === 1 ? 0.28 : 0;
  const bars = Array.from({length: 7}, (_, i) => ({
    top: random(`${seed}-t-${frame}-${i}`) * 100,
    height: 1 + random(`${seed}-h-${frame}-${i}`) * 7,
    offset: (random(`${seed}-o-${frame}-${i}`) - 0.5) * 30 * strength,
    tint: i % 3 === 0 ? colors.fringeCyan : i % 3 === 1 ? colors.fringeRed : '#000',
  }));

  return (
    <AbsoluteFill>
      <AbsoluteFill
        style={{
          transform: `translateX(${shift}px)`,
          filter: `contrast(${1 + strength * 0.6}) saturate(${1 - strength * 0.7})`,
        }}
      >
        {children}
      </AbsoluteFill>
      {bars.map((bar, i) => (
        <div
          key={i}
          style={{
            position: 'absolute',
            left: `${bar.offset}%`,
            width: '100%',
            top: `${bar.top}%`,
            height: `${bar.height}%`,
            background: bar.tint,
            opacity: bar.tint === '#000' ? 0.85 * strength : 0.22 * strength,
            mixBlendMode: bar.tint === '#000' ? 'normal' : 'screen',
          }}
        />
      ))}
      <AbsoluteFill style={{background: colors.bone, opacity: peak}} />
    </AbsoluteFill>
  );
};
