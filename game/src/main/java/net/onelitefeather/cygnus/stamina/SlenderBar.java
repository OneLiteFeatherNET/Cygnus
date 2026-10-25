package net.onelitefeather.cygnus.stamina;

import net.kyori.adventure.sound.Sound;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.SetCooldownPacket;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.ExecutionType;
import net.onelitefeather.cygnus.attribute.AttributeHelper;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.StaminaConfig;
import net.onelitefeather.cygnus.creek.dread.DreadSource;
import net.onelitefeather.cygnus.event.StaminaStateChangeEvent;
import net.onelitefeather.cygnus.player.CygnusPlayer;

import java.time.temporal.ChronoUnit;
import java.util.function.LongSupplier;

/**
 * Manages the stamina/stealth ability of the slender player.
 * <p>
 * The bar cycles through three {@link State}s:
 * <ul>
 *     <li>{@link State#READY} - idle, hidden, ability can be activated</li>
 *     <li>{@link State#DRAINING} - the ability is active: the slender is visible and vulnerable
 *     (blinded, slowed, can't sprint) and swings at nearby survivors once a second, while {@link #currentTime} counts down</li>
 *     <li>{@link State#REGENERATING} - the slender is hidden again and recovers (night vision, normal
 *     speed) while {@link #currentTime} counts back up, reached either by manually cancelling DRAINING or
 *     automatically once it runs out</li>
 * </ul>
 * Regeneration must reach {@value #MIN_TIME_TO_REACTIVATE} before {@link #changeStatus()} allows draining again,
 * and once hidden the slender stays hidden for at least
 * {@linkplain StaminaConfig#slenderReappearCooldownSeconds() the reappear cooldown}, so he cannot chain one
 * appearance onto the next. While he looks through the creek's eyes, regeneration stands still.
 *
 * @author theEvilReaper
 * @version 1.3.0
 * @since 1.0.0
 **/
@SuppressWarnings("java:S3252")
public final class SlenderBar extends StaminaBar implements SlenderBarHelper {

    private static final Sound LEVEL = Sound.sound(SoundEvent.ENTITY_PLAYER_LEVELUP, Sound.Source.MASTER, 1F, 1F);

    /** What only the slender hears when his swing hurts a survivor, the sound of a regular attack. */
    static final Sound HIT = Sound.sound(SoundEvent.ENTITY_PLAYER_ATTACK_STRONG, Sound.Source.PLAYER, 0.6F, 1F);

    /**
     * How long the slender waits before he hears {@link #HIT} again, in milliseconds. He swings only
     * once a second anyway, so the cooldown just keeps the sound from ever stacking up.
     */
    static final long HIT_COOLDOWN_MILLIS = 1000L;

    /**
     * Upper bound of {@link #currentTime}, in stamina units and also the number of segments rendered in the
     * action-bar progress display.
     */
    static final int MAX_TIME = 16;

    /**
     * Amount {@link #currentTime} changes per tick (the bar ticks every 500ms, see the constructor).
     */
    static final float TIME_STEP = 0.5f;

    /**
     * Minimum {@link #currentTime} regeneration must reach before {@link #changeStatus()} allows draining
     * again, preventing the ability from being immediately re-triggered after it ran dry.
     */
    private static final int MIN_TIME_TO_REACTIVATE = 10;

    /**
     * Bar ticks per swing. The slender swings once a second, every second tick of an appearance, so
     * the first swing falls one second after he appears and a survivor next to him has a moment to react.
     */
    static final int TICKS_PER_SWING = 2;

    /** What only the slender hears when a swing misses, quiet enough not to give anything away. */
    static final Sound MISS = Sound.sound(SoundEvent.ENTITY_PLAYER_ATTACK_NODAMAGE, Sound.Source.PLAYER, 0.3F, 1F);

    /** How long one tick of the bar takes, in milliseconds. */
    private static final long TICK_MILLIS = 500L;

    /** How long one server tick takes, in milliseconds, the unit the item cooldown is sent in. */
    private static final long SERVER_TICK_MILLIS = 50L;

    /**
     * The cooldown group of the eye. The item has no cooldown component of its own, so the client
     * files its cooldown under the item's key.
     */
    static final String COOLDOWN_GROUP = Material.ENDER_EYE.key().asString();

