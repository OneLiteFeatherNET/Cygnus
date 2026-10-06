import {AbsoluteFill, Img, interpolate, staticFile, useCurrentFrame, useVideoConfig} from 'remotion';
import {Caption} from '../components/Caption';
import {Footage} from '../components/Footage';
import {Glitch} from '../components/Glitch';
import {Noise, Scanlines} from '../components/Noise';
import {colors, roboto} from '../theme';

// Harter Schnitt nach Störung, dann entsättigte Zuschauersicht. Kein Blut, kein Gesicht.
export const Spectate: React.FC<{lines: [string, string]; src?: string; durationInFrames: number}> = ({lines, src, durationInFrames}) => {
  const frame = useCurrentFrame();
  const {width, height} = useVideoConfig();
  const unit = Math.min(width, height) / 1080;
  const hit = frame < 10;
  const sat = interpolate(frame, [10, 30], [1, 0], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'});

  return (
    <AbsoluteFill style={{background: colors.black}}>
      <AbsoluteFill style={{filter: `grayscale(${1 - sat}) brightness(${hit ? 0.3 : 0.8})`, transform: `scale(${hit ? 1.08 : 1})`}}>
        <Footage src={src} seed="spectate" />
      </AbsoluteFill>
      {hit ? <Glitch intensity={2.5} seed="spectate" /> : null}
      <Noise opacity={hit ? 0.6 : 0.08} seed="spectate" />
      <Scanlines />
      {!hit ? (
        <AbsoluteFill style={{alignItems: 'center', paddingTop: 110 * unit}}>
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 16 * unit,
              fontFamily: roboto,
              fontSize: 34 * unit,
              letterSpacing: 8,
              color: colors.text,
              opacity: interpolate(frame, [12, 22], [0, 1], {extrapolateRight: 'clamp'}),
            }}
          >
            <Img src={staticFile('pack/icons/ghost.png')} style={{width: 48 * unit, height: 48 * unit, imageRendering: 'pixelated'}} />
            SPECTATOR
          </div>
        </AbsoluteFill>
      ) : null}
      <Caption lines={lines} durationInFrames={durationInFrames} />
    </AbsoluteFill>
  );
};
