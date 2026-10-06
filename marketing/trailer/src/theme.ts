import {loadFont} from '@remotion/fonts';
import {staticFile} from 'remotion';

// Roboto ist OLF-Markenschrift; Mono nur für das Kamera-Overlay (Timestamp, REC).
// Lokal eingebunden (public/fonts, SIL OFL), damit Renders offline und reproduzierbar laufen.
export const roboto = 'Roboto';
export const robotoMono = 'Roboto Mono';

for (const weight of ['300', '400', '700', '900']) {
  loadFont({family: roboto, url: staticFile(`fonts/roboto-latin-${weight}-normal.woff2`), weight});
}
loadFont({family: robotoMono, url: staticFile('fonts/roboto-mono-latin-400-normal.woff2'), weight: '400'});

export const colors = {
  // Brand-Neutrals und -Akzente, keine freien Farben (onelitefeather-brand).
  charcoal: '#1F1F23',
  black: '#000000',
  text: '#E5E5E5',
  white: '#FFFFFF',
  pink: '#EC008B',
  purple: '#91268F',
  deepBlue: '#2A388F',
  orange: '#F7931D',
  cyan: '#27A9E1',
};

export const olfGradient = `linear-gradient(135deg, ${colors.orange} 0%, ${colors.pink} 35%, ${colors.purple} 70%, ${colors.deepBlue} 100%)`;

export const FPS = 30;
