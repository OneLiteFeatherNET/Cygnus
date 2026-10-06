import React from 'react';
import {Composition} from 'remotion';
import {defaultTrailerProps, trailerSchema} from './schema';
import {mainTrailer} from './storyboard/main';
import {shortTrailer} from './storyboard/short';
import {teaser} from './storyboard/teaser';
import {totalFrames, validate} from './storyboard/timeline';
import type {Storyboard} from './storyboard/types';
import {FPS} from './theme';
import {Trailer} from './Trailer';

type Props = typeof defaultTrailerProps;

const variant = (id: string, board: Storyboard) => {
  validate(id, board, FPS);
  const component: React.FC<Props> = (props) => <Trailer {...props} board={board} />;
  return {id, board, component};
};

const variants = [
  variant('CygnusTrailer', mainTrailer),
  variant('CygnusShort', shortTrailer),
  variant('CygnusTeaser', teaser),
];

export const RemotionRoot: React.FC = () => (
  <>
    {variants.map(({id, board, component}) => {
      return (
        <Composition
          key={id}
          id={id}
          component={component}
          schema={trailerSchema}
          defaultProps={defaultTrailerProps}
          durationInFrames={totalFrames(board, FPS)}
          fps={FPS}
          width={board.vertical ? 1080 : 1920}
          height={board.vertical ? 1920 : 1080}
        />
      );
    })}
  </>
);
