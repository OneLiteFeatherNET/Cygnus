import {AbsoluteFill, Img, interpolate, spring, staticFile, useCurrentFrame, useVideoConfig} from 'remotion';
import {OlfLockup} from '../components/OlfLockup';
import {copy, TrailerProps} from '../props';
import {colors, olfGradient, roboto} from '../theme';

// Abschluss auf Charcoal: Titel, Claim, drei belegte Fakten, CTA, Absender.
// Bewusst ohne Rauschen: ruhige Fläche, das Logo wird nie verfremdet.
export const EndCard: React.FC<TrailerProps & {durationInFrames: number}> = (props) => {
  const frame = useCurrentFrame();
  const {fps, width, height} = useVideoConfig();
  const vertical = height > width;
  const narrow = width < 1500;
  // Hochkant ist die Fläche schmaler, aber auf dem Handy näher am Auge: Text größer.
  const unit = (Math.min(width, height) / 1080) * (vertical ? 1.3 : 1);
  const t = copy[props.language];
  // Titelgröße aus der Breite: Roboto Black in Versalien plus Sperrung braucht ~0.88 em pro Zeichen.
  const titleSize = Math.min(210, (width * 0.82) / (Math.max(4, props.title.length) * 0.88));
  const title = spring({frame, fps, config: {damping: 200}, durationInFrames: 30});
  const rest = interpolate(frame, [20, 40], [0, 1], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'});
  const cta = interpolate(frame, [40, 60], [0, 1], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'});

  return (
    <AbsoluteFill style={{background: colors.charcoal, fontFamily: roboto, color: colors.white}}>
      <AbsoluteFill
        style={{
          justifyContent: 'center',
          alignItems: 'center',
          textAlign: 'center',
          padding: 60 * unit,
          paddingBottom: (vertical ? 260 : narrow ? 170 : 120) * unit,
        }}
      >
        <div
          style={{
            fontSize: titleSize,
            fontWeight: 900,
            letterSpacing: titleSize * 0.16,
            marginRight: -titleSize * 0.16,
            lineHeight: 1,
            opacity: title,
            transform: `scale(${0.94 + title * 0.06})`,
          }}
        >
          {props.title}
        </div>
        <div style={{width: 360 * unit * title, height: 6 * unit, background: olfGradient, margin: `${(narrow ? 22 : 28) * unit}px 0`}} />
        <div style={{fontSize: (narrow ? 36 : 44) * unit, fontWeight: 300, letterSpacing: 2, opacity: rest}}>{t.claim}</div>
        <div
          style={{
            display: 'flex',
            flexDirection: narrow ? 'column' : 'row',
            gap: (narrow ? 10 : 40) * unit,
            marginTop: (narrow ? 26 : 36) * unit,
            fontSize: 30 * unit,
            color: colors.text,
            opacity: rest,
          }}
        >
          {t.facts.map((f) => (
            <div key={f} style={{display: 'flex', alignItems: 'center', gap: 12 * unit, justifyContent: 'center'}}>
              <Img src={staticFile('pack/icons/page.png')} style={{width: 30 * unit, height: 30 * unit, imageRendering: 'pixelated'}} />
              {f}
            </div>
          ))}
        </div>
        <div
          style={{
            marginTop: (narrow ? 36 : 56) * unit,
            opacity: cta,
            display: 'flex',
            flexDirection: narrow ? 'column' : 'row',
            gap: (narrow ? 12 : 64) * unit,
            fontSize: 40 * unit,
          }}
        >
          <div>
            <span style={{color: colors.text, fontWeight: 300}}>{t.ctaServer}: </span>
            <span style={{fontWeight: 700}}>{props.serverAddress}</span>
          </div>
          <div>
            <span style={{color: colors.text, fontWeight: 300}}>{t.ctaDiscord}: </span>
            <span style={{fontWeight: 700}}>{props.discordUrl}</span>
          </div>
        </div>
        {props.availability ? (
          <div style={{marginTop: 20 * unit, fontSize: 30 * unit, color: colors.text, opacity: cta}}>{props.availability}</div>
        ) : null}
      </AbsoluteFill>
      <AbsoluteFill style={{justifyContent: 'flex-end', alignItems: 'center', paddingBottom: (vertical ? 160 : 50) * unit, opacity: cta}}>
        <OlfLockup scale={unit} />
      </AbsoluteFill>
    </AbsoluteFill>
  );
};
