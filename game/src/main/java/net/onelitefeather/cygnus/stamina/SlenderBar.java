package net.onelitefeather.cygnus.stamina;

import net.kyori.adventure.sound.Sound;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.instance.Instance;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.ExecutionType;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.event.StaminaStateChangeEvent;
import net.onelitefeather.cygnus.player.CygnusPlayer;

import java.time.temporal.ChronoUnit;

/**
 * Manages the stamina/stealth ability of the slender player.
 * <p>
 * The bar cycles through three {@link State}s:
 * <ul>
 *     <li>{@link State#READY} - idle, hidden, ability can be activated</li>
 *     <li>{@link State#DRAINING} - the ability is active: the slender is visible and vulnerable
 *     (blinded, slowed, can't sprint) and damages nearby survivors, while {@link #currentTime} counts down</li>
 *     <li>{@link State#REGENERATING} - the slender is hidden again and recovers (night vision, normal
 *     speed) while {@link #currentTime} counts back up, reached either by manually cancelling DRAINING or
 *     automatically once it runs out</li>
 * </ul>
 * Regeneration must reach {@value #MIN_TIME_TO_REACTIVATE} before {@link #changeStatus()} allows draining again.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 **/
@SuppressWarnings("java:S3252")
public final class SlenderBar extends StaminaBar implements SlenderBarHelper {

    private static final Sound LEVEL = Sound.sound(SoundEvent.ENTITY_PLAYER_LEVELUP, Sound.Source.MASTER, 1F, 1F);

    /**
     * Upper bound of {@link #currentTime}, in stamina units and also the number of segments rendered in the
     * action-bar progress display.
     */
    private static final int MAX_TIME = 16;

    /**
     * Amount {@link #currentTime} changes per tick (the bar ticks every 500ms, see the constructor).
     */
    private static final float TIME_STEP = 0.5f;

    /**
     * Minimum {@link #currentTime} regeneration must reach before {@link #changeStatus()} allows draining
     * again, preventing the ability from being immediately re-triggered after it ran dry.
     */
    private static final int MIN_TIME_TO_REACTIVATE = 10;

    /**
     * Movement speed while {@link State#DRAINING} - deliberately slow since the slender is visible then.
     */
    private static final double DRAINING_MOVEMENT_SPEED = 0.0669;

    private static final float HIDDEN_MOVEMENT_SPEED = 0.1f;
    private static final int DAMAGE_RANGE = 3;

    private final String tileChar;
    private final int time;
    private double currentTime;
    private StaminaColors colorState;

    /**
     * Runs its periodic {@link #consume()} at {@link ExecutionType#TICK_END} rather than the default
     * {@code TICK_START}: incoming player packets (e.g. a manual state change via
     * {@link net.minestom.server.event.player.PlayerUseItemEvent}) are handled between those two phases,
     * so this guarantees {@link #consume()} always sees a state already updated by a same-tick manual
     * transition instead of racing it with stale data.
     */
    SlenderBar(CygnusPlayer player) {
        super(player, ChronoUnit.MILLIS, 500, ExecutionType.TICK_END);
        this.tileChar = "▋";
        this.time = MAX_TIME;
        this.currentTime = time;
        this.colorState = StaminaColors.DRAINING;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStart() {
        this.state = State.READY;
        this.player.addEffect(NIGHT_VISION.potion());
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
        if (currentTime >= 0) {
            currentTime -= TIME_STEP;
            Instance instance = player.getInstance();
            applyDamage(instance, player.getUuid(), player.getPosition(), DAMAGE_RANGE, TIME_STEP);
            this.colorState.sendProgressBar(player, tileChar, (int) currentTime, time);
            return;
        }
        enterRegenerating();
    }

    private void handleRegeneration() {
        if (currentTime < time) {
            currentTime = Math.min(time, currentTime + TIME_STEP);
            this.colorState.sendProgressBar(player, tileChar, (int) currentTime, time);
            return;
        }
        enterReady();
    }

    /**
     * Toggles the ability for the current {@link State}: activates draining from {@link State#READY} or
     * {@link State#REGENERATING}, or cancels an active drain back into {@link State#REGENERATING}.
     *
     * @return {@code false} if regeneration hasn't reached {@link #MIN_TIME_TO_REACTIVATE} yet and the
     * status could not be changed, {@code true} otherwise
     */
    public boolean changeStatus() {
        if (state == State.REGENERATING && this.currentTime < MIN_TIME_TO_REACTIVATE) return false;
        switch (state) {
            case READY -> enterDraining(false);
            case REGENERATING -> enterDraining(true);
            case DRAINING -> enterRegenerating();
        }
        return true;
    }

    /**
     * @param fromRegenerating whether this transition interrupts an ongoing regeneration, which plays a spawn sound
     */
    private void enterDraining(boolean fromRegenerating) {
        state = State.DRAINING;
        colorState = StaminaColors.DRAINING;
        player.setTag(Tags.HIDDEN, VISIBLE);
        if (fromRegenerating) {
            this.playSpawnSound(player.getInstance(), player.getPosition(), player.getUuid());
        }
        this.applyBlindness(player);
        player.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(DRAINING_MOVEMENT_SPEED);
        player.sendSpringPackets();
        player.setSprinting(false);
        player.setBlockedSprinting(true);
        EventDispatcher.call(new StaminaStateChangeEvent(player, state));
        this.colorState.sendProgressBar(player, tileChar, (int) currentTime, time);
    }

    private void enterRegenerating() {
        state = State.REGENERATING;
        colorState = StaminaColors.REGENERATING;
        player.setTag(Tags.HIDDEN, HIDDEN);
        this.playTeleportSound(player.getInstance(), player.getPosition(), player.getUuid());
        this.applyNightVision(player);
        player.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(HIDDEN_MOVEMENT_SPEED);
        player.sendSpringPackets();
        player.setBlockedSprinting(false);
        EventDispatcher.call(new StaminaStateChangeEvent(player, state));
        this.colorState.sendProgressBar(player, tileChar, (int) currentTime, time);
    }

    private void enterReady() {
        state = State.READY;
        colorState = StaminaColors.DRAINING;
        player.playSound(LEVEL, player.getPosition());
    }
}