    private final String tileChar;
    private final int time;
    private final long reappearCooldownMillis;
    /** How far around him a draining slender hurts survivors, in blocks. */
    private final int damageRange;
    private final LongSupplier clock;
    private final DreadSource dread;
    private final SlenderSwing swing;
    private double currentTime;
    private StaminaColors colorState;
    /** The earliest moment the slender may appear again, in milliseconds. */
    private long reappearAt;
    /** The earliest moment the slender may hear {@link #HIT} again, in milliseconds. */
    private long hitSoundAt = Long.MIN_VALUE;
    /** Whether regeneration was held back on the last tick because he looked through the creek. */
    private boolean paused;
    /** The bar ticks of the running appearance, counted to swing on every {@value #TICKS_PER_SWING}th. */
    private int drainTicks;

    /**
     * Runs its periodic {@link #consume()} at {@link ExecutionType#TICK_END} rather than the default
     * {@code TICK_START}: incoming player packets (e.g. a manual state change via
     * {@link net.minestom.server.event.player.PlayerUseItemEvent}) are handled between those two phases,
     * so this guarantees {@link #consume()} always sees a state already updated by a same-tick manual
     * transition instead of racing it with stale data.
     *
     * @param player who owns the bar
     * @param config the settings, of which the bar reads the reappear cooldown and the damage range
     * @param clock  supplies the current time in milliseconds
     * @param dread  supplies the fear of each survivor, which softens the swings
     * @param swing  rolls what a swing does to a survivor
     */
    SlenderBar(CygnusPlayer player, StaminaConfig config, LongSupplier clock, DreadSource dread, SlenderSwing swing) {
        super(player, ChronoUnit.MILLIS, 500, ExecutionType.TICK_END);
        this.tileChar = "▋";
        this.time = MAX_TIME;
        this.reappearCooldownMillis = config.slenderReappearCooldownSeconds() * 1000L;
        this.damageRange = config.slenderDamageRange();
        this.clock = clock;
        this.dread = dread;
        this.swing = swing;
        this.currentTime = time;
        this.colorState = StaminaColors.DRAINING;
        this.reappearAt = Long.MIN_VALUE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStart() {
        this.state = State.READY;
        AttributeHelper.resetSpeed(player);
        AttributeHelper.removeSlenderDrainingSpeed(player);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void consume() {
        if (state == State.READY) return;
        if (state == State.DRAINING) {
            this.handleDraining();
            return;
        }
        this.handleRegeneration();
    }

    private void handleDraining() {
        currentTime -= TIME_STEP;
        if (++drainTicks % TICKS_PER_SWING == 0) {
            SwingOutcome outcome = swing(player.getInstance(), player.getUuid(), player.getPosition(), damageRange, swing, dread);
            if (outcome.hit()) {
                this.playHitSound();
            }
            if (outcome.missed()) {
                player.playSound(MISS, Sound.Emitter.self());
            }
        }
        // Hides in the same tick the bar runs dry: one tick later he would stay visible and swing
        // once more with a bar that already shows empty.
        if (currentTime <= 0) {
            currentTime = 0;
            enterRegenerating();
            return;
        }
        this.colorState.sendProgressBar(player, tileChar, currentTime, time);
    }

    /**
     * Lets the slender hear that he hurt someone, at most once per {@value #HIT_COOLDOWN_MILLIS} ms.
     */
    private void playHitSound() {
        long now = this.clock.getAsLong();
        if (now < this.hitSoundAt) return;
        this.hitSoundAt = now + HIT_COOLDOWN_MILLIS;
        player.playSound(HIT, Sound.Emitter.self());
    }

    /**
     * Refills the bar, but not while the slender looks through the creek's eyes: possessing it must
     * not double as a free wait for his next appearance.
     * <p>
     * The eye's cooldown overlay counted on the refill going on, so it is sent again once he is back.
     * </p>
     */
    private void handleRegeneration() {
        if (player.hasTag(Tags.POSSESSING)) {
            this.paused = true;
            return;
        }
        if (this.paused) {
            this.paused = false;
            long reappearIn = Math.max(0L, this.reappearAt - this.clock.getAsLong());
            this.sendCooldown(Math.max(reappearIn, this.millisUntilReactivation()));
        }
        if (currentTime < time) {
            currentTime = Math.min(time, currentTime + TIME_STEP);
            this.colorState.sendProgressBar(player, tileChar, currentTime, time);
            return;
        }
        enterReady();
    }

    /**
     * Returns how long regeneration takes from here to {@value #MIN_TIME_TO_REACTIVATE}.
     *
     * @return the time in milliseconds, {@code 0} once the bar is there
     */
    private long millisUntilReactivation() {
        double missing = Math.max(0.0D, MIN_TIME_TO_REACTIVATE - this.currentTime);
        return (long) Math.ceil(missing / TIME_STEP) * TICK_MILLIS;
    }

    /**
     * Shows the slender on his eye how long he has to wait before he can appear again.
     * <p>
     * Rounded up to whole server ticks: the client must not hand the eye back a tick before the
     * server takes it.
     * </p>
     *
     * @param millis the wait in milliseconds, {@code 0} clears the cooldown
     */
    private void sendCooldown(long millis) {
        int ticks = (int) Math.ceil((double) millis / SERVER_TICK_MILLIS);
        player.sendPacket(new SetCooldownPacket(COOLDOWN_GROUP, ticks));
    }

    /**
     * Stops the bar and takes the cooldown off the eye, so no stale overlay outlives the round.
     * <p>
     * A round can end while he is draining, which never runs {@link #enterRegenerating()}. The
     * blindness has no end of its own, so the drain effects are lifted here or he stays in the dark.
     * </p>
     */
    @Override
    public void stop() {
        super.stop();
        this.sendCooldown(0);
        this.clearBlindness(player);
        AttributeHelper.removeSlenderDrainingSpeed(player);
        player.setBlockedSprinting(false);
    }

    /**
     * Toggles the ability for the current {@link State}: activates draining from {@link State#READY} or
     * {@link State#REGENERATING}, or cancels an active drain back into {@link State#REGENERATING}.
     *
     * @return {@code false} if regeneration hasn't reached {@link #MIN_TIME_TO_REACTIVATE} yet or the
     * reappear cooldown is still running and the status could not be changed, {@code true} otherwise
     */
    public boolean changeStatus() {
        if (state == State.REGENERATING && this.currentTime < MIN_TIME_TO_REACTIVATE) return false;
        // Holds for READY too: a bar that refilled inside the cooldown must not skip it
        if (state != State.DRAINING && this.clock.getAsLong() < this.reappearAt) return false;
        switch (state) {
            case READY, REGENERATING -> enterDraining();
            case DRAINING -> enterRegenerating();
        }
        return true;
    }

    /**
     * Makes the slender appear. Every appearance is heard, the first one of the round too, so a
     * survivor nearby gets a chance to react.
     */
    private void enterDraining() {
        state = State.DRAINING;
        drainTicks = 0;
        colorState = StaminaColors.DRAINING;
        player.setTag(Tags.HIDDEN, VISIBLE);
        this.playSpawnSound(player.getInstance(), player.getPosition(), player.getUuid());
        this.applyBlindness(player);
        AttributeHelper.applySlenderDrainingSpeed(player);
        player.sendSprintPackets();
        player.setSprinting(false);
        player.setBlockedSprinting(true);
        EventDispatcher.call(new StaminaStateChangeEvent(player, state));
        this.colorState.sendProgressBar(player, tileChar, currentTime, time);
    }

    private void enterRegenerating() {
        state = State.REGENERATING;
        this.reappearAt = this.clock.getAsLong() + this.reappearCooldownMillis;
        this.sendCooldown(Math.max(this.reappearCooldownMillis, this.millisUntilReactivation()));
        colorState = StaminaColors.REGENERATING;
        player.setTag(Tags.HIDDEN, HIDDEN);
        this.playTeleportSound(player.getInstance(), player.getPosition(), player.getUuid());
        this.clearBlindness(player);
        AttributeHelper.removeSlenderDrainingSpeed(player);
        player.sendSprintPackets();
        player.setBlockedSprinting(false);
        EventDispatcher.call(new StaminaStateChangeEvent(player, state));
        this.colorState.sendProgressBar(player, tileChar, currentTime, time);
    }


    private void enterReady() {
        state = State.READY;
        colorState = StaminaColors.DRAINING;
        player.playSound(LEVEL, player.getPosition());
    }
}
