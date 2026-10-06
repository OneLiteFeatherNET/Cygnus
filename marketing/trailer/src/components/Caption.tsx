import {AbsoluteFill, interpolate, useCurrentFrame, useVideoConfig} from 'remotion';
import {colors, roboto} from '../theme';

// Zweizeilige Unterzeile im Stil einer Untertitelspur: Zeile 1 trägt, Zeile 2 präzisiert.
export const Caption: React.FC<{lines: [string, string] | [string]; durationInFrames: number; align?: 'center' | 'bottom'}> = ({
  lines,
  durationInFrames,
  align = 'bottom',
}) => {
  const frame = useCurrentFrame();
  const {width, height} = useVideoConfig();
  const vertical = height > width;
  const unit = Math.min(width, height) / 1080;
  const fadeOut = interpolate(frame, [durationInFrames - 10, durationInFrames], [1, 0], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'});
  const first = interpolate(frame, [4, 16], [0, 1], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'});
  const second = interpolate(frame, [22, 34], [0, 1], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'});

  return (
    <AbsoluteFill
      style={{
        justifyContent: align === 'center' ? 'center' : 'flex-end',
        alignItems: 'center',
        paddingBottom: align === 'bottom' ? (vertical ? 420 : 150) * unit : 0,
        fontFamily: roboto,
        color: colors.white,
        textAlign: 'center',
        opacity: fadeOut,
        textShadow: '0 0 24px rgba(0,0,0,0.9)',
      }}
    >
      <div style={{fontSize: (vertical ? 92 : 78) * unit, fontWeight: 700, letterSpacing: -0.5, whiteSpace: 'pre-line', opacity: first, transform: `translateY(${(1 - first) * 12}px)`}}>
        {lines[0]}
      </div>
      {lines[1] ? (
        <div style={{marginTop: 14 * unit, fontSize: (vertical ? 52 : 42) * unit, fontWeight: 300, letterSpacing: 1, color: colors.text, opacity: second}}>
          {lines[1]}
        </div>
      ) : null}
    </AbsoluteFill>
  );
};
