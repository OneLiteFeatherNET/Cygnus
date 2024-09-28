package net.onelitefeather.cygnus.stamina;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.common.util.Helper;
import net.onelitefeather.cygnus.movement.PlayerStopSprintingEvent;
import org.jetbrains.annotations.NotNull;

import java.time.temporal.ChronoUnit;

public non-sealed class FoodBar extends StaminaBar {

    private static final int MAX_FOOD = 20;
    private static final int FOOD_TAKE = 2;
    private float currentSpeedCount;

    FoodBar(@NotNull Player player) {
        super(player, ChronoUnit.MILLIS, 500);
        status = Status.READY;
        this.currentSpeedCount = MAX_FOOD;
    }

    @Override
    protected void onStart() {
        this.status = Status.READY;
        this.player.setExp(normalize(this.currentSpeedCount));
    }

    @Override
    public void consume() {
        if (status == Status.READY) return;

        if (status == Status.DRAINING) {
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
            player.sendPacket(player.getMetadataPacket());
            MinecraftServer.getGlobalEventHandler().call(new PlayerStopSprintingEvent(player));
            status = Status.REGENERATING;
        }
    }

    /**
     * Handles the food regeneration for the player.
     */
    private void handleFoodRegeneration() {
        if (this.currentSpeedCount == MAX_FOOD) {
            status = Status.READY;
            Helper.changeSpeedValue(player, true);
            return;
        }

        if (this.currentSpeedCount < MAX_FOOD) {
            ++this.currentSpeedCount;
            player.setExp(normalize(this.currentSpeedCount));
        }
    }

    private float normalize(float current) {
        var newValue = (current) / MAX_FOOD;
        if (newValue >= 0) {
            return newValue;
        }
        return 0;
    }

    public boolean canConsume() {
        if (status == Status.READY) {
            status = Status.DRAINING;
            return true;
        }
        if (status == Status.REGENERATING && currentSpeedCount > 7D) {
            status = Status.DRAINING;
            return true;
        }

        System.out.println("Can't consume because the status is " + status + " and the currentSpeed is " + currentSpeedCount);
        return false;
    }

    /**
     * Switches the status from {@link Status#REGENERATING} to {@link Status#DRAINING}.
     * That means the player can't consume food anymore during the regeneration.
     */
    public void switchToRegenerating() {
        if (status == Status.REGENERATING) return;
        status = Status.REGENERATING;
    }
}
