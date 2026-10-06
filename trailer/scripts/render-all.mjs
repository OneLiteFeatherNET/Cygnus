// Renders every trailer composition to out/. Extra arguments go to `remotion render`,
// e.g. `npm run render -- --props=props/halloween.json`.
import {spawnSync} from 'node:child_process';

const compositions = ['CygnusTrailer', 'CygnusShort', 'CygnusTeaser'];
const extra = process.argv.slice(2);

for (const id of compositions) {
  const result = spawnSync(
    'npx',
    ['remotion', 'render', 'src/index.ts', id, `out/${id}.mp4`, ...extra],
    {stdio: 'inherit'},
  );
  if (result.status !== 0) {
    process.exit(result.status ?? 1);
  }
}
