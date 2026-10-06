import {AbsoluteFill, Img, interpolate, Easing, staticFile, useCurrentFrame, useVideoConfig} from 'remotion';
import {Caption} from '../components/Caption';
import {CreekFigure} from '../components/CreekFigure';
import {Footage} from '../components/Footage';
import {Glitch} from '../components/Glitch';
import {Noise, Scanlines} from '../components/Noise';

// Erwischt (CatchLaunch): Der Creek steht direkt vor einem, dann geht es senkrecht nach oben,
// der Wald fällt weg, darüber der Mond aus dem Pack. Die Alternative (CatchSwap) nennt die Unterzeile.
export const CreekCatch: React.FC<{lines: [string, string]; src?: string; durationInFrames: number}> = ({lines, src, durationInFrames}) => {
  const frame = useCurrentFrame();
  const {width, height} = useVideoConfig();
  const unit = Math.min(width, height) / 1080;
  const close = frame < 8;
  const lift = interpolate(frame, [8, 26], [0, 1], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp', easing: Easing.out(Easing.cubic)});
  const moonSize = 560 * unit;

  return (
    <AbsoluteFill style={{background: '#05060a', overflow: 'hidden'}}>
      <Img
        src={staticFile('pack/moon/full_moon.png')}
        style={{
          position: 'absolute',
          left: width * 0.58,
          top: height * 0.02 + (1 - lift) * height * 0.3,
          width: moonSize,
          height: moonSize,
          imageRendering: 'pixelated',
          // Mond-Texturen haben schwarzen Grund; im Spiel additiv geblendet, hier per screen.
          mixBlendMode: 'screen',
          opacity: lift * 0.9,
        }}
      />
      <AbsoluteFill style={{transform: `translateY(${lift * height * 1.1}px) rotate(${lift * 4}deg)`, filter: lift > 0 && lift < 0.9 ? 'blur(3px)' : undefined}}>
        <Footage src={src} seed="catch" />
        {close && !src ? (
          <div style={{position: 'absolute', left: width / 2 - height * 0.5, top: -height * 0.15}}>
            <CreekFigure height={height * 2.4} />
          </div>
        ) : null}
      </AbsoluteFill>
      {frame >= 6 && frame < 12 ? <Glitch intensity={1.2} seed="catch" /> : null}
      <Noise opacity={0.03} seed="catch" />
      <Scanlines />
      <Caption lines={lines} durationInFrames={durationInFrames} />
    </AbsoluteFill>
  );
};
