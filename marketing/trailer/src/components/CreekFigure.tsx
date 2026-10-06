// Der Creek als Silhouette: im Spiel ein umgekleideter Creaking (game/…/creek/body/CreakingBody.java),
// also hoch, dünn, aus Rinde, mit zwei glimmenden Augen. Blockig gezeichnet, damit es nach Minecraft
// aussieht. Kein Gesicht, keine Wunden (Jugendschutz): der Schreck sind die Augen und die Nähe.
const BARK = '#2a2420';
const BARK_LIGHT = '#3b322b';
const EYE = '#ff8a1f';

export const CreekFigure: React.FC<{height: number; eyes?: number; lean?: number; style?: React.CSSProperties}> = ({
  height,
  eyes = 1,
  lean = 0,
  style,
}) => {
  // Raster: 12 Einheiten breit, 30 hoch (ein Creaking ist knapp drei Blöcke hoch).
  const u = height / 30;
  const r = (x: number, y: number, w: number, h: number, fill: string, key?: string) => (
    <rect key={key} x={x * u} y={y * u} width={w * u} height={h * u} fill={fill} />
  );
  return (
    <svg width={12 * u} height={30 * u} style={{overflow: 'visible', transform: `rotate(${lean}deg)`, transformOrigin: '50% 100%', ...style}}>
      {/* Kopf */}
      {r(3, 0, 6, 6, BARK)}
      {r(2, 1, 1, 2, BARK_LIGHT)}
      {r(9, 0, 1, 3, BARK_LIGHT)}
      {/* Augen mit Glimmen */}
      <defs>
        <filter id="creek-glow" x="-200%" y="-200%" width="500%" height="500%">
          <feGaussianBlur stdDeviation={u * 0.9} />
        </filter>
      </defs>
      <g opacity={eyes}>
        <rect x={4 * u} y={2.5 * u} width={1.2 * u} height={1.2 * u} fill={EYE} filter="url(#creek-glow)" />
        <rect x={6.8 * u} y={2.5 * u} width={1.2 * u} height={1.2 * u} fill={EYE} filter="url(#creek-glow)" />
        {r(4, 2.5, 1.2, 1.2, EYE)}
        {r(6.8, 2.5, 1.2, 1.2, EYE)}
      </g>
      {/* Rumpf, schmal */}
      {r(4, 6, 4, 11, BARK)}
      {r(5, 8, 1, 4, BARK_LIGHT)}
      {/* Arme, zu lang, mit Ästen */}
      {r(1, 6, 3, 2, BARK)}
      {r(1, 8, 2, 11, BARK)}
      {r(0, 18, 1, 3, BARK)}
      {r(8, 6, 3, 2, BARK)}
      {r(9, 8, 2, 12, BARK)}
      {r(11, 19, 1, 3, BARK)}
      {r(11, 4, 1, 3, BARK_LIGHT)}
      {/* Beine */}
      {r(4, 17, 1.6, 13, BARK)}
      {r(6.4, 17, 1.6, 13, BARK)}
    </svg>
  );
};
