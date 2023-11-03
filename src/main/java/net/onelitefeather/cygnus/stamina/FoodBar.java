package net.onelitefeather.cygnus.stamina;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.movement.PlayerStopSprintingEvent;
import net.onelitefeather.cygnus.utils.Helper;
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
        switch (status) {
            case DRAINING -> {
                this.currentSpeedCount = this.currentSpeedCount - FOOD_TAKE;
                player.setExp(normalize(this.currentSpeedCount));

                if (this.currentSpeedCount <= 0.0D) {
                    player.setSprinting(false);
                    MinecraftServer.getGlobalEventHandler().call(new PlayerStopSprintingEvent(player));
                    status = Status.REGENERATING;
                }
            }
            case REGENERATING -> {
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
        }
    }

    private float normalize(float current) {
        return (current) / MAX_FOOD;
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

    public void switchToRegenerating() {
        if (status == Status.REGENERATING) return;
        status = Status.REGENERATING;
    }
}
