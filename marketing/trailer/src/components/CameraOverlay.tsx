import {AbsoluteFill, useCurrentFrame, useVideoConfig} from 'remotion';
import {colors, robotoMono} from '../theme';

// Found-Footage-Rahmen: REC-Punkt, Bandzähler, Ecken. Bewusst klein, das Bild trägt.
export const CameraOverlay: React.FC<{startTime?: string}> = ({startTime = '23:41:07'}) => {
  const frame = useCurrentFrame();
  const {fps, width} = useVideoConfig();
  const [h, m, s] = startTime.split(':').map(Number);
  const total = h * 3600 + m * 60 + s + Math.floor(frame / fps);
  const pad = (n: number) => String(n).padStart(2, '0');
  const clock = `${pad(Math.floor(total / 3600) % 24)}:${pad(Math.floor(total / 60) % 60)}:${pad(total % 60)}`;
  const blink = Math.floor(frame / (fps / 2)) % 2 === 0;
  const unit = width / 1920;
  const corner = (pos: React.CSSProperties): React.CSSProperties => ({
    position: 'absolute',
    width: 60 * unit,
    height: 60 * unit,
    borderColor: 'rgba(229,229,229,0.55)',
    borderStyle: 'solid',
    borderWidth: 0,
    ...pos,
  });
  const inset = 48 * unit;

  return (
    <AbsoluteFill style={{fontFamily: robotoMono, color: colors.text, fontSize: 30 * unit, pointerEvents: 'none'}}>
      <div style={corner({top: inset, left: inset, borderTopWidth: 3, borderLeftWidth: 3})} />
      <div style={corner({top: inset, right: inset, borderTopWidth: 3, borderRightWidth: 3})} />
      <div style={corner({bottom: inset, left: inset, borderBottomWidth: 3, borderLeftWidth: 3})} />
      <div style={corner({bottom: inset, right: inset, borderBottomWidth: 3, borderRightWidth: 3})} />
      <div style={{position: 'absolute', top: inset + 20 * unit, left: inset + 30 * unit, display: 'flex', alignItems: 'center', gap: 14 * unit}}>
        <div style={{width: 20 * unit, height: 20 * unit, borderRadius: '50%', background: '#D0021B', opacity: blink ? 1 : 0.15}} />
        REC
      </div>
      <div style={{position: 'absolute', bottom: inset + 20 * unit, right: inset + 30 * unit, letterSpacing: 2}}>{clock}</div>
    </AbsoluteFill>
  );
};
