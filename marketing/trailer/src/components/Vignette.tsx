import {AbsoluteFill, Img, staticFile} from 'remotion';

// Tunnel-Vision-Textur direkt aus dem Pack (gleiche Maske wie im Spiel, multipliziert).
export const Vignette: React.FC<{stage?: 4 | 16 | 28}> = ({stage = 16}) => (
  <AbsoluteFill style={{mixBlendMode: 'multiply', pointerEvents: 'none'}}>
    <Img src={staticFile(`pack/tunnel_vision/stage_${stage}.png`)} style={{width: '100%', height: '100%', objectFit: 'fill'}} />
  </AbsoluteFill>
);
