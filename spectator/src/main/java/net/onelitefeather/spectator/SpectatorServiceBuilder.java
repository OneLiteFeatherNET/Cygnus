package net.onelitefeather.spectator;

import net.kyori.adventure.text.Component;
import net.onelitefeather.spectator.item.SpectatorItem;
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

    public SpectatorServiceBuilder() {
        this.prefix = Component.text("Spectators");
        this.separator = Component.text(", ");
        this.items = new HashSet<>();
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
    public SpectatorService.@NotNull Builder hotbarItem(@NotNull SpectatorItem item) {
        this.items.add(item);
        return this;
    }

    @Override
    public @NotNull SpectatorService build() {
        Map<Integer, SpectatorItem> hotBarItems = null;

        if (this.items != null && !this.items.isEmpty()) {
            hotBarItems = this.items.stream()
                    .collect(Collectors.toMap(SpectatorItem::itemIndex, Function.identity()));
        }

        if (hotBarItems == null || hotBarItems.isEmpty()) {
            return DefaultSpectatorService.of(this.prefix, this.separator);
        }

        return DefaultSpectatorService.of(this.prefix, this.separator, hotBarItems);
    }
}
