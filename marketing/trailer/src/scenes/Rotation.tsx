import {AbsoluteFill, Img, interpolate, random, staticFile, useCurrentFrame, useVideoConfig} from 'remotion';
import {Caption} from '../components/Caption';
import {Noise, Scanlines} from '../components/Noise';
import {colors} from '../theme';

// Draufsicht auf eine Karte: Spawnpunkte, eine Seite läuft per TTL ab und taucht woanders auf.
const SPOTS = 14;

export const Rotation: React.FC<{lines: [string, string]; durationInFrames: number}> = ({lines, durationInFrames}) => {
  const frame = useCurrentFrame();
  const {width, height} = useVideoConfig();
  const unit = Math.min(width, height) / 1080;
  const cycle = 30;
  const active = Math.floor(frame / cycle);
  const ttl = 1 - (frame % cycle) / cycle;
  const mapSize = Math.min(width, height) * 0.62;
  const spot = (i: number) => ({x: random(`spot-x-${i}`) * 0.84 + 0.08, y: random(`spot-y-${i}`) * 0.84 + 0.08});
  const current = Math.floor(random(`active-${active}`) * SPOTS);
  const p = spot(current);
  const r = 46 * unit;
  const fade = interpolate(frame, [0, 12], [0, 1], {extrapolateRight: 'clamp'});

  return (
    <AbsoluteFill style={{background: colors.black, justifyContent: 'center', alignItems: 'center'}}>
      <div
        style={{
          position: 'relative',
          width: mapSize,
          height: mapSize,
          opacity: fade,
          marginBottom: height > width ? 360 * unit : 140 * unit,
          border: '2px solid rgba(229,229,229,0.25)',
          backgroundImage:
            'linear-gradient(rgba(229,229,229,0.06) 1px, transparent 1px), linear-gradient(90deg, rgba(229,229,229,0.06) 1px, transparent 1px)',
          backgroundSize: `${mapSize / 16}px ${mapSize / 16}px`,
        }}
      >
        {new Array(SPOTS).fill(0).map((_, i) => {
          const s = spot(i);
          return (
            <div
              key={i}
              style={{position: 'absolute', left: s.x * mapSize - 6, top: s.y * mapSize - 6, width: 12, height: 12, background: 'rgba(229,229,229,0.25)'}}
            />
          );
        })}
        <svg style={{position: 'absolute', left: p.x * mapSize - r, top: p.y * mapSize - r}} width={r * 2} height={r * 2}>
          <circle cx={r} cy={r} r={r - 4} fill="none" stroke="rgba(229,229,229,0.15)" strokeWidth={4} />
          <circle
            cx={r}
            cy={r}
            r={r - 4}
            fill="none"
            stroke={colors.white}
            strokeWidth={4}
            strokeDasharray={2 * Math.PI * (r - 4)}
            strokeDashoffset={(1 - ttl) * 2 * Math.PI * (r - 4)}
            transform={`rotate(-90 ${r} ${r})`}
          />
        </svg>
        <Img
          src={staticFile(`pack/page/page_${(active % 6) + 1}.png`)}
          style={{position: 'absolute', left: p.x * mapSize - 24 * unit, top: p.y * mapSize - 24 * unit, width: 48 * unit, height: 48 * unit, imageRendering: 'pixelated', opacity: Math.min(1, ttl * 3)}}
        />
      </div>
      <Noise opacity={0.03} seed="rotation" />
      <Scanlines />
      <Caption lines={lines} durationInFrames={durationInFrames} />
    </AbsoluteFill>
  );
};
