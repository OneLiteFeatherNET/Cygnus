package net.onelitefeather.spectator.listener;

import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.item.ItemStack;
import net.onelitefeather.spectator.item.SpectatorItem;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class SpectatorItemListener implements Consumer<PlayerUseItemEvent> {

    private final Predicate<Player> spectatorCheck;
    private final Function<Integer, @NotNull Consumer<Player>> itemLogicMapper;

    public SpectatorItemListener(@NotNull Predicate<Player> spectatorCheck, @NotNull Function<Integer, Consumer<Player>> itemLogicMapper) {
        this.spectatorCheck = spectatorCheck;
        this.itemLogicMapper = itemLogicMapper;
    }

    @Override
    public void accept(@NotNull PlayerUseItemEvent event) {
        Player player = event.getPlayer();
        if (!this.spectatorCheck.test(player)) return;

        ItemStack stack = event.getItemStack();

        if (!stack.hasTag(SpectatorItem.SPEC_ITEM_TAG)) return;

        int itemIndex = stack.getTag(SpectatorItem.SPEC_ITEM_TAG);
        this.itemLogicMapper.apply(itemIndex).accept(player);
    }
}
