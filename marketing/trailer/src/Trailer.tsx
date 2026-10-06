import {AbsoluteFill, Html5Audio, Sequence, Series, interpolate, staticFile} from 'remotion';
import {copy, TrailerProps} from './props';
import {ColdOpen} from './scenes/ColdOpen';
import {CreekCatch} from './scenes/CreekCatch';
import {CreekHunt} from './scenes/CreekHunt';
import {CreekStalk} from './scenes/CreekStalk';
import {EndCard} from './scenes/EndCard';
import {Hunter} from './scenes/Hunter';
import {Pages} from './scenes/Pages';
import {Rotation} from './scenes/Rotation';
import {Spectate} from './scenes/Spectate';
import {Stamina} from './scenes/Stamina';
import {frames, sceneStart, SceneId, Variant, variantDuration} from './variants';

// Eine Variante ist eine Szenenliste aus src/variants.json; dieselben Szenen in anderer Auswahl,
// Reihenfolge und Länge ergeben die Social-Media-Varianten.
const Scene: React.FC<{id: SceneId; durationInFrames: number; variant: Variant; props: TrailerProps}> = ({id, durationInFrames, variant, props}) => {
  const t = copy[props.language];
  const f = props.footage;
  const d = durationInFrames;
  switch (id) {
    case 'coldOpen':
      return <ColdOpen text={t.hooks[variant.hook]} durationInFrames={d} />;
    case 'hunter':
      return <Hunter lines={t.hunter} src={f.hunt} durationInFrames={d} />;
    case 'pages':
      return <Pages lines={t.pages} src={f.pages} durationInFrames={d} />;
    case 'stamina':
      return <Stamina lines={t.stamina} src={f.stamina} durationInFrames={d} />;
    case 'rotation':
      return <Rotation lines={t.rotation} durationInFrames={d} />;
    case 'creekStalk':
      return <CreekStalk lines={t.creekStalk} src={f.creekStalk} durationInFrames={d} />;
    case 'creekHunt':
      return <CreekHunt lines={t.creekHunt} src={f.creekHunt} durationInFrames={d} />;
    case 'creekCatch':
      return <CreekCatch lines={t.creekCatch} src={f.creekCatch} durationInFrames={d} />;
    case 'spectate':
      return <Spectate lines={t.spectate} src={f.spectate} durationInFrames={d} />;
    case 'end':
      return <EndCard {...props} durationInFrames={d} />;
  }
};

const Static: React.FC<{from: number; frames: number; variant: 1 | 2 | 3; volume: number}> = ({from, frames, variant, volume}) => (
  <Sequence from={Math.max(0, from)} durationInFrames={frames} layout="none">
    <Html5Audio src={staticFile(`pack/sounds/vhs_static_${variant}.ogg`)} volume={volume} />
  </Sequence>
);

export const Trailer: React.FC<TrailerProps & {variant: Variant}> = ({variant, ...props}) => {
  const total = variantDuration(variant);
  const hitAt = sceneStart(variant, variant.hit);
  const endAt = sceneStart(variant, 'end');
  const music = props.music === undefined ? `music/${variant.id}.ogg` : props.music;

  return (
    <AbsoluteFill style={{background: 'black'}}>
      <Series>
        {variant.scenes.map(([id, seconds], i) => (
          <Series.Sequence key={`${id}-${i}`} durationInFrames={frames(seconds)}>
            <Scene id={id} durationInFrames={frames(seconds)} variant={variant} props={props} />
          </Series.Sequence>
        ))}
      </Series>

      {/* Ton: Musik trägt, das Bandrauschen aus dem Pack setzt nur kurze Akzente an Schnitten. */}
      {props.staticVolume > 0 ? (
        <>
          <Static from={0} frames={frames(0.6)} variant={1} volume={0.6 * props.staticVolume} />
          {hitAt >= 0 ? <Static from={hitAt} frames={6} variant={1} volume={props.staticVolume} /> : null}
          {endAt >= 0 ? <Static from={endAt - 3} frames={5} variant={2} volume={0.5 * props.staticVolume} /> : null}
        </>
      ) : null}
      {music ? (
        <Html5Audio
          src={staticFile(music)}
          volume={(frame) => interpolate(frame, [0, 6, total - 20, total], [0, 0.9, 0.9, 0], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'})}
        />
      ) : null}
    </AbsoluteFill>
  );
};
