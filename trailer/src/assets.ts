import {availableAssets} from './generated/assets';

export const hasAsset = (path: string): boolean => availableAssets.includes(path);

export const clipFile = (id: string): string | null => {
  for (const ext of ['mp4', 'webm', 'mov', 'mkv']) {
    const path = `clips/${id}.${ext}`;
    if (hasAsset(path)) {
      return path;
    }
  }
  return null;
};
