package net.onelitefeather.cygnus.listener.game;

import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.movement.PlayerStartSprintingEvent;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import net.onelitefeather.cygnus.stamina.FoodBar;
import net.onelitefeather.cygnus.utils.TeamHelper;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Function;

public final class PlayerStartSprintingListener implements Consumer<PlayerStartSprintingEvent> {

    private final Function<@NotNull Player, @NotNull FoodBar> staminaFunction;

    public PlayerStartSprintingListener(@NotNull Function<@NotNull Player, @NotNull FoodBar> staminaFunction) {
        this.staminaFunction = staminaFunction;
    }

    @Override
    public void accept(@NotNull PlayerStartSprintingEvent event) {
        var player = event.getPlayer();
        if (!player.hasTag(Tags.TEAM_ID)) return;
        if (TeamHelper.isSlenderTeam(player)) return;

        CygnusPlayer cygnusPlayer = (CygnusPlayer) player;

        if (cygnusPlayer.hasBlockedSprinting()) {
            event.setCancelled(true);
        }

        FoodBar staminaBarRef = staminaFunction.apply(player);
        if (!staminaBarRef.canConsume()) {
            event.setCancelled(true);
        }
    }
}
