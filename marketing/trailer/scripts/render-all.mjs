// Rendert alle Varianten in allen Formaten und Sprachen nach out/<sprache>/<variante>-<format>.mp4,
// dazu die Thumbnails. Das Bundle wird einmal gebaut und für alle Renders benutzt.
//
//   npm run render:all                         alles (5 Varianten × 3 Formate × 2 Sprachen)
//   npm run render:all -- --lang=de            nur Deutsch
//   npm run render:all -- --format=vertical    nur 9:16
//   npm run render:all -- --variant=creek      nur eine Variante
//   npm run render:all -- --props=kanal.json   eigene Props (z. B. anderer Discord-Kurzlink pro Plattform)
//
// Ohne Download-Zugang für Chrome: REMOTION_BROWSER_EXECUTABLE=/pfad/zu/headless_shell
import {bundle} from '@remotion/bundler';
import {getCompositions, renderMedia, renderStill} from '@remotion/renderer';
import {mkdirSync, readFileSync} from 'node:fs';
import {resolve} from 'node:path';

const args = Object.fromEntries(
  process.argv
    .slice(2)
    .filter((a) => a.startsWith('--'))
    .map((a) => a.slice(2).split('=')),
);
const languages = args.lang ? args.lang.split(',') : ['de', 'en'];
const extraProps = args.props ? JSON.parse(readFileSync(args.props, 'utf8')) : {};
const browserExecutable = process.env.REMOTION_BROWSER_EXECUTABLE ?? null;

const serveUrl = await bundle({entryPoint: resolve('src/index.ts')});

for (const language of languages) {
  const inputProps = {...extraProps, language};
  const outDir = resolve('out', language);
  mkdirSync(outDir, {recursive: true});
  const compositions = await getCompositions(serveUrl, {inputProps, browserExecutable});

  for (const composition of compositions) {
    const [variant, format] = composition.id.split(/-(?=[^-]+$)/);
    if (args.format && format !== args.format) continue;
    const isStill = composition.durationInFrames === 1;
    if (args.variant && !isStill && variant !== args.variant) continue;

    const output = resolve(outDir, `${composition.id}.${isStill ? 'png' : 'mp4'}`);
    const started = Date.now();
    if (isStill) {
      await renderStill({composition, serveUrl, output, inputProps, browserExecutable});
    } else {
      await renderMedia({composition, serveUrl, codec: 'h264', crf: 24, outputLocation: output, inputProps, browserExecutable});
    }
    console.log(`${output} (${((Date.now() - started) / 1000).toFixed(0)} s)`);
  }
}
