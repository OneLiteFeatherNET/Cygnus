import {z} from 'zod';

// Everything a campaign changes without touching code: dates, link, wording.
// Editable in Remotion Studio's props panel, or per render with --props=<file>.json.
export const trailerSchema = z.object({
  // When people can play. Cygnus needs a group, so the trailer sells a time slot,
  // not "join whenever" - an empty lobby loses the player for good.
  slot: z.string(),
  // Where the click goes. One 1lf.link slug per channel, see docs/distribution.md.
  link: z.string(),
  // Headline of the call to action.
  callToAction: z.string(),
  // Shown in the first seconds. The game warns about flashing before every round;
  // the trailer does the same.
  flashWarning: z.string(),
  // Required by the Minecraft Usage Guidelines on anything promotional.
  minecraftDisclaimer: z.string(),
  // Background track under public/audio/, or empty for the pack's static only.
  music: z.string(),
  musicVolume: z.number().min(0).max(1),
});

export type TrailerProps = z.infer<typeof trailerSchema>;

export const defaultTrailerProps: TrailerProps = {
  slot: 'Halloween · Fr & Sa · 20 Uhr',
  link: '1lf.link/discord',
  callToAction: 'Bring drei Freunde mit.',
  flashWarning: 'Enthält flackernde Bildeffekte.',
  minecraftDisclaimer:
    'Kein offizielles Minecraft-Produkt. Nicht von Mojang oder Microsoft genehmigt oder mit ihnen verbunden.',
  music: '',
  musicVolume: 0.6,
};
