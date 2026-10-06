import {Config} from '@remotion/cli/config';

Config.setVideoImageFormat('jpeg');
Config.setCodec('h264');
// Plattformen re-encoden ohnehin; CRF 18 hält Rauschen und dunkle Verläufe sauber.
Config.setCrf(18);
// Der Look lebt von Schwarzwerten, deshalb kein Kompressionsbanding im Dunkeln.
Config.setColorSpace('bt709');

// In Umgebungen ohne Download-Zugang (CI, Cloud-Container) einen vorhandenen Chromium nutzen:
// REMOTION_BROWSER_EXECUTABLE=/opt/pw-browsers/chromium_headless_shell-1194/chrome-linux/headless_shell
if (process.env.REMOTION_BROWSER_EXECUTABLE) {
  Config.setBrowserExecutable(process.env.REMOTION_BROWSER_EXECUTABLE);
}
