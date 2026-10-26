package net.onelitefeather.cygnus.stamina;

import net.minestom.server.event.EventDispatcher;
import net.minestom.server.timer.ExecutionType;
import net.onelitefeather.cygnus.common.config.StaminaConfig;
import net.onelitefeather.cygnus.movement.PlayerStopSprintingEvent;
import net.onelitefeather.cygnus.player.CygnusPlayer;

import java.time.temporal.ChronoUnit;

/**
 * Represents the stamina bar implementation based on the player's food level and experience bar.
 * <p>
 * Manages the consumption and regeneration of stamina when a survivor sprints.
 * When stamina is depleted, sprinting is blocked until the bar is back at the
 * {@linkplain StaminaConfig#sprintResumeShare() resume share}; the same share decides when a survivor
 * who stopped early may sprint again.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.4.0
 * @since 1.0.0
 */
public non-sealed class FoodBar extends StaminaBar {

    private static final int MAX_FOOD = 20;
    private static final int FOOD_TAKE = 2;
    private final StaminaConfig config;
    private float currentSpeedCount;
    private boolean displayHeld;

    /**
     * Creates a new instance of the {@link FoodBar} for the specific player
     *
     * @param player who owns the bar
     * @param config the sprint settings
     */
    FoodBar(CygnusPlayer player, StaminaConfig config) {
        super(player, ChronoUnit.MILLIS, 1000, ExecutionType.TICK_START);
        this.config = config;
        state = State.READY;
        this.currentSpeedCount = MAX_FOOD;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStart() {
        this.state = State.READY;
        this.display();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void consume() {
        if (state == State.READY) return;

        if (state == State.DRAINING) {
            this.handleFoodDraining();
            return;
        }

        this.handleFoodRegeneration();
    }

    /**
     * Handles the food draining for the player.
     */
    private void handleFoodDraining() {
        this.currentSpeedCount = this.currentSpeedCount - FOOD_TAKE;
        this.display();

        if (this.currentSpeedCount <= 0.0D) {
            player.setSprinting(false);
            player.setBlockedSprinting(true);
            EventDispatcher.call(new PlayerStopSprintingEvent(player));
            state = State.REGENERATING;
        }
    }

    /**
     * Handles the food regeneration for the player.
     */
    private void handleFoodRegeneration() {
        this.currentSpeedCount = (float) Math.min(MAX_FOOD, this.currentSpeedCount + this.config.regenPerSecond());
        this.display();

        // A survivor who ran dry gets the sprint back long before the bar is full again
        if (this.canResume()) {
            player.setBlockedSprinting(false);
        }
        if (this.currentSpeedCount >= MAX_FOOD) {
            state = State.READY;
        }
    }

    /**
     * Returns whether the bar is full enough to start a new sprint while it regenerates.
     *
     * @return {@code true} once the bar reaches the resume share
     */
    private boolean canResume() {
        return this.currentSpeedCount >= this.config.sprintResumeShare() * MAX_FOOD;
    }

    /**
     * Mirrors the stamina into the experience bar, unless the display is held.
     */
    private void display() {
        if (this.displayHeld) return;
        player.setExp(normalize(this.currentSpeedCount));
    }

    /**
     * Stops mirroring the stamina into the experience bar. The stamina itself goes on as before.
     * <p>
     * The client shows the experience bar for a few seconds after every change of it, on top of the
     * locator bar. Holding the display keeps the locator bar visible while the bar regenerates.
     * </p>
     */
    public void holdDisplay() {
        this.displayHeld = true;
    }

    /**
     * Mirrors the stamina into the experience bar again and brings it up to date. A bar that no
     * longer runs leaves the experience bar alone, it may belong to something else by now.
     */
    public void releaseDisplay() {
        if (!this.displayHeld) return;
        this.displayHeld = false;
        if (this.isRunning()) this.display();
    }

    /**
     * Normalizes the current food value to a percentage between {@code 0.0f} and {@code 1.0f}
     * for display on the player's experience bar.
     *
     * @param current the current food value to normalize
     * @return the normalized value clamped to a minimum of {@code 0.0f}
     */
    private float normalize(float current) {
        return Math.max(0.0f, current / MAX_FOOD);
    }

    /**
     * Returns the remaining stamina as a share of a full bar.
     * <p>
     * This is what drives the survivor's tunnel vision. The bar mirrors the same value into the
     * experience bar, but reading it back from there would tie game logic to a display detail.
     * </p>
     *
     * @return the remaining stamina between {@code 0.0f} and {@code 1.0f}
     */
    public float remainingShare() {
        return normalize(this.currentSpeedCount);
    }

    /**
     * Returns an indication state if the bar could be consumed.
     *
     * @return true for yes otherwise false
     */
    public boolean canConsume() {
        return (state == State.READY) || (state == State.DRAINING) || (state == State.REGENERATING && this.canResume());
    }

    /**
     * Set's the internal {@link State} to draining which starts the tick loop
     */
    public void startConsume() {
        state = State.DRAINING;
    }

    /**
     * Switches the status from {@link State#REGENERATING} to {@link State#DRAINING}.
     * That means the player can't consume food anymore during the regeneration.
     */
    public void switchToRegenerating() {
        if (state == State.REGENERATING) return;
        state = State.REGENERATING;
    }

    /**
     * Empties the bar instantly and lets it regenerate.
     * <p>
     * Used when the creek catches a survivor. Sprinting is blocked just like after running the
     * bar empty, so the survivor cannot escape the slender for a while.
     * </p>
     */
    public void drain() {
        this.currentSpeedCount = 0;
        this.display();
        if (player.isSprinting()) {
            player.setSprinting(false);
            EventDispatcher.call(new PlayerStopSprintingEvent(player));
        }
        player.setBlockedSprinting(true);
        state = State.REGENERATING;
    }

    /**
     * Lifts the bar to the given share and allows sprinting again.
     * <p>
     * Used when an adrenaline rush kicks in, so a survivor caught with an empty bar can still
     * sprint away. The bar never ends up below the {@linkplain StaminaConfig#sprintResumeShare() resume share},
     * so the refill always allows a sprint. A bar that is already fuller stays as it is. The state
     * stays too: a running sprint goes on, an empty bar keeps regenerating from the share on.
     * </p>
     *
     * @param share how full the bar gets, between 0 and 1
     */
    public void refillTo(double share) {
        float target = (float) (Math.max(share, this.config.sprintResumeShare()) * MAX_FOOD);
        if (this.currentSpeedCount >= target) return;
        this.currentSpeedCount = target;
        this.display();
        player.setBlockedSprinting(false);
    }
}
