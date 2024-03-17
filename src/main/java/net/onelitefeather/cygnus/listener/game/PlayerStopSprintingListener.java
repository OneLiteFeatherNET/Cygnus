package net.onelitefeather.cygnus.listener.game;

import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerStopSprintingEvent;
import net.onelitefeather.cygnus.stamina.FoodBar;
import net.onelitefeather.cygnus.utils.Helper;
import net.onelitefeather.cygnus.utils.Tags;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Function;

public class PlayerStopSprintingListener implements Consumer<PlayerStopSprintingEvent> {

    private final Function<@NotNull Player, @NotNull FoodBar> staminaFunction;

    public PlayerStopSprintingListener(@NotNull Function<@NotNull Player, @NotNull FoodBar> staminaFunction) {
        this.staminaFunction = staminaFunction;
    }

    @Override
    public void accept(@NotNull PlayerStopSprintingEvent event) {
        var player = event.getPlayer();
        if (!player.hasTag(Tags.TEAM_ID)) return;

        if (player.getTag(Tags.TEAM_ID) != Helper.SURVIVOR_ID) return;

        FoodBar staminaBarRef = staminaFunction.apply(player);
        staminaBarRef.switchToRegenerating();
    }
}
