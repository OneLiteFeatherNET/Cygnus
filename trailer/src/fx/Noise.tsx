import React from 'react';
import {AbsoluteFill, useCurrentFrame} from 'remotion';

// Sensor grain. The turbulence seed follows the frame, so every render of the same
// frame is identical and the grain still moves.
export const Noise: React.FC<{opacity: number; id: string}> = ({opacity, id}) => {
  const frame = useCurrentFrame();
  const filter = `noise-${id}`;
  return (
    <AbsoluteFill style={{opacity, mixBlendMode: 'screen', pointerEvents: 'none'}}>
      <svg width="100%" height="100%" preserveAspectRatio="none">
        <filter id={filter}>
          <feTurbulence type="fractalNoise" baseFrequency="0.9" numOctaves="2" seed={frame % 97} />
          <feColorMatrix type="saturate" values="0" />
        </filter>
        <rect width="100%" height="100%" filter={`url(#${filter})`} />
      </svg>
    </AbsoluteFill>
  );
};
