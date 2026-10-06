import {Config} from '@remotion/cli/config';

Config.setVideoImageFormat('jpeg');
Config.setJpegQuality(90);
Config.setCodec('h264');
// Platforms re-encode anyway; a high source quality survives that better.
Config.setCrf(18);
Config.setOverwriteOutput(true);
