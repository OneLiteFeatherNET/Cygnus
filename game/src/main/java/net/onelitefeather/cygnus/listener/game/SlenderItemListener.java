package net.onelitefeather.cygnus.listener.game;

import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.item.ItemStack;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.stamina.SlenderBarTrigger;
import net.onelitefeather.cygnus.team.TeamHelper;

import java.util.function.Consumer;

public final class SlenderItemListener implements Consumer<PlayerUseItemEvent> {

    private final SlenderBarTrigger barTrigger;

    public SlenderItemListener(SlenderBarTrigger barTrigger) {
        this.barTrigger = barTrigger;
    }

    @Override
    public void accept(PlayerUseItemEvent event) {
        Player player = event.getPlayer();
        ItemStack stack = event.getItemStack();
        
        if (!stack.hasTag(Tags.ITEM_TAG)) return;
        Byte tagValue = stack.getTag(Tags.ITEM_TAG);

        if (tagValue == null || tagValue != 0) return;
        if (!TeamHelper.isSlenderTeam(player)) return;
        
        barTrigger.trigger(player);
    }
}
