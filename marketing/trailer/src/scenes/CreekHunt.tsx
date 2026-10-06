import {AbsoluteFill, interpolate, useCurrentFrame, useVideoConfig} from 'remotion';
import {CameraOverlay} from '../components/CameraOverlay';
import {Caption} from '../components/Caption';
import {CreekFigure} from '../components/CreekFigure';
import {Footage} from '../components/Footage';
import {Noise, Scanlines} from '../components/Noise';
import {Vignette} from '../components/Vignette';
import {colors, roboto} from '../theme';

// Jagd (HuntState): Der Creek bewegt sich nur, solange man nicht hinsieht. Im Bild: kurze
// Schwarzblenden (wegsehen), nach jeder steht er näher. Dazu die rote Tab-Markierung (◆),
// mit der alle sehen, wen er gerade jagt (HuntedTabWitness).
const BLINKS = 4;

const TabList: React.FC<{scale: number}> = ({scale}) => {
  const rows: [string, boolean][] = [
    ['Survivor_1', false],
    ['Survivor_2', true],
    ['Survivor_3', false],
    ['Survivor_4', false],
  ];
  return (
    <div
      style={{
        position: 'absolute',
        top: 130 * scale,
        right: 90 * scale,
        padding: `${10 * scale}px ${18 * scale}px`,
        background: 'rgba(0,0,0,0.55)',
        fontFamily: roboto,
        fontSize: 26 * scale,
        lineHeight: 1.5,
        color: colors.text,
      }}
    >
      {rows.map(([name, hunted]) => (
        <div key={name} style={{color: hunted ? '#FF5555' : colors.text}}>
          {name}
          {hunted ? ' ◆' : ''}
        </div>
      ))}
    </div>
  );
};

export const CreekHunt: React.FC<{lines: [string, string]; src?: string; durationInFrames: number}> = ({lines, src, durationInFrames}) => {
  const frame = useCurrentFrame();
  const {width, height} = useVideoConfig();
  const vertical = height > width;
  const unit = Math.min(width, height) / 1080;
  const step = durationInFrames / BLINKS;
  const index = Math.min(BLINKS - 1, Math.floor(frame / step));
  const local = frame - index * step;
  const dark = index > 0 && local < 4;
  const closeness = interpolate(index, [0, BLINKS - 1], [0.32, 0.95]);
  const figureHeight = height * closeness * (vertical ? 0.7 : 1);
  const breath = 1 + Math.sin(frame / 5) * 0.004;

  return (
    <AbsoluteFill style={{background: 'black'}}>
      {!dark ? (
        <AbsoluteFill style={{transform: `scale(${breath})`}}>
          <Footage src={src} seed="hunt" />
          {!src ? (
            <div style={{position: 'absolute', left: width / 2 - (figureHeight * 0.4) / 2, bottom: -figureHeight * 0.08 * index + height * (vertical ? 0.22 : 0.04)}}>
              <CreekFigure height={figureHeight} eyes={1} />
            </div>
          ) : null}
          <Vignette stage={index >= BLINKS - 1 ? 28 : 16} />
        </AbsoluteFill>
      ) : null}
      <Noise opacity={dark ? 0.08 : 0.03} seed="hunt" />
      <Scanlines />
      {!dark ? <TabList scale={unit} /> : null}
      <CameraOverlay startTime="23:47:02" />
      <Caption lines={lines} durationInFrames={durationInFrames} />
    </AbsoluteFill>
  );
};
