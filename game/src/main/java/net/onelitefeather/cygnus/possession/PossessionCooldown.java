package net.onelitefeather.cygnus.possession;

import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.SetCooldownPacket;
import net.onelitefeather.cygnus.utils.Items;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * The wait between two possessions, kept per player.
 * <p>
 * It always runs in full once a possession started, also when the slender ended it early. A use
 * that never got into the creek costs nothing.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.16.0
 */
final class PossessionCooldown {

    /** The client files the cooldown under the item's key, it has no cooldown component. */
    static final String COOLDOWN_GROUP = Items.POSSESSION_MATERIAL.key().asString();

    private static final double TICK_MILLIS = 50.0D;

    private final long millis;
    private final LongSupplier clock;
    private final Map<UUID, Long> readyAt = new ConcurrentHashMap<>();

    /**
     * Creates the cooldown.
     *
     * @param seconds how long it lasts
     * @param clock   supplies the current time in milliseconds
     */
    PossessionCooldown(int seconds, LongSupplier clock) {
        this.millis = seconds * 1000L;
        this.clock = clock;
    }

    /**
     * Tells whether the player may possess the creek again.
     *
     * @param player the player
     * @return {@code true} once the cooldown is over or never ran
     */
    boolean isReady(Player player) {
        return this.clock.getAsLong() >= this.readyAt.getOrDefault(player.getUuid(), 0L);
    }

    /**
     * Starts the full cooldown and shows it on the item.
     *
     * @param player the player who just possessed the creek
     */
    void start(Player player) {
        this.readyAt.put(player.getUuid(), this.clock.getAsLong() + this.millis);
        if (this.millis <= 0L || !player.isOnline()) return;
        player.sendPacket(new SetCooldownPacket(COOLDOWN_GROUP, (int) Math.ceil(this.millis / TICK_MILLIS)));
    }

    /**
     * Forgets every cooldown, for a new round.
     */
    void reset() {
        this.readyAt.clear();
    }
}
