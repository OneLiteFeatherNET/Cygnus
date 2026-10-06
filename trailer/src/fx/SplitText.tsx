import React from 'react';
import {random, useCurrentFrame} from 'remotion';
import {colors} from '../theme';

// Type with the dread shader's colour fringe. The split wanders a little, every
// other frame, so the text never sits quite still.
export const SplitText: React.FC<{
  children: React.ReactNode;
  style?: React.CSSProperties;
  seed: string;
  amount?: number;
}> = ({children, style, seed, amount = 3}) => {
  const frame = useCurrentFrame();
  const wobble = random(`${seed}-${Math.floor(frame / 2)}`) * amount;
  const split = amount + wobble;
  return (
    <div
      style={{
        color: colors.bone,
        textShadow: `${-split}px 0 ${colors.fringeRed}aa, ${split}px 0 ${colors.fringeCyan}aa`,
        ...style,
      }}
    >
      {children}
    </div>
  );
};
