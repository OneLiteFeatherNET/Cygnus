package net.onelitefeather.spectator;

import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.onelitefeather.spectator.event.SpectatorAddEvent;
import net.onelitefeather.spectator.event.SpectatorRemoveEvent;
import net.onelitefeather.spectator.item.SpectatorItem;
import net.onelitefeather.spectator.listener.SpectatorChatListener;
import net.onelitefeather.spectator.listener.SpectatorItemListener;
import net.onelitefeather.spectator.listener.SpectatorRemoveListener;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class DefaultSpectatorService implements SpectatorService {

    private final Set<Player> spectators;
    private final Map<Integer, SpectatorItem> hotBarItems;
    private Component spectatorPrefix;
    private Component spectatorSeparator;

    /**
     * Creates a new instance of the {@link SpectatorService}.
     *
     * @param spectatorPrefix    the prefix for the spectator message
     * @param spectatorSeparator the separator for the spectator message
     * @param detectAutoQuit     whether to detect when a spectator quits
     * @param autoSpectatorChat  whether to allow spectators to chat
     * @return a new instance of the {@link SpectatorService}
     */
    @Contract(pure = true, value = "_, _, _, _ -> new")
    static @NotNull SpectatorService of(@NotNull Component spectatorPrefix, @NotNull Component spectatorSeparator, boolean detectAutoQuit, boolean autoSpectatorChat) {
        return new DefaultSpectatorService(spectatorPrefix, spectatorSeparator, detectAutoQuit, autoSpectatorChat, null);
    }

    /**
     * Creates a new instance of the {@link SpectatorService}.
     *
     * @param spectatorPrefix    the prefix for the spectator message
     * @param spectatorSeparator the separator for the spectator message
     * @param detectAutoQuit     whether to detect when a spectator quits
     * @param autoSpectatorChat  whether to allow spectators to chat
     * @param hotBarItems        the hotbar items for the spectator
     * @return a new instance of the {@link SpectatorService}
     */
    @Contract(pure = true, value = "_, _, _, _, _ -> new")
    static @NotNull SpectatorService of(
            @NotNull Component spectatorPrefix,
            @NotNull Component spectatorSeparator,
            boolean detectAutoQuit,
            boolean autoSpectatorChat,
            @NotNull Map<Integer, SpectatorItem> hotBarItems
    ) {
        return new DefaultSpectatorService(spectatorPrefix, spectatorSeparator, detectAutoQuit, autoSpectatorChat, hotBarItems);
    }

    /**
     * Creates a new instance of the {@link SpectatorService}.
     *
     * @param spectatorPrefix    the prefix for the spectator message
     * @param spectatorSeparator the separator for the spectator message
     * @param detectAutoQuit     whether to detect when a spectator quits
     * @param autoSpectatorChat  whether to allow spectators to chat
     * @param hotBarItems        the hotbar items for the spectator
     */
    DefaultSpectatorService(@Nullable Component spectatorPrefix, @NotNull Component spectatorSeparator, boolean detectAutoQuit, boolean autoSpectatorChat, @Nullable Map<Integer, SpectatorItem> hotBarItems) {
        this.spectatorPrefix = spectatorPrefix;
        this.spectatorSeparator = spectatorSeparator;
        this.hotBarItems = hotBarItems;
        this.spectators = new HashSet<>();

        EventNode<Event> eventNode = MinecraftServer.getGlobalEventHandler();

        if (detectAutoQuit) {
            eventNode.addListener(PlayerDisconnectEvent.class, new SpectatorRemoveListener(this::isSpectator, this::remove));
        }

        if (autoSpectatorChat) {
            eventNode.addListener(PlayerChatEvent.class, new SpectatorChatListener(this.spectatorPrefix, this.spectatorSeparator, this::isSpectator, this::getSpectators));
        }

        if (this.hotBarItems != null && !this.hotBarItems.isEmpty()) {
            SpectatorItemListener spectatorItemListener = new SpectatorItemListener(this::isSpectator, itemId -> this.hotBarItems.get(itemId).logic());
            eventNode.addListener(PlayerUseItemEvent.class, spectatorItemListener);
        }
    }

    @Override
    public void clear() {
        if (this.spectators.isEmpty()) return;
        this.spectators.removeIf(player -> {
            this.clearPlayer(player);
            EventDispatcher.call(new SpectatorRemoveEvent(player));
            return true;
        });
        this.spectators.clear();
    }

    @Override
    public boolean isSpectator(@NotNull Player player) {
        return spectators.contains(player) && player.getTag(SPECTATOR_TAG) == (byte) 1;
    }

    @Override
    public boolean hasSpectators() {
        return !this.spectators.isEmpty();
    }

    @Override
    public boolean add(@NotNull Player player) {
        if (this.isSpectator(player)) return false;
        SpectatorAddEvent spectatorAddEvent = new SpectatorAddEvent(player);

        EventDispatcher.callCancellable(spectatorAddEvent, () -> {
            this.spectators.add(player);
            this.setItemsToPlayer(player);
            player.setTag(SPECTATOR_TAG, (byte) 1);
            EventDispatcher.call(new SpectatorAddEvent(player));
        });
        return true;
    }

    @Override
    public boolean remove(@NotNull Player player) {
        if (!this.spectators.remove(player)) return false;
        this.clearPlayer(player);
        EventDispatcher.call(new SpectatorRemoveEvent(player));
        return true;
    }

    /**
     * Removes some of the player's data when they are no longer a spectator.
     *
     * @param player the player to remove data from
     */
    private void clearPlayer(@NotNull Player player) {
        player.removeTag(SPECTATOR_TAG);
        player.getInventory().clear();
        if (player.getOpenInventory() != null) player.closeInventory();
    }

    /**
     * Set each {@link SpectatorItem} which is registered to the player.
     *
     * @param player the player to set the items to
     */
    private void setItemsToPlayer(@NotNull Player player) {
        if (this.hotBarItems == null || this.hotBarItems.isEmpty()) return;
        for (SpectatorItem value : this.hotBarItems.values()) {
            player.getInventory().setItemStack(value.slotIndex(), value.item());
        }
    }

    @Override
    public @NotNull @UnmodifiableView Set<@NotNull Player> getSpectators() {
        return Collections.unmodifiableSet(this.spectators);
    }
}
