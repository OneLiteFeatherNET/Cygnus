import React from 'react';
import {Audio} from '@remotion/media';
import {AbsoluteFill, Sequence, staticFile, useVideoConfig} from 'remotion';
import {hasAsset} from './assets';
import {ClipBeat, CtaBeat, EndCard, TextBeat, TitleBeat, WarningBanner, WarningBeat} from './beats/Beats';
import {GlitchIn} from './fx/GlitchIn';
import {VhsOverlay} from './fx/VhsOverlay';
import type {TrailerProps} from './schema';
import {GLITCH_FRAMES, timeline, type TimedBeat} from './storyboard/timeline';
import type {Storyboard} from './storyboard/types';
import {colors} from './theme';

const staticSounds = ['pack/vhs_static_1.ogg', 'pack/vhs_static_2.ogg', 'pack/vhs_static_3.ogg'];

const BeatView: React.FC<{timed: TimedBeat; board: Storyboard; props: TrailerProps}> = ({timed, board, props}) => {
  const {beat} = timed;
  switch (beat.kind) {
    case 'warning':
      return <WarningBeat text={props.flashWarning} />;
    case 'clip':
      return <ClipBeat beat={beat} vertical={board.vertical} />;
    case 'text':
      return <TextBeat lines={beat.lines} />;
    case 'title':
      return <TitleBeat subtitle={beat.subtitle} />;
    case 'cta':
      return <CtaBeat props={props} />;
    case 'end':
      return <EndCard props={props} />;
  }
};

export const Trailer: React.FC<TrailerProps & {board: Storyboard}> = ({board, ...props}) => {
  const {fps} = useVideoConfig();
  const beats = timeline(board, fps);
  const endFrom = beats.find((b) => b.beat.kind === 'end')?.from ?? Infinity;
  const glitchSounds = staticSounds.filter(hasAsset);
  const music = props.music && hasAsset(props.music) ? props.music : null;

  return (
    <AbsoluteFill style={{background: colors.night}}>
      {beats.map((timed, index) => (
        <Sequence key={index} from={timed.from} durationInFrames={timed.frames}>
          {timed.cut === 'glitch' ? (
            <GlitchIn seed={`cut-${index}`}>
              <BeatView timed={timed} board={board} props={props} />
            </GlitchIn>
          ) : (
            <BeatView timed={timed} board={board} props={props} />
          )}
        </Sequence>
      ))}

      <Sequence durationInFrames={Number.isFinite(endFrom) ? endFrom : undefined}>
        <VhsOverlay />
      </Sequence>

      {board.warningAsBanner ? (
        <Sequence durationInFrames={Math.round(2.5 * fps)}>
          <WarningBanner text={props.flashWarning} />
        </Sequence>
      ) : null}

      {/* The pack's own static under every glitch cut. */}
      {glitchSounds.length > 0
        ? beats
            .filter((b) => b.cut === 'glitch')
            .map((b, i) => (
              <Sequence key={`sfx-${i}`} from={b.from} durationInFrames={GLITCH_FRAMES * 2}>
                <Audio src={staticFile(glitchSounds[i % glitchSounds.length])} volume={0.5} />
              </Sequence>
            ))
        : null}

      {music ? <Audio src={staticFile(music)} volume={props.musicVolume} /> : null}
    </AbsoluteFill>
  );
};
