package net.onelitefeather.spectator;

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
import net.onelitefeather.spectator.util.ChatData;
import net.onelitefeather.spectator.util.ListenerData;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class DefaultSpectatorService implements SpectatorService {

    private final ListenerData listenerData;
    private final ChatData chatData;
    private final Set<Player> spectators;
    private final Map<Integer, SpectatorItem> hotBarItems;
    private boolean autoRegisterListener;

    /**
     * Creates a new instance of the {@link SpectatorService}.
     *
     * @param listenerData the listener option for the spectator
     * @param chatData     the chat option for the spectator
     * @return a new instance of the {@link SpectatorService}
     */
    @Contract(pure = true, value = " _, _, _ -> new")
    static @NotNull SpectatorService of(
            @NotNull ListenerData listenerData,
            @NotNull ChatData chatData,
            boolean autoRegisterListener
    ) {
        return new DefaultSpectatorService(listenerData, chatData, null, autoRegisterListener);
    }

    /**
     * Creates a new instance of the {@link SpectatorService}.
     *
     * @param hotBarItems  the hotbar items for the spectator
     * @param listenerData the listener option for the spectator
     * @param chatData     the chat option for the spectator
     * @return a new instance of the {@link SpectatorService}
     */
    @Contract(pure = true, value = "_, _, _, _ -> new")
    static @NotNull SpectatorService of(
            @NotNull ListenerData listenerData,
            @NotNull ChatData chatData,
            @NotNull Map<Integer, SpectatorItem> hotBarItems,
            boolean autoRegisterListener
    ) {
        return new DefaultSpectatorService(listenerData, chatData, hotBarItems, autoRegisterListener);
    }

    /**
     * Creates a new instance of the {@link SpectatorService}.
     *
     * @param listenerData the listener option for the spectator
     * @param chatData     the chat option for the spectator
     * @param hotBarItems  the hotbar items for the spectator
     */
    DefaultSpectatorService(
            @NotNull ListenerData listenerData,
            @NotNull ChatData chatData,
            @Nullable Map<Integer, SpectatorItem> hotBarItems,
            boolean autoRegisterListener
    ) {
        this.autoRegisterListener = autoRegisterListener;
        this.hotBarItems = hotBarItems;
        this.spectators = new HashSet<>();
        this.listenerData = listenerData;
        this.chatData = chatData;

        if (autoRegisterListener) {
            this.registerListener();
        }
    }

    @Override
    public void registerListener() {
        if (!autoRegisterListener) return;
        EventNode<Event> eventNode = MinecraftServer.getGlobalEventHandler();

        if (listenerData.detectSpectatorQuit()) {
            eventNode.addListener(PlayerDisconnectEvent.class, new SpectatorRemoveListener(this::isSpectator, this::remove));
        }

        if (listenerData.detectSpectatorChat()) {
            eventNode.addListener(PlayerChatEvent.class, new SpectatorChatListener(chatData, this::isSpectator, this::getSpectators));
        }

        if (this.hotBarItems != null && !this.hotBarItems.isEmpty()) {
            SpectatorItemListener spectatorItemListener = new SpectatorItemListener(this::isSpectator, itemId -> this.hotBarItems.get(itemId).logic());
            eventNode.addListener(PlayerUseItemEvent.class, spectatorItemListener);
        }
        this.autoRegisterListener = false;
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

    /**
     * Returns an unmodifiable view of all spectators.
     *
     * @return the given spectators as an unmodifiable view
     */
    @Override
    public @NotNull @UnmodifiableView Set<@NotNull Player> getSpectators() {
        return Collections.unmodifiableSet(this.spectators);
    }
}
