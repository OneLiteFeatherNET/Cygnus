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

/**
 * The slender's tracker: shows him where survivors walked around him, then needs a cooldown.
 * <p>
 * The newest seconds are left out, so the prints never lead straight to a survivor. A scan that
 * finds nothing still costs the cooldown. That is an answer too, and it keeps the tracker from
 * being a free probe. The cooldown belongs to the slender role, so a new slender after a
 * takeover inherits what is left of it.
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
    private long readyAt;

    /**
     * Creates the scan.
     *
     * @param config  the radius, the gap, the maximum, the lifetime and the cooldown
     * @param log     the recorded survivor points
     * @param spawner places the prints
     * @param clock   supplies the current time in milliseconds
     */
    TrackingScan(FootprintConfig config, SurvivorTrackLog log, FootprintSpawner spawner, LongSupplier clock) {
        this.config = config;
        this.log = log;
        this.spawner = spawner;
        this.clock = clock;
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
        int shown = 0;
        for (Pos spot : ScanSelection.select(points, this.config.scanMaxPrints(), this.config.minSpacing())) {
            if (this.spawner.spawn(instance, spot, FootprintKind.SURVIVOR, this.config.scanLifetimeSeconds(),
                    TeamHelper::isSlenderTeam)) {
                shown++;
            }
        }
        if (shown == 0) {
            slender.sendMessage(NOTHING_FOUND);
        }
        this.readyAt = now + this.config.scanCooldownSeconds() * 1000L;
        this.sendCooldown(slender, now);
        return true;
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

    private void sendCooldown(Player slender, long now) {
        long left = this.readyAt - now;
        if (left <= 0L) return;
        slender.sendPacket(new SetCooldownPacket(COOLDOWN_GROUP, (int) Math.ceil(left / TICK_MILLIS)));
    }
}
