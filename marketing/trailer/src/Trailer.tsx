import {AbsoluteFill, Html5Audio, Sequence, Series, interpolate, staticFile} from 'remotion';
import {copy, TrailerProps} from './props';
import {ColdOpen} from './scenes/ColdOpen';
import {EndCard} from './scenes/EndCard';
import {Hunter} from './scenes/Hunter';
import {Pages} from './scenes/Pages';
import {Rotation} from './scenes/Rotation';
import {Spectate} from './scenes/Spectate';
import {Stamina} from './scenes/Stamina';
import cuts from './cuts.json';
import {FPS} from './theme';

// Szenenfolge = Feature-Reihenfolge des Spiels: Rundenlänge, Jäger, Seiten, Ausdauer,
// Rotation, Zuschauen, Absender. Teaser und Short teilen sich die Szenen, nur das Timing variiert.
export type Cut = {
  coldOpen: number;
  hunter: number;
  pages: number;
  stamina: number;
  rotation: number;
  spectate: number;
  end: number;
};

export const teaserCut: Cut = cuts.teaser;
// Short: ohne Rotationsszene, Hook in den ersten zwei Sekunden (Retention auf TikTok/Shorts).
export const shortCut: Cut = cuts.short;

export const cutDuration = (cut: Cut) =>
  Object.values(cut).reduce((sum, s) => sum + Math.round(s * FPS), 0);

const Static: React.FC<{from: number; frames: number; variant: 1 | 2 | 3; volume?: number}> = ({from, frames, variant, volume = 0.6}) => (
  <Sequence from={from} durationInFrames={frames} layout="none">
    <Html5Audio src={staticFile(`pack/sounds/vhs_static_${variant}.ogg`)} volume={volume} />
  </Sequence>
);

export const Trailer: React.FC<TrailerProps & {cut: Cut; hook?: boolean}> = ({cut, hook, ...props}) => {
  const t = copy[props.language];
  const f = (s: number) => Math.round(s * FPS);
  const spectateAt = Object.entries(cut)
    .filter(([k]) => ['coldOpen', 'hunter', 'pages', 'stamina', 'rotation'].includes(k))
    .reduce((sum, [, s]) => sum + f(s), 0);
  const total = cutDuration(cut);
  const endAt = total - f(cut.end);

  return (
    <AbsoluteFill style={{background: 'black'}}>
      <Series>
        <Series.Sequence durationInFrames={f(cut.coldOpen)}>
          <ColdOpen text={hook ? t.shortHook : t.coldOpen} durationInFrames={f(cut.coldOpen)} />
        </Series.Sequence>
        <Series.Sequence durationInFrames={f(cut.hunter)}>
          <Hunter lines={t.hunter} src={props.footage.hunt} durationInFrames={f(cut.hunter)} />
        </Series.Sequence>
        <Series.Sequence durationInFrames={f(cut.pages)}>
          <Pages lines={t.pages} src={props.footage.pages} durationInFrames={f(cut.pages)} />
        </Series.Sequence>
        <Series.Sequence durationInFrames={f(cut.stamina)}>
          <Stamina lines={t.stamina} src={props.footage.stamina} durationInFrames={f(cut.stamina)} />
        </Series.Sequence>
        {cut.rotation > 0 ? (
          <Series.Sequence durationInFrames={f(cut.rotation)}>
            <Rotation lines={t.rotation} durationInFrames={f(cut.rotation)} />
          </Series.Sequence>
        ) : null}
        <Series.Sequence durationInFrames={f(cut.spectate)}>
          <Spectate lines={t.spectate} src={props.footage.spectate} durationInFrames={f(cut.spectate)} />
        </Series.Sequence>
        <Series.Sequence durationInFrames={f(cut.end)}>
          <EndCard {...props} durationInFrames={f(cut.end)} />
        </Series.Sequence>
      </Series>

      {/* Ton: Musik trägt, das Bandrauschen aus dem Pack setzt nur kurze Akzente an Schnitten. */}
      {props.staticVolume > 0 ? (
        <>
          <Static from={0} frames={f(0.6)} variant={1} volume={0.6 * props.staticVolume} />
          <Static from={spectateAt} frames={6} variant={1} volume={props.staticVolume} />
          <Static from={endAt - 3} frames={5} variant={2} volume={0.5 * props.staticVolume} />
        </>
      ) : null}
      {props.music ? (
        <Html5Audio
          src={staticFile(props.music)}
          volume={(frame) => interpolate(frame, [0, 6, total - 20, total], [0, 0.9, 0.9, 0], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'})}
        />
      ) : null}
    </AbsoluteFill>
  );
};
