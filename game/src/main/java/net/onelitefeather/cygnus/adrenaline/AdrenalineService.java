package net.onelitefeather.cygnus.adrenaline;

import net.kyori.adventure.sound.Sound;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;
import net.onelitefeather.cygnus.GameFeature;
import net.onelitefeather.cygnus.attribute.AttributeHelper;
import net.onelitefeather.cygnus.common.config.AdrenalineConfig;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.event.GameStartEvent;
import net.onelitefeather.cygnus.event.StaminaStateChangeEvent;
import net.onelitefeather.cygnus.stamina.StaminaBar;
import net.onelitefeather.cygnus.utils.RepeatingTask;
import org.jetbrains.annotations.Nullable;

import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Gives a survivor an adrenaline rush when the visible slender comes close.
 * <p>
 * For a few seconds the survivor moves faster, which is enough to get away even with an empty
 * sprint bar. Once a rush is over, the same survivor has to wait out a cooldown before the next
 * one. When a rush ends and when the next one may start is kept on the player as transient tags.
 * </p>
 * <p>
 * The slender counts as visible while his bar drains, which the service learns from the
 * {@link StaminaStateChangeEvent} instead of reaching into the bar.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.16.0
 */
public final class AdrenalineService implements GameFeature {

    /** How often the survivors around the slender are looked at, in milliseconds. */
    static final long TICK_MILLIS = 250L;

    /** When the running rush of a survivor ends, in milliseconds. */
    static final Tag<Long> RUSH_UNTIL = Tag.Transient("adrenalineUntil");

    /** When a survivor may get the next rush, in milliseconds. */
    static final Tag<Long> COOLDOWN_UNTIL = Tag.Transient("adrenalineCooldownUntil");

    /** What the survivor hears when the rush kicks in, so they know why they are faster. */
    private static final Sound HEARTBEAT = Sound.sound(SoundEvent.ENTITY_WARDEN_HEARTBEAT, Sound.Source.MASTER, 1F, 1F);

    private final EventNode<Event> node = EventNode.all("adrenaline");

    private final AdrenalineConfig config;
    private final Supplier<Set<Player>> survivors;
    private final LongSupplier clock;
    private final RepeatingTask task = new RepeatingTask(this::tick);
    private final Set<Player> rushing = new HashSet<>();
    private @Nullable Player visibleSlender;

    /**
     * Creates the service.
     *
     * @param config    the radius, the bonus, the duration and the cooldown
     * @param survivors supplies the survivors of the round
     * @param clock     supplies the current time in milliseconds
     */
    public AdrenalineService(AdrenalineConfig config, Supplier<Set<Player>> survivors, LongSupplier clock) {
        this.config = config;
        this.survivors = survivors;
        this.clock = clock;
        this.registerListeners();
    }

    @Override
    public EventNode<Event> node() {
        return this.node;
    }

    /**
     * Hooks the service into the round's lifecycle and follows the slender's visibility.
     */
    private void registerListeners() {
        this.node.addListener(GameStartEvent.class, _ -> this.task.start(TICK_MILLIS, ChronoUnit.MILLIS));
        this.node.addListener(StaminaStateChangeEvent.class, this::slenderChanged);
        this.node.addListener(PlayerDeathEvent.class, event -> this.end(event.getPlayer()));
        this.node.addListener(PlayerDisconnectEvent.class, event -> this.left(event.getPlayer()));
        this.node.addListener(GameFinishEvent.class, _ -> this.stop());
    }

    /**
     * Remembers whether the slender can be seen: only while his bar drains.
     *
     * @param event the change of the slender's bar
     */
    private void slenderChanged(StaminaStateChangeEvent event) {
        if (event.getState() == StaminaBar.State.DRAINING) {
            this.visibleSlender = event.getPlayer();
        } else if (event.getPlayer() == this.visibleSlender) {
            this.visibleSlender = null;
        }
    }

    /**
     * Ends the rushes that ran out and starts one for every survivor close to the visible slender.
     */
    void tick() {
        long now = this.clock.getAsLong();
        for (Player player : List.copyOf(this.rushing)) {
            Long until = player.getTag(RUSH_UNTIL);
            if (until == null || now >= until) {
                this.end(player);
            }
        }

        Player slender = this.visibleSlender;
        if (slender == null) return;
        Instance world = slender.getInstance();
        if (world == null) return;
        Pos where = slender.getPosition();
        double radius = this.config.radius();
        for (Player survivor : this.survivors.get()) {
            if (survivor.getInstance() != world) continue;
            if (survivor.getPosition().distance(where) > radius) continue;
            Long cooldown = survivor.getTag(COOLDOWN_UNTIL);
            if (cooldown != null && now < cooldown) continue;
            this.rush(survivor, now);
        }
    }

    /**
     * Starts a rush for a survivor.
     *
     * @param survivor the survivor
     * @param now      the current time in milliseconds
     */
    private void rush(Player survivor, long now) {
        long ends = now + this.config.durationSeconds() * 1000L;
        survivor.setTag(RUSH_UNTIL, ends);
        // The cooldown starts once the rush is over, not when it begins.
        survivor.setTag(COOLDOWN_UNTIL, ends + this.config.cooldownSeconds() * 1000L);
        AttributeHelper.applyAdrenaline(survivor, this.config.speedBonus());
        survivor.playSound(HEARTBEAT, Sound.Emitter.self());
        this.rushing.add(survivor);
    }

    /**
     * Ends the rush of a player, if they have one. The cooldown stays.
     *
     * @param player the player
     */
    private void end(Player player) {
        if (!this.rushing.remove(player)) return;
        AttributeHelper.removeAdrenaline(player);
        player.removeTag(RUSH_UNTIL);
    }

    /**
     * Forgets a player who left, and the slender if it was him.
     *
     * @param player the player who left
     */
    private void left(Player player) {
        this.end(player);
        if (player == this.visibleSlender) {
            this.visibleSlender = null;
        }
    }

    /**
     * Stops looking at the survivors and takes every rush and cooldown off them.
     */
    private void stop() {
        this.task.stop();
        this.visibleSlender = null;
        for (Player player : List.copyOf(this.rushing)) {
            this.end(player);
        }
        for (Player survivor : this.survivors.get()) {
            survivor.removeTag(COOLDOWN_UNTIL);
        }
    }
}
