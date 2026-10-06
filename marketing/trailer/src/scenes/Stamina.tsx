import {AbsoluteFill, interpolate, useCurrentFrame, useVideoConfig} from 'remotion';
import {CameraOverlay} from '../components/CameraOverlay';
import {Caption} from '../components/Caption';
import {Footage} from '../components/Footage';
import {Noise, Scanlines} from '../components/Noise';
import {Vignette} from '../components/Vignette';

// Ausdauer liegt im Spiel auf der XP-Leiste. Hier läuft sie leer, die Sicht zieht sich zu.
export const Stamina: React.FC<{lines: [string, string]; src?: string; durationInFrames: number}> = ({lines, src, durationInFrames}) => {
  const frame = useCurrentFrame();
  const {width, height} = useVideoConfig();
  const vertical = height > width;
  const unit = Math.min(width, height) / 1080;
  const value = interpolate(frame, [0, durationInFrames * 0.8], [1, 0], {extrapolateRight: 'clamp'});
  const empty = value <= 0.001;
  const shake = empty ? 0 : Math.sin(frame * 1.7) * 4 * value;
  const barWidth = (vertical ? 860 : 1100) * unit;

  return (
    <AbsoluteFill>
      <AbsoluteFill style={{transform: `translate(${shake}px, ${Math.abs(shake) / 2}px) scale(1.04)`}}>
        <Footage src={src} seed="stamina" />
      </AbsoluteFill>
      <Vignette stage={empty ? 28 : 16} />
      <AbsoluteFill style={{justifyContent: 'flex-end', alignItems: 'center', paddingBottom: (vertical ? 300 : 70) * unit}}>
        <div style={{width: barWidth, height: 26 * unit, background: '#1a1a1a', border: `${3 * unit}px solid #000`, boxShadow: 'inset 0 0 0 2px #3a3a3a'}}>
          <div
            style={{
              width: `${value * 100}%`,
              height: '100%',
              background: empty ? 'transparent' : 'linear-gradient(180deg, #b6ff6a 0%, #80ff20 45%, #4fa30f 100%)',
            }}
          />
        </div>
      </AbsoluteFill>
      <Noise opacity={0.07} seed="stamina" />
      <Scanlines />
      <CameraOverlay startTime="23:45:10" />
      <Caption lines={lines} durationInFrames={durationInFrames} />
    </AbsoluteFill>
  );
};
