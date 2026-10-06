import {AbsoluteFill, Img, interpolate, spring, staticFile, useCurrentFrame, useVideoConfig} from 'remotion';
import {CameraOverlay} from '../components/CameraOverlay';
import {Caption} from '../components/Caption';
import {Footage} from '../components/Footage';
import {Noise, Scanlines} from '../components/Noise';
import {Vignette} from '../components/Vignette';
import {colors, roboto} from '../theme';

// Die sechs Seiten-Texturen aus dem Pack fliegen ein, der Zähler zählt hoch.
export const Pages: React.FC<{lines: [string, string]; src?: string; durationInFrames: number}> = ({lines, src, durationInFrames}) => {
  const frame = useCurrentFrame();
  const {fps, width, height} = useVideoConfig();
  const vertical = height > width;
  const unit = Math.min(width, height) / 1080;
  const size = (vertical ? 190 : 150) * unit;
  const collected = Math.min(6, Math.floor(interpolate(frame, [10, durationInFrames - 20], [0, 6.99], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'})));

  return (
    <AbsoluteFill>
      <Footage src={src} seed="pages" />
      <Vignette stage={4} />
      <AbsoluteFill style={{justifyContent: 'center', alignItems: 'center', paddingBottom: (vertical ? 260 : 120) * unit}}>
        <div style={{display: 'grid', gridTemplateColumns: `repeat(${vertical ? 3 : 6}, ${size}px)`, gap: 28 * unit}}>
          {[1, 2, 3, 4, 5, 6].map((n, i) => {
            const s = spring({frame: frame - 10 - i * 9, fps, config: {damping: 14}});
            const got = i < collected;
            return (
              <Img
                key={n}
                src={staticFile(`pack/page/page_${n}.png`)}
                style={{
                  width: size,
                  height: size,
                  imageRendering: 'pixelated',
                  opacity: s * (got ? 1 : 0.35),
                  transform: `scale(${0.6 + s * 0.4}) rotate(${(i % 2 ? 1 : -1) * 4}deg)`,
                  filter: got ? 'drop-shadow(0 0 18px rgba(255,244,214,0.35))' : 'grayscale(1)',
                }}
              />
            );
          })}
        </div>
        <div style={{marginTop: 36 * unit, fontFamily: roboto, fontWeight: 700, fontSize: 54 * unit, color: colors.text, letterSpacing: 4}}>
          {collected}
        </div>
      </AbsoluteFill>
      <Noise opacity={0.03} seed="pages" />
      <Scanlines />
      <CameraOverlay startTime="23:43:52" />
      <Caption lines={lines} durationInFrames={durationInFrames} />
    </AbsoluteFill>
  );
};
