import {Composition, Still} from 'remotion';
import {defaultProps, TrailerProps} from './props';
import {Thumbnail} from './Thumbnail';
import {cutDuration, shortCut, teaserCut, Trailer} from './Trailer';
import {FPS} from './theme';

const Teaser: React.FC<TrailerProps> = (p) => <Trailer {...p} cut={teaserCut} />;
const Short: React.FC<TrailerProps> = (p) => <Trailer {...p} cut={shortCut} hook />;

export const RemotionRoot: React.FC = () => (
  <>
    {/* YouTube, Discord-Ankündigung, Server-Listing: 16:9, ~27 s */}
    <Composition
      id="CygnusTeaser"
      component={Teaser}
      durationInFrames={cutDuration(teaserCut)}
      fps={FPS}
      width={1920}
      height={1080}
      defaultProps={defaultProps}
    />
    {/* TikTok, YouTube Shorts, Instagram Reels: 9:16, ~20 s, Hook statt Rundenlänge */}
    <Composition
      id="CygnusShort"
      component={Short}
      durationInFrames={cutDuration(shortCut)}
      fps={FPS}
      width={1080}
      height={1920}
      defaultProps={{...defaultProps, music: 'music/cygnus-short.ogg'}}
    />
    <Still id="CygnusThumbnail" component={Thumbnail} width={1920} height={1080} defaultProps={defaultProps} />
  </>
);
