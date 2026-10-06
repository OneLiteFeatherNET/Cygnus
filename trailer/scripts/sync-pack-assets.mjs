// Copies the pack's own sounds into public/pack/ so the trailer sounds like the game.
// Usage: npm run pack-assets [-- <path to a cygnus-pack checkout>]
// Defaults to a cygnus-pack checkout next to this repository.
import {cpSync, existsSync, mkdirSync, readdirSync} from 'node:fs';
import {join, resolve} from 'node:path';
import {fileURLToPath} from 'node:url';

const root = fileURLToPath(new URL('..', import.meta.url));
const pack = resolve(process.argv[2] ?? join(root, '..', '..', 'cygnus-pack'));
const sounds = join(pack, 'pack', 'assets', 'cygnus', 'sounds');

if (!existsSync(sounds)) {
  console.error(`No pack sounds at ${sounds}. Pass the path to a cygnus-pack checkout.`);
  process.exit(1);
}

const target = join(root, 'public', 'pack');
mkdirSync(target, {recursive: true});
const copied = readdirSync(sounds).filter((name) => name.endsWith('.ogg'));
for (const name of copied) {
  cpSync(join(sounds, name), join(target, name));
}
console.log(`pack-assets: copied ${copied.length} sound(s) from ${sounds}`);
