import {AbsoluteFill, useVideoConfig} from 'remotion';
import {CameraOverlay} from './components/CameraOverlay';
import {CreekFigure} from './components/CreekFigure';
import {Forest} from './components/Forest';
import {Noise, Scanlines} from './components/Noise';
import {Vignette} from './components/Vignette';
import {copy, TrailerProps} from './props';
import {colors, olfGradient, roboto} from './theme';

// Thumbnail / Cover: Wald, der Creek mit glimmenden Augen, Titel. Lesbar ab 320 px Breite.
export const Thumbnail: React.FC<TrailerProps> = ({language, title}) => {
  const {width, height} = useVideoConfig();
  const landscape = width >= 1500;
  const t = copy[language];
  const titleSize = Math.min(230, (width * 0.8) / (Math.max(4, title.length) * 0.88));
  const square = !landscape && height <= width;
  const figure = height * (landscape ? 0.75 : square ? 0.45 : 0.5);
  return (
    <AbsoluteFill>
      <Forest seed="thumb" />
      <div style={{position: 'absolute', left: landscape ? width * 0.7 : width / 2 - figure * 0.2, bottom: landscape ? height * 0.05 : square ? -height * 0.05 : height * 0.08}}>
        <CreekFigure height={figure} />
      </div>
      <Vignette stage={16} />
      <Noise opacity={0.03} seed="thumb" />
      <Scanlines opacity={0.12} />
      <CameraOverlay />
      <AbsoluteFill
        style={{
          justifyContent: landscape ? 'center' : 'flex-start',
          alignItems: landscape ? 'flex-start' : 'center',
          paddingLeft: landscape ? 140 : 0,
          paddingTop: landscape ? 0 : height * 0.18,
          fontFamily: roboto,
          color: colors.white,
          textAlign: landscape ? 'left' : 'center',
        }}
      >
        <div style={{fontSize: titleSize, fontWeight: 900, letterSpacing: titleSize * 0.13, lineHeight: 1, textShadow: '0 0 40px rgba(0,0,0,0.9)'}}>{title}</div>
        <div style={{width: 420, height: 8, background: olfGradient, margin: '30px 0'}} />
        <div style={{fontSize: landscape ? 56 : 52, fontWeight: 300, letterSpacing: 1, textShadow: '0 0 24px rgba(0,0,0,0.9)', whiteSpace: 'pre-line'}}>
          {t.hooks.creek}
        </div>
      </AbsoluteFill>
    </AbsoluteFill>
  );
};
