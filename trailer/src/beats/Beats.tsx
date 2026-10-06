import React from 'react';
import {AbsoluteFill, Img, interpolate, staticFile, useCurrentFrame, useVideoConfig} from 'remotion';
import type {Beat} from '../storyboard/types';
import type {TrailerProps} from '../schema';
import {SplitText} from '../fx/SplitText';
import {useUnit} from '../fx/useUnit';
import {colors, fonts} from '../theme';
import {Clip} from './Clip';

const useFadeIn = (frames = 8): number => {
  const frame = useCurrentFrame();
  return interpolate(frame, [0, frames], [0, 1], {extrapolateRight: 'clamp'});
};

export const WarningBeat: React.FC<{text: string}> = ({text}) => {
  const u = useUnit();
  return (
    <AbsoluteFill style={{background: colors.night, justifyContent: 'center', alignItems: 'center'}}>
      <div style={{fontFamily: fonts.mono, color: colors.bone, fontSize: 36 * u, opacity: useFadeIn(12)}}>
        ⚠ {text}
      </div>
    </AbsoluteFill>
  );
};

export const WarningBanner: React.FC<{text: string}> = ({text}) => {
  const u = useUnit();
  return (
    <div
      style={{
        position: 'absolute',
        top: 160 * u,
        left: 0,
        right: 0,
        textAlign: 'center',
        fontFamily: fonts.mono,
        fontSize: 28 * u,
        color: colors.bone,
      }}
    >
      <span style={{background: '#000000b0', padding: `${8 * u}px ${16 * u}px`}}>⚠ {text}</span>
    </div>
  );
};

const Caption: React.FC<{text: string; vertical: boolean}> = ({text, vertical}) => {
  const u = useUnit();
  return (
    <AbsoluteFill
      style={{
        justifyContent: 'flex-end',
        alignItems: 'center',
        paddingBottom: (vertical ? 420 : 110) * u,
        opacity: useFadeIn(),
      }}
    >
      <SplitText seed={text} amount={2} style={{fontFamily: fonts.sans, fontWeight: 300, fontSize: 56 * u}}>
        {text}
      </SplitText>
    </AbsoluteFill>
  );
};

export const ClipBeat: React.FC<{beat: Extract<Beat, {kind: 'clip'}>; vertical: boolean}> = ({beat, vertical}) => (
  <AbsoluteFill>
    <Clip id={beat.clip} from={beat.from} />
    {beat.caption ? <Caption text={beat.caption} vertical={vertical} /> : null}
  </AbsoluteFill>
);

export const TextBeat: React.FC<{lines: string[]}> = ({lines}) => {
  const frame = useCurrentFrame();
  const {fps} = useVideoConfig();
  const u = useUnit();
  return (
    <AbsoluteFill style={{background: colors.night, justifyContent: 'center', alignItems: 'center'}}>
      {lines.map((line, i) => {
        // Lines arrive one after another, the way a page reveals its writing.
        const start = i * fps * 0.6;
        const opacity = interpolate(frame, [start, start + 8], [0, 1], {
          extrapolateLeft: 'clamp',
          extrapolateRight: 'clamp',
        });
        return (
          <SplitText
            key={line}
            seed={line}
            style={{fontFamily: fonts.sans, fontWeight: 300, fontSize: 92 * u, lineHeight: 1.2, opacity}}
          >
            {line}
          </SplitText>
        );
      })}
    </AbsoluteFill>
  );
};

export const TitleBeat: React.FC<{subtitle?: string}> = ({subtitle}) => {
  const frame = useCurrentFrame();
  const {durationInFrames} = useVideoConfig();
  const u = useUnit();
  // The name closes in on itself over the beat, like the fog does at round start.
  const tracking = interpolate(frame, [0, durationInFrames], [0.6, 0.32]);
  const subtitleFade = useFadeIn(20);
  return (
    <AbsoluteFill style={{background: colors.night, justifyContent: 'center', alignItems: 'center'}}>
      <SplitText
        seed="title"
        amount={4}
        style={{
          fontFamily: fonts.sans,
          fontWeight: 300,
          fontSize: 170 * u,
          letterSpacing: `${tracking}em`,
          marginRight: `-${tracking}em`,
        }}
      >
        CYGNUS
      </SplitText>
      {subtitle ? (
        <div
          style={{
            fontFamily: fonts.sans,
            fontWeight: 300,
            color: colors.fog,
            fontSize: 34 * u,
            marginTop: 24 * u,
            opacity: subtitleFade,
            textAlign: 'center',
            padding: `0 ${40 * u}px`,
          }}
        >
          {subtitle}
        </div>
      ) : null}
    </AbsoluteFill>
  );
};

export const CtaBeat: React.FC<{props: TrailerProps}> = ({props}) => {
  const u = useUnit();
  const fade = useFadeIn(10);
  return (
    <AbsoluteFill
      style={{background: colors.night, justifyContent: 'center', alignItems: 'center', textAlign: 'center'}}
    >
      <SplitText seed="slot" style={{fontFamily: fonts.sans, fontWeight: 500, fontSize: 72 * u, opacity: fade}}>
        {props.slot}
      </SplitText>
      <div style={{fontFamily: fonts.sans, fontWeight: 300, color: colors.bone, fontSize: 54 * u, marginTop: 28 * u}}>
        {props.callToAction}
      </div>
      <div style={{fontFamily: fonts.mono, color: colors.fog, fontSize: 44 * u, marginTop: 48 * u}}>{props.link}</div>
    </AbsoluteFill>
  );
};

// Signed by the network: the only place the OLF palette and logo appear. The logo
// stays clear of grain, glitch and fringe (brand rules 4, 6 and 8), which is why
// Trailer.tsx switches the VHS overlay off for this beat.
export const EndCard: React.FC<{props: TrailerProps}> = ({props}) => {
  const u = useUnit();
  const fade = useFadeIn(15);
  return (
    <AbsoluteFill
      style={{background: colors.charcoal, justifyContent: 'center', alignItems: 'center', textAlign: 'center'}}
    >
      <div style={{opacity: fade, display: 'flex', flexDirection: 'column', alignItems: 'center'}}>
        <Img src={staticFile('brand/olf-logo.png')} style={{height: 190 * u, width: 'auto'}} />
        <div style={{fontFamily: fonts.sans, fontWeight: 400, color: '#FFFFFF', fontSize: 64 * u, marginTop: 28 * u}}>
          OneLiteFeather
        </div>
        <div
          style={{
            fontFamily: fonts.sans,
            fontWeight: 300,
            color: '#E5E5E5',
            fontSize: 26 * u,
            letterSpacing: '0.35em',
            marginRight: '-0.35em',
            marginTop: 8 * u,
          }}
        >
          Living Life Lite
        </div>
        <div style={{height: 6 * u, width: 360 * u, background: colors.olfGradient, marginTop: 40 * u}} />
        <div style={{fontFamily: fonts.mono, color: '#E5E5E5', fontSize: 38 * u, marginTop: 40 * u}}>{props.link}</div>
        <div style={{fontFamily: fonts.sans, fontWeight: 300, color: '#E5E5E5', fontSize: 24 * u, marginTop: 18 * u}}>
          Open Source · github.com/OneLiteFeatherNET/Cygnus
        </div>
      </div>
      <div
        style={{
          position: 'absolute',
          bottom: 40 * u,
          left: 60 * u,
          right: 60 * u,
          fontFamily: fonts.sans,
          fontWeight: 300,
          color: '#E5E5E5aa',
          fontSize: 18 * u,
        }}
      >
        {props.minecraftDisclaimer}
      </div>
    </AbsoluteFill>
  );
};
