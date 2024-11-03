package net.onelitefeather.spectator.listener;

import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.item.ItemStack;
import net.onelitefeather.spectator.item.SpectatorItem;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * The {@link SpectatorItemListener} implements the {@link Consumer} interface and listens for {@link PlayerUseItemEvent}s.
 * It checks if the player is a spectator and if the item is a spectator item.
 * If both conditions are met, the item logic is executed.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
public final class SpectatorItemListener implements Consumer<PlayerUseItemEvent> {

    private final Predicate<Player> spectatorCheck;
    private final Function<Integer, @NotNull Consumer<Player>> itemLogicMapper;

    /**
     * Constructs a new {@link SpectatorItemListener} with the given parameters.
     *
     * @param spectatorCheck  the predicate to check if the player is a spectator
     * @param itemLogicMapper the function to map the item index to the logic
     */
    public SpectatorItemListener(@NotNull Predicate<Player> spectatorCheck, @NotNull Function<Integer, Consumer<Player>> itemLogicMapper) {
        this.spectatorCheck = spectatorCheck;
        this.itemLogicMapper = itemLogicMapper;
    }

    /**
     * Accepts the given {@link PlayerUseItemEvent} and checks if the player is a spectator and the item is a spectator item.
     * If both conditions are met the item logic is executed.
     *
     * @param event the event to handle
     */
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
