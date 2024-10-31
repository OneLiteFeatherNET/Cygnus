package net.onelitefeather.cygnus.listener.game;

import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.event.player.PlayerUseItemOnBlockEvent;
import net.minestom.server.event.trait.ItemEvent;
import net.minestom.server.item.ItemStack;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.stamina.SlenderBarTrigger;
import net.onelitefeather.cygnus.utils.TeamHelper;
import org.jetbrains.annotations.NotNull;

public final class SlenderItemListener {

    private final SlenderBarTrigger barTrigger;

    public SlenderItemListener(@NotNull SlenderBarTrigger barTrigger, @NotNull EventNode<Event> eventNode) {
        this.barTrigger = barTrigger;
        eventNode.addListener(PlayerUseItemEvent.class, event -> trigger(event.getPlayer(), event));
    }

    private void trigger(@NotNull Player player, @NotNull ItemEvent event) {
        ItemStack stack = event.getItemStack();
        if (!stack.hasTag(Tags.ITEM_TAG)) return;
        byte tagValue = stack.getTag(Tags.ITEM_TAG);

        if (tagValue != 0) return;
        if (!TeamHelper.isSlenderTeam(player)) return;
        barTrigger.trigger(player);
    }
}
