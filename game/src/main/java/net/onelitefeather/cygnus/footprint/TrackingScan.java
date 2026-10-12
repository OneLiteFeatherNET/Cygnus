package net.onelitefeather.cygnus.footprint;

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.SetCooldownPacket;
import net.minestom.server.sound.SoundEvent;
import net.onelitefeather.cygnus.common.config.FootprintConfig;
import net.onelitefeather.cygnus.team.TeamHelper;
import net.onelitefeather.cygnus.utils.Items;

import java.util.List;
import java.util.function.LongSupplier;
import java.util.random.RandomGenerator;

/**
 * The slender's tracker: shows him where survivors walked around him, then needs a cooldown.
 * <p>
 * The newest seconds are left out, so the prints never lead straight to a survivor. If no track
 * lies inside the radius, a short scent line points roughly towards the nearest one instead, and
 * the tracker only needs the shorter scent cooldown. A scan that finds nothing at all costs that
 * shorter cooldown as well. That is an answer too, and it keeps the tracker from being a free probe. The cooldown belongs to the slender role, so a new slender after a
 * takeover inherits what is left of it. A round starts with the tracker locked for a while: all
 * survivors start at the same spot, and an early scan would show where each of them ran off to.
 * </p>
 *
 * @author Joltra
 * @version 1.0.0
 * @since 2.16.0
 */
final class TrackingScan {

    /** The client files the cooldown under the tracker's item key, it has no cooldown component. */
    static final String COOLDOWN_GROUP = Items.TRACKING_MATERIAL.key().asString();

    static final Component NOTHING_FOUND = Component.text("No tracks nearby", NamedTextColor.GRAY, TextDecoration.ITALIC);

    private static final Sound DENIED = Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_BASS, Sound.Source.MASTER, 0.6F, 0.5F);
    private static final double TICK_MILLIS = 50.0D;

    private final FootprintConfig config;
    private final SurvivorTrackLog log;
    private final FootprintSpawner spawner;
    private final LongSupplier clock;
    private final RandomGenerator random;
    private long readyAt;

    /**
     * Creates the scan.
     *
     * @param config  the radius, the gap, the maximum, the lifetime and the cooldown
     * @param log     the recorded survivor points
     * @param spawner places the prints
     * @param clock   supplies the current time in milliseconds
     * @param random  turns the scent line
     */
    TrackingScan(FootprintConfig config, SurvivorTrackLog log, FootprintSpawner spawner, LongSupplier clock,
                 RandomGenerator random) {
        this.config = config;
        this.log = log;
        this.spawner = spawner;
        this.clock = clock;
        this.random = random;
    }

    /**
     * Uses the tracker.
     *
     * @param slender the slender using it
     * @return {@code true} if the scan ran, {@code false} if the cooldown still runs
     */
    boolean use(Player slender) {
        long now = this.clock.getAsLong();
        if (now < this.readyAt) {
            slender.playSound(DENIED, Sound.Emitter.self());
            return false;
        }
        Instance instance = slender.getInstance();
        if (instance == null) return false;

        long newest = now - this.config.scanGapSeconds() * 1000L;
        List<TrackPoint> points = this.log.pointsNear(slender.getPosition(), this.config.scanRadius(), newest, now);
        if (this.show(instance, ScanSelection.select(points, this.config.scanMaxPrints(), this.config.minSpacing()))) {
            this.readyAt = now + this.config.scanCooldownSeconds() * 1000L;
        } else {
            List<Pos> scent = this.log.nearestPoint(slender.getPosition(), newest, now)
                    .map(nearest -> ScentTrail.spots(slender.getPosition(), nearest.position(),
                            this.config.scentPrints(), this.config.scentSpreadDegrees(), this.random))
                    .orElse(List.of());
            if (!this.show(instance, scent)) {
                slender.sendMessage(NOTHING_FOUND);
            }
            this.readyAt = now + this.config.scentCooldownSeconds() * 1000L;
        }
        this.sendCooldown(slender, now);
        return true;
    }

    /**
     * Locks the tracker for the start delay of a new round.
     */
    void start() {
        this.readyAt = this.clock.getAsLong() + this.config.scanStartDelaySeconds() * 1000L;
    }

    /**
     * Shows a slender what is left of the cooldown, for example after a takeover.
     *
     * @param slender the slender
     */
    void showCooldown(Player slender) {
        this.sendCooldown(slender, this.clock.getAsLong());
    }

    /**
     * Makes the tracker ready again, for a new round.
     */
    void reset() {
        this.readyAt = 0L;
    }

    private boolean show(Instance instance, List<Pos> spots) {
        boolean shown = false;
        for (Pos spot : spots) {
            shown |= this.spawner.spawn(instance, spot, FootprintKind.SURVIVOR, this.config.scanLifetimeSeconds(),
                    TeamHelper::isSlenderTeam);
        }
        return shown;
    }

    private void sendCooldown(Player slender, long now) {
        long left = this.readyAt - now;
        if (left <= 0L) return;
        slender.sendPacket(new SetCooldownPacket(COOLDOWN_GROUP, (int) Math.ceil(left / TICK_MILLIS)));
    }
}
