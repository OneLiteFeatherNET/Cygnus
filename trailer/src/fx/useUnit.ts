import {useVideoConfig} from 'remotion';

// One unit is a pixel at 1080 on the short side, so 16:9 and 9:16 share type sizes.
export const useUnit = (): number => {
  const {width, height} = useVideoConfig();
  return Math.min(width, height) / 1080;
};
