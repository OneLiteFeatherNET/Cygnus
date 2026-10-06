import {AbsoluteFill, interpolate, random, useCurrentFrame} from 'remotion';
import {CameraOverlay} from '../components/CameraOverlay';
import {Caption} from '../components/Caption';
import {Footage} from '../components/Footage';
import {Glitch} from '../components/Glitch';
import {Noise, Scanlines} from '../components/Noise';
import {Vignette} from '../components/Vignette';

// Der Jäger ist fast immer unsichtbar: im Platzhalter nur für einzelne Frames sichtbar.
export const Hunter: React.FC<{lines: [string, string]; src?: string; durationInFrames: number}> = ({lines, src, durationInFrames}) => {
  const frame = useCurrentFrame();
  const glimpse = frame > durationInFrames * 0.45 && random(`glimpse-${Math.floor(frame / 2)}`) > 0.72 ? 1 : 0;
  const glitch = glimpse ? 1.2 : interpolate(frame, [0, 8], [0.8, 0], {extrapolateRight: 'clamp'});
  return (
    <AbsoluteFill>
      <Footage src={src} seed="hunter" figure={glimpse} />
      <Vignette stage={16} />
      {glitch > 0 ? <Glitch intensity={glitch} seed="hunter" /> : null}
      <Noise opacity={0.07 + glimpse * 0.25} seed="hunter" />
      <Scanlines />
      <CameraOverlay />
      <Caption lines={lines} durationInFrames={durationInFrames} />
    </AbsoluteFill>
  );
};
