package net.onelitefeather.cygnus.stamina;

import net.minestom.server.event.EventDispatcher;
import net.minestom.server.timer.ExecutionType;
import net.onelitefeather.cygnus.movement.PlayerStopSprintingEvent;
import net.onelitefeather.cygnus.player.CygnusPlayer;

import java.time.temporal.ChronoUnit;

/**
 * Represents the stamina bar implementation based on the player's food level and experience bar.
 * <p>
 * Manages the consumption and regeneration of stamina when a survivor sprints.
 * When stamina is depleted, sprinting is temporarily blocked until sufficient stamina
 * has regenerated.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.2.0
 * @since 1.0.0
 */
public non-sealed class FoodBar extends StaminaBar {

    private static final int MAX_FOOD = 20;
    private static final int FOOD_TAKE = 2;
    private float currentSpeedCount;

    /**
     * Creates a new instance of the {@link FoodBar} for the specific player
     *
     * @param player who owns the bar
     */
    FoodBar(CygnusPlayer player) {
        super(player, ChronoUnit.MILLIS, 1000, ExecutionType.TICK_START);
        state = State.READY;
        this.currentSpeedCount = MAX_FOOD;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStart() {
        this.state = State.READY;
        this.player.setExp(normalize(this.currentSpeedCount));
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
        player.setExp(normalize(this.currentSpeedCount));

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
        this.currentSpeedCount = Math.min(MAX_FOOD, this.currentSpeedCount + 1);
        player.setExp(normalize(this.currentSpeedCount));

        if (this.currentSpeedCount >= MAX_FOOD) {
            state = State.READY;
            player.setBlockedSprinting(false);
        }
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
     * Returns an indication state if the bar could be consumed.
     *
     * @return true for yes otherwise false
     */
    public boolean canConsume() {
        return (state == State.READY) || (state == State.DRAINING) || (state == State.REGENERATING && currentSpeedCount > 7D);
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
}
