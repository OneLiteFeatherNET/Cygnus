import React from 'react';
import {AbsoluteFill, interpolate, random, useCurrentFrame, useVideoConfig} from 'remotion';
import {Noise} from './Noise';

// The pack's VHS look carried onto the whole cut: grain, scanlines, a tracking band
// that rolls down the frame, and the vignette the dread shader breathes with.
export const VhsOverlay: React.FC = () => {
  const frame = useCurrentFrame();
  const {height, fps} = useVideoConfig();
  const bandCycle = fps * 6;
  const bandY = interpolate(frame % bandCycle, [0, bandCycle], [-0.1, 1.1]) * height;
  const breathe = 0.55 + 0.08 * Math.sin((frame / fps) * Math.PI * 0.5);
  const jitter = random(`band-${Math.floor(frame / 2)}`) * 0.06;

  return (
    <AbsoluteFill style={{pointerEvents: 'none'}}>
      <Noise opacity={0.11} id="vhs" />
      <AbsoluteFill
        style={{
          backgroundImage:
            'repeating-linear-gradient(0deg, rgba(0,0,0,0.22) 0px, rgba(0,0,0,0.22) 1px, transparent 1px, transparent 3px)',
        }}
      />
      <div
        style={{
          position: 'absolute',
          left: 0,
          right: 0,
          top: bandY,
          height: height * 0.04,
          background: `linear-gradient(180deg, transparent, rgba(255,255,255,${0.04 + jitter}), transparent)`,
        }}
      />
      <AbsoluteFill
        style={{
          background: `radial-gradient(ellipse at center, transparent 45%, rgba(0,0,0,${breathe}) 100%)`,
        }}
      />
    </AbsoluteFill>
  );
};
