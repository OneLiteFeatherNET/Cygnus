import {loadFont} from '@remotion/fonts';
import {cancelRender, continueRender, delayRender} from 'remotion';
import roboto300 from '@fontsource/roboto/files/roboto-latin-300-normal.woff2';
import roboto400 from '@fontsource/roboto/files/roboto-latin-400-normal.woff2';
import roboto500 from '@fontsource/roboto/files/roboto-latin-500-normal.woff2';
import robotoMono400 from '@fontsource/roboto-mono/files/roboto-mono-latin-400-normal.woff2';

// Fonts ship with the bundle instead of coming from Google Fonts at render time,
// so a render works offline and behind proxies, and looks the same every time.
export const fonts = {
  sans: 'Roboto',
  mono: 'Roboto Mono',
};

const handle = delayRender('Loading fonts');
Promise.all([
  loadFont({family: fonts.sans, url: roboto300, weight: '300'}),
  loadFont({family: fonts.sans, url: roboto400, weight: '400'}),
  loadFont({family: fonts.sans, url: roboto500, weight: '500'}),
  loadFont({family: fonts.mono, url: robotoMono400, weight: '400'}),
])
  .then(() => continueRender(handle))
  .catch((err: unknown) => cancelRender(err));

// The trailer lives in the game's colours; the OLF palette only appears on the end
// card, where the network signs it.
export const colors = {
  night: '#050608',
  fog: '#9aa3a8',
  bone: '#e5e5e5',
  // The dread shader's colour fringe, used for the chromatic split on type.
  fringeRed: '#ff2a3d',
  fringeCyan: '#27e1d4',
  charcoal: '#1F1F23',
  olfGradient: 'linear-gradient(135deg, #F7931D 0%, #EC008B 35%, #91268F 70%, #2A388F 100%)',
};

export const FPS = 30;
