import {AbsoluteFill, interpolate, useCurrentFrame, useVideoConfig} from 'remotion';
import {CameraOverlay} from '../components/CameraOverlay';
import {Caption} from '../components/Caption';
import {CreekFigure} from '../components/CreekFigure';
import {Footage} from '../components/Footage';
import {Glitch} from '../components/Glitch';
import {Noise, Scanlines} from '../components/Noise';
import {Vignette} from '../components/Vignette';

// Verfolgen (StalkState): Der Creek steht in der Ferne, hält den Blick kurz und ist dann woanders,
// jedes Mal ein Stück näher. Nur der Verfolgte sieht ihn.
const SPOTS = [
  {x: 0.72, size: 0.2},
  {x: 0.24, size: 0.27},
  {x: 0.6, size: 0.36},
];

export const CreekStalk: React.FC<{lines: [string, string]; src?: string; durationInFrames: number}> = ({lines, src, durationInFrames}) => {
  const frame = useCurrentFrame();
  const {width, height} = useVideoConfig();
  const step = durationInFrames / SPOTS.length;
  const index = Math.min(SPOTS.length - 1, Math.floor(frame / step));
  const local = frame - index * step;
  const jump = local < 3 && index > 0;
  const spot = SPOTS[index];
  const fadeIn = interpolate(local, [0, 6], [0, 1], {extrapolateRight: 'clamp'});
  const figureHeight = height * spot.size * (height > width ? 0.8 : 1);

  return (
    <AbsoluteFill>
      <Footage src={src} seed="stalk" />
      {!src ? (
        <div style={{position: 'absolute', left: width * spot.x, bottom: height * (height > width ? 0.3 : 0.14), opacity: jump ? 0 : fadeIn}}>
          <CreekFigure height={figureHeight} eyes={0.35 + 0.65 * Math.abs(Math.sin(frame / 9))} />
        </div>
      ) : null}
      <Vignette stage={16} />
      {jump ? <Glitch intensity={0.6} seed="stalk" /> : null}
      <Noise opacity={0.03} seed="stalk" />
      <Scanlines />
      <CameraOverlay startTime="23:46:20" />
      <Caption lines={lines} durationInFrames={durationInFrames} />
    </AbsoluteFill>
  );
};
