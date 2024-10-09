package net.onelitefeather.cygnus.setup.data;

import de.icevizion.aves.map.BaseMap;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.common.map.MapEntry;
import net.onelitefeather.cygnus.setup.functional.MapDataLoader;
import net.onelitefeather.cygnus.setup.inventory.LobbyViewInventory;
import net.onelitefeather.cygnus.setup.util.SetupMode;
import org.jetbrains.annotations.NotNull;

public final class LobbyData extends SetupDataImpl {

    private final LobbyViewInventory inventory;

    LobbyData(
            @NotNull MapDataLoader loader,
            @NotNull Player player,
            @NotNull MapEntry mapEntry,
            @NotNull SetupMode mode,
            @NotNull BaseMap baseMap
    ) {
        super(loader, player, mapEntry, mode, baseMap);
        this.inventory = new LobbyViewInventory(baseMap);
    }

    @Override
    public void openInventory() {
        player.openInventory(inventory.getInventory());
    }

    @Override
    public void reset() {
        super.reset();
        this.inventory.unregister();
    }
}
