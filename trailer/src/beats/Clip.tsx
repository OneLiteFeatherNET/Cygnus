import React from 'react';
import {Video} from '@remotion/media';
import {AbsoluteFill, staticFile, useVideoConfig} from 'remotion';
import {clipFile} from '../assets';
import {clips, type ClipId} from '../clips';
import {Noise} from '../fx/Noise';
import {useUnit} from '../fx/useUnit';
import {colors, fonts} from '../theme';

// A recorded shot, or - until it has been recorded - a placeholder that says which
// shot belongs here, so a preview doubles as the to-do list for the recording.
export const Clip: React.FC<{id: ClipId; from?: number}> = ({id, from = 0}) => {
  const {fps} = useVideoConfig();
  const file = clipFile(id);
  if (file) {
    return (
      <AbsoluteFill>
        <Video
          src={staticFile(file)}
          trimBefore={Math.round(from * fps)}
          muted
          objectFit="cover"
          style={{width: '100%', height: '100%'}}
        />
      </AbsoluteFill>
    );
  }
  return <Placeholder id={id} />;
};

const Placeholder: React.FC<{id: ClipId}> = ({id}) => {
  const u = useUnit();
  return (
    <AbsoluteFill
      style={{
        background: `radial-gradient(ellipse at 50% 60%, #1b2226 0%, ${colors.night} 70%)`,
        justifyContent: 'center',
        alignItems: 'center',
      }}
    >
      <Noise opacity={0.25} id={`ph-${id}`} />
      <div
        style={{
          fontFamily: fonts.mono,
          color: colors.fog,
          border: `${2 * u}px dashed ${colors.fog}66`,
          padding: `${24 * u}px ${32 * u}px`,
          maxWidth: '78%',
          textAlign: 'center',
        }}
      >
        <div style={{fontSize: 26 * u, letterSpacing: '0.2em'}}>AUFNAHME FEHLT · {id}</div>
        <div style={{fontSize: 30 * u, marginTop: 14 * u, color: colors.bone, lineHeight: 1.35}}>
          {clips[id]}
        </div>
        <div style={{fontSize: 20 * u, marginTop: 14 * u, opacity: 0.7}}>public/clips/{id}.mp4</div>
      </div>
    </AbsoluteFill>
  );
};
