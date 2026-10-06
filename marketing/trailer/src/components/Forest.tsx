import {AbsoluteFill, interpolate, random, useCurrentFrame, useVideoConfig} from 'remotion';
import {colors} from '../theme';

// Prozeduraler Platzhalter für fehlende Gameplay-Aufnahmen: Blockige Baumreihen in drei
// Tiefenebenen mit Nebel und langsamer Kamerafahrt. Blockig, damit es nach Minecraft aussieht.
const Row: React.FC<{seed: string; depth: number; drift: number}> = ({seed, depth, drift}) => {
  const {width, height} = useVideoConfig();
  const block = (18 + depth * 22) * (Math.min(width, height) / 1080);
  const count = Math.ceil((width * 1.6) / (block * 3.2));
  const shade = Math.round(14 + depth * 10);
  return (
    <AbsoluteFill style={{transform: `translateX(${-drift * (0.4 + depth * 0.6)}px)`}}>
      {new Array(count).fill(0).map((_, i) => {
        const x = i * block * 3.2 + random(`${seed}-x-${i}`) * block * 1.5 - width * 0.2;
        const trunkH = height * (0.55 + random(`${seed}-h-${i}`) * 0.4);
        const crownW = block * (2 + Math.floor(random(`${seed}-c-${i}`) * 3));
        return (
          <div key={i}>
            <div style={{position: 'absolute', left: x, bottom: 0, width: block, height: trunkH, background: `rgb(${shade},${shade},${shade + 3})`}} />
            <div
              style={{
                position: 'absolute',
                left: x - crownW / 2 + block / 2,
                bottom: trunkH - block,
                width: crownW,
                height: block * (3 + Math.floor(random(`${seed}-k-${i}`) * 3)),
                background: `rgb(${shade - 2},${shade + 1},${shade - 2})`,
              }}
            />
          </div>
        );
      })}
    </AbsoluteFill>
  );
};

export const Forest: React.FC<{seed?: string; figure?: number}> = ({seed = 'forest', figure = 0}) => {
  const frame = useCurrentFrame();
  const {width, height} = useVideoConfig();
  const drift = frame * 1.4;
  const flicker = 0.85 + random(`flicker-${Math.floor(frame / 3)}`) * 0.15;
  const unit = Math.min(width, height) / 1080;

  return (
    <AbsoluteFill style={{background: colors.black, overflow: 'hidden'}}>
      <AbsoluteFill style={{background: `radial-gradient(ellipse at 50% 30%, #15161c 0%, ${colors.black} 70%)`}} />
      <Row seed={`${seed}-far`} depth={0} drift={drift} />
      <AbsoluteFill style={{background: 'linear-gradient(0deg, rgba(60,62,70,0.35) 0%, transparent 60%)'}} />
      {figure > 0 ? (
        // Silhouette des Jägers: nur angedeutet, schmal, ohne Gesicht. Kein Gore (Jugendschutz).
        <div
          style={{
            position: 'absolute',
            left: width * 0.62,
            bottom: height * 0.08,
            width: 34 * unit,
            height: 300 * unit,
            background: '#050506',
            opacity: figure,
            boxShadow: `0 -110px 0 -6px #050506`,
          }}
        />
      ) : null}
      <Row seed={`${seed}-mid`} depth={1} drift={drift} />
      <Row seed={`${seed}-near`} depth={2} drift={drift} />
      {/* Taschenlampenkegel */}
      <AbsoluteFill
        style={{
          background: `radial-gradient(circle at ${50 + Math.sin(frame / 25) * 6}% ${55 + Math.cos(frame / 31) * 4}%, rgba(255,244,214,${0.32 * flicker}) 0%, rgba(255,244,214,0.10) 20%, transparent 45%)`,
          mixBlendMode: 'screen',
        }}
      />
      <AbsoluteFill style={{background: `rgba(0,0,0,${interpolate(flicker, [0.85, 1], [0.25, 0])})`}} />
    </AbsoluteFill>
  );
};
