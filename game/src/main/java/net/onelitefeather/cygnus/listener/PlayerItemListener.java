package net.onelitefeather.cygnus.listener;

import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.onelitefeather.cygnus.stamina.SlenderBar;
import net.onelitefeather.cygnus.stamina.StaminaService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public final class PlayerItemListener implements Consumer<PlayerUseItemEvent> {

    private final StaminaService staminaService;
    private final Consumer<Player> updateRuleFunction;

    public PlayerItemListener(@Nullable StaminaService staminaService, @NotNull Consumer<Player> updateRuleFunction) {
        this.staminaService = staminaService;
        this.updateRuleFunction = updateRuleFunction;
    }

    @Override
    public void accept(@NotNull PlayerUseItemEvent event) {
        if (!event.getItemStack().hasTag(Tags.ITEM_TAG)) return;

        var player = event.getPlayer();

        var tagValue = event.getItemStack().getTag(Tags.ITEM_TAG);

        var staminaBar = (SlenderBar) staminaService.getSlenderBar();

        if (tagValue == 0 && player.getTag(Tags.TEAM_ID) == Helper.SLENDER_ID && staminaBar != null && staminaBar.changeStatus()) {
            changeVisibilityStatus(player);
            updateRuleFunction.accept(player);
        }
    }


    private void changeVisibilityStatus(@NotNull Player player) {
        var currentValue = player.getTag(Tags.HIDDEN);
        if (currentValue == 0) {
            player.setTag(Tags.HIDDEN, (byte) 1);
        } else {
            player.setTag(Tags.HIDDEN, (byte) 0);
        }
    }

}
