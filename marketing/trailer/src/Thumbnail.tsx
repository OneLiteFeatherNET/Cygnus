import {AbsoluteFill} from 'remotion';
import {CameraOverlay} from './components/CameraOverlay';
import {Forest} from './components/Forest';
import {Noise, Scanlines} from './components/Noise';
import {Vignette} from './components/Vignette';
import {copy, TrailerProps} from './props';
import {colors, olfGradient, roboto} from './theme';

// YouTube-Thumbnail / Listing-Banner: Wald, angedeutete Gestalt, Titel. Lesbar ab 320 px Breite.
export const Thumbnail: React.FC<TrailerProps> = ({language, title}) => {
  const t = copy[language];
  return (
    <AbsoluteFill>
      <Forest seed="thumb" figure={1} />
      <Vignette stage={16} />
      <Noise opacity={0.03} seed="thumb" />
      <Scanlines opacity={0.12} />
      <CameraOverlay />
      <AbsoluteFill style={{justifyContent: 'center', paddingLeft: 140, fontFamily: roboto, color: colors.white}}>
        <div style={{fontSize: 230, fontWeight: 900, letterSpacing: 30, lineHeight: 1, textShadow: '0 0 40px rgba(0,0,0,0.9)'}}>{title}</div>
        <div style={{width: 420, height: 8, background: olfGradient, margin: '30px 0'}} />
        <div style={{fontSize: 56, fontWeight: 300, letterSpacing: 2, textShadow: '0 0 24px rgba(0,0,0,0.9)'}}>{t.hunter.join(' ')}</div>
      </AbsoluteFill>
    </AbsoluteFill>
  );
};
