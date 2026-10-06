import {AbsoluteFill, interpolate, useCurrentFrame} from 'remotion';
import {Caption} from '../components/Caption';
import {Noise, Scanlines} from '../components/Noise';
import {colors} from '../theme';

// Bandrauschen, das sich zu Schwarz beruhigt; darauf die Rundenlänge als erster Fakt.
export const ColdOpen: React.FC<{text: string; durationInFrames: number}> = ({text, durationInFrames}) => {
  const frame = useCurrentFrame();
  const noise = interpolate(frame, [0, 24, 40], [0.9, 0.5, 0.1], {extrapolateRight: 'clamp'});
  return (
    <AbsoluteFill style={{background: colors.black}}>
      <Noise opacity={noise} seed="cold" />
      <Scanlines />
      <Caption lines={[text]} durationInFrames={durationInFrames} align="center" />
    </AbsoluteFill>
  );
};
