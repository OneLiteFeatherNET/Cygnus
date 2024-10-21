package net.onelitefeather.cygnus.listener;

import de.icevizion.aves.util.functional.PlayerConsumer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.item.ItemStack;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.stamina.SlenderBar;
import net.onelitefeather.cygnus.stamina.StaminaService;
import net.onelitefeather.cygnus.utils.TeamHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public final class PlayerItemListener implements Consumer<PlayerUseItemEvent> {

    private final StaminaService staminaService;
    private final PlayerConsumer updateRuleFunction;

    public PlayerItemListener(@Nullable StaminaService staminaService, @NotNull PlayerConsumer updateRuleFunction) {
        this.staminaService = staminaService;
        this.updateRuleFunction = updateRuleFunction;
    }

    @Override
    public void accept(@NotNull PlayerUseItemEvent event) {
        ItemStack stack = event.getItemStack();
        if (!stack.hasTag(Tags.ITEM_TAG)) return;

        Player player = event.getPlayer();
        byte tagValue = stack.getTag(Tags.ITEM_TAG);
        SlenderBar staminaBar = (SlenderBar) staminaService.getSlenderBar();

        if (tagValue != 0 || staminaBar == null) return;

        if (TeamHelper.isSlenderTeam(player) && staminaBar.changeStatus()) {
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
