import {Composition, Folder, Still} from 'remotion';
import {defaultProps, TrailerProps} from './props';
import {Thumbnail} from './Thumbnail';
import {Trailer} from './Trailer';
import {formats, variants, variantDuration} from './variants';
import {FPS} from './theme';

// Jede Variante in jedem Format: IDs wie "creek-vertical". Sprache und Links kommen über Props.
const components = Object.fromEntries(
  variants.map((variant) => [variant.id, ((p: TrailerProps) => <Trailer {...p} variant={variant} />) as React.FC<TrailerProps>]),
);

export const RemotionRoot: React.FC = () => (
  <>
    {formats.map((format) => (
      <Folder key={format.id} name={format.id}>
        {variants.map((variant) => (
          <Composition
            key={variant.id}
            id={`${variant.id}-${format.id}`}
            component={components[variant.id]}
            durationInFrames={variantDuration(variant)}
            fps={FPS}
            width={format.width}
            height={format.height}
            defaultProps={defaultProps}
          />
        ))}
      </Folder>
    ))}
    <Folder name="stills">
      <Still id="thumbnail-landscape" component={Thumbnail} width={1920} height={1080} defaultProps={defaultProps} />
      <Still id="thumbnail-vertical" component={Thumbnail} width={1080} height={1920} defaultProps={defaultProps} />
      <Still id="thumbnail-square" component={Thumbnail} width={1080} height={1080} defaultProps={defaultProps} />
    </Folder>
  </>
);
