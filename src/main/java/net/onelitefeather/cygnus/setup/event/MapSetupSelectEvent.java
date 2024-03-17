package net.onelitefeather.cygnus.setup.event;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.onelitefeather.cygnus.setup.MapEntry;
import net.onelitefeather.cygnus.setup.SetupMode;
import org.jetbrains.annotations.NotNull;

public final class MapSetupSelectEvent implements PlayerEvent, CancellableEvent {

    private final Player player;
    private final MapEntry mapEntry;
    private final SetupMode setupMode;
    private boolean cancelled;

    public MapSetupSelectEvent(@NotNull Player player, @NotNull MapEntry mapEntry, @NotNull SetupMode setupMode) {
        this.player = player;
        this.mapEntry = mapEntry;
        this.setupMode = setupMode;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    public @NotNull MapEntry getMapEntry() {
        return mapEntry;
    }

    public SetupMode getSetupMode() {
        return setupMode;
    }

    @Override
    public @NotNull Player getPlayer() {
        return this.player;
    }
}
