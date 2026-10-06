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
  // Hochkant ist die Fläche schmaler, aber auf dem Handy näher am Auge: Text größer.
  const unit = (Math.min(width, height) / 1080) * (vertical ? 1.3 : 1);
  const t = copy[props.language];
  const title = spring({frame, fps, config: {damping: 200}, durationInFrames: 30});
  const rest = interpolate(frame, [20, 40], [0, 1], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'});
  const cta = interpolate(frame, [40, 60], [0, 1], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'});

  return (
    <AbsoluteFill style={{background: colors.charcoal, fontFamily: roboto, color: colors.white}}>
      <AbsoluteFill style={{justifyContent: 'center', alignItems: 'center', textAlign: 'center', gap: 0, padding: 80 * unit}}>
        <div
          style={{
            fontSize: (vertical ? 150 : 210) * unit,
            fontWeight: 900,
            letterSpacing: (vertical ? 14 : 34) * unit,
            marginRight: -(vertical ? 14 : 34) * unit,
            lineHeight: 1,
            opacity: title,
            transform: `scale(${0.94 + title * 0.06})`,
          }}
        >
          {props.title}
        </div>
        <div style={{width: 360 * unit * title, height: 6 * unit, background: olfGradient, margin: `${28 * unit}px 0`}} />
        <div style={{fontSize: (vertical ? 36 : 44) * unit, fontWeight: 300, letterSpacing: 2, opacity: rest}}>{t.claim}</div>
        <div
          style={{
            display: 'flex',
            flexDirection: vertical ? 'column' : 'row',
            gap: (vertical ? 14 : 40) * unit,
            marginTop: 36 * unit,
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
            marginTop: 56 * unit,
            opacity: cta,
            display: 'flex',
            flexDirection: vertical ? 'column' : 'row',
            gap: (vertical ? 18 : 64) * unit,
            fontSize: 38 * unit,
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
        <div style={{marginTop: 22 * unit, fontSize: 30 * unit, color: colors.text, opacity: cta}}>
          {vertical ? (
            <>
              <div>{props.availability}</div>
              <div>{props.sessionHint}</div>
            </>
          ) : (
            `${props.availability} · ${props.sessionHint}`
          )}
        </div>
      </AbsoluteFill>
      <AbsoluteFill style={{justifyContent: 'flex-end', alignItems: 'center', paddingBottom: (vertical ? 160 : 60) * unit, opacity: cta}}>
        <OlfLockup scale={unit} />
      </AbsoluteFill>
    </AbsoluteFill>
  );
};
