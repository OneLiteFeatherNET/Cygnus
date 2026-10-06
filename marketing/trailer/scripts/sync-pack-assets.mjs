// Kopiert die im Trailer verwendeten Texturen und Sounds aus einem cygnus-pack-Checkout.
// Aufruf: npm run sync-pack [-- /pfad/zu/cygnus-pack]   (Default: ../../../cygnus-pack)
import {cpSync, mkdirSync, readdirSync} from 'node:fs';
import {join, resolve} from 'node:path';

const packRoot = resolve(process.argv[2] ?? '../../../cygnus-pack');
const assets = join(packRoot, 'pack/assets/cygnus');
const target = resolve('public/pack');

const copies = [
  ['textures/item/page', 'page', (f) => f.endsWith('.png')],
  ['textures/font/icons', 'icons', (f) => ['page.png', 'clock.png', 'ghost.png', 'map.png', 'flashlight.png'].includes(f)],
  ['sounds', 'sounds', (f) => f.endsWith('.ogg')],
  ['textures/gui/tunnel_vision', 'tunnel_vision', (f) => ['stage_4.png', 'stage_16.png', 'stage_28.png'].includes(f)],
  ['../minecraft/textures/environment/celestial/moon', 'moon', (f) => f === 'full_moon.png'],
];

for (const [from, to, keep] of copies) {
  mkdirSync(join(target, to), {recursive: true});
  for (const file of readdirSync(join(assets, from)).filter(keep)) {
    cpSync(join(assets, from, file), join(target, to, file));
    console.log(`${from}/${file} -> public/pack/${to}/${file}`);
  }
}
