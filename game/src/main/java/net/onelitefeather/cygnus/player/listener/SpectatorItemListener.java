package net.onelitefeather.cygnus.player.listener;

import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.item.ItemStack;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.spectator.SpectatorService;
import net.onelitefeather.cygnus.utils.Items;

import java.util.function.Consumer;

public final class SpectatorItemListener implements Consumer<PlayerUseItemEvent> {

    private final SpectatorService spectatorService;

    public SpectatorItemListener(SpectatorService spectatorService) {
        this.spectatorService = spectatorService;
    }

    @Override
    public void accept(PlayerUseItemEvent event) {
        Player player = event.getPlayer();
        ItemStack stack = event.getItemStack();

        if (!stack.hasTag(Tags.ITEM_TAG)) return;
        if (!spectatorService.isSpectator(player)) return;

        Byte tagValue = stack.getTag(Tags.ITEM_TAG);
        if (tagValue == null) return;

        if (tagValue == Items.SPECTATE_ITEM) {
            spectatorService.openOverview(player);
        } else if (tagValue == Items.LEAVE_ITEM) {
            spectatorService.leave(player);
        }
    }
}
