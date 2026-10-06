import {Img, staticFile} from 'remotion';
import {colors, roboto} from '../theme';

// Reversed-Variante (weiße Wortmarke) für dunkle Flächen. Feder unverändert:
// kein Schatten, keine Drehung, kein Glitch, kein Blur (Markenregeln 1-8).
export const OlfLockup: React.FC<{scale: number}> = ({scale}) => (
  <div style={{display: 'flex', alignItems: 'center', gap: 22 * scale}}>
    <Img src={staticFile('brand/olf-logo.png')} style={{height: 76 * scale, width: 'auto'}} />
    <div style={{fontFamily: roboto, color: colors.white, textAlign: 'left'}}>
      <div style={{fontSize: 40 * scale, fontWeight: 400, lineHeight: 1.1}}>OneLiteFeather</div>
      <div style={{fontSize: 17 * scale, fontWeight: 300, letterSpacing: 6 * scale, color: colors.text, marginTop: 4 * scale}}>Living Life Lite</div>
    </div>
  </div>
);
