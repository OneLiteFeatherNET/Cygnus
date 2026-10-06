import {AbsoluteFill, OffthreadVideo, staticFile} from 'remotion';
import {Forest} from './Forest';

// Echte Aufnahme, wenn vorhanden; sonst Platzhalter. Ton der Aufnahme bleibt stumm,
// die Tonspur des Trailers wird zentral gemischt.
export const Footage: React.FC<{src?: string; seed: string; figure?: number}> = ({src, seed, figure}) => {
  if (!src) return <Forest seed={seed} figure={figure} />;
  return (
    <AbsoluteFill style={{background: 'black'}}>
      <OffthreadVideo src={staticFile(src)} muted style={{width: '100%', height: '100%', objectFit: 'cover'}} />
    </AbsoluteFill>
  );
};
