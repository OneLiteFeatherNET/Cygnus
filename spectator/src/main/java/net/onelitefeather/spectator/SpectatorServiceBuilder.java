package net.onelitefeather.spectator;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.onelitefeather.spectator.item.SpectatorItem;
import net.onelitefeather.spectator.util.ChatData;
import net.onelitefeather.spectator.util.ListenerData;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class SpectatorServiceBuilder implements SpectatorService.Builder {

    private final Set<SpectatorItem> items;
    private Component prefix;
    private Component separator;
    private TextColor messageColor;
    private boolean detectSpectatorQuit;
    private boolean spectatorChat;

    public SpectatorServiceBuilder() {
        this.prefix = Component.text("[", NamedTextColor.GRAY).append(Component.text("!", NamedTextColor.RED)).append(Component.text("] ", NamedTextColor.GRAY));
        this.separator = Component.text("≫ ", NamedTextColor.YELLOW);
        this.items = new HashSet<>();
        this.messageColor = NamedTextColor.GRAY;
    }

    @Override
    public SpectatorService.@NotNull Builder prefix(@NotNull Component prefix) {
        this.prefix = prefix;
        return this;
    }

    @Override
    public SpectatorService.@NotNull Builder separator(@NotNull Component separator) {
        this.separator = separator;
        return this;
    }

    @Override
    public SpectatorService.@NotNull Builder messageColor(@NotNull TextColor messageColor) {
        this.messageColor = messageColor;
        return this;
    }

    @Override
    public SpectatorService.@NotNull Builder hotbarItem(@NotNull SpectatorItem item) {
        this.items.add(item);
        return this;
    }

    @Override
    public SpectatorService.@NotNull Builder detectSpectatorQuit() {
        if (this.detectSpectatorQuit) return this;
        this.detectSpectatorQuit = true;
        return this;
    }

    @Override
    public SpectatorService.@NotNull Builder spectatorChat() {
        if (this.spectatorChat) return this;
        this.spectatorChat = true;
        return this;
    }


    @Override
    public @NotNull SpectatorService build() {
        Map<Integer, SpectatorItem> hotBarItems = null;

        if (this.items != null && !this.items.isEmpty()) {
            hotBarItems = this.items.stream()
                    .collect(Collectors.toMap(SpectatorItem::itemIndex, Function.identity()));
        }

        ListenerData listenerData = ListenerData.of(this.detectSpectatorQuit, this.spectatorChat);
        ChatData chatData = new ChatData(this.prefix, this.separator, this.messageColor);

        if (hotBarItems == null || hotBarItems.isEmpty()) {
            return DefaultSpectatorService.of(listenerData, chatData);
        }
        return DefaultSpectatorService.of(listenerData, chatData, hotBarItems);
    }
}
