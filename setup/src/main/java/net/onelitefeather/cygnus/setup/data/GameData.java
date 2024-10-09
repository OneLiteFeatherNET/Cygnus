package net.onelitefeather.cygnus.setup.data;

import de.icevizion.aves.map.BaseMap;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.common.map.MapEntry;
import net.onelitefeather.cygnus.setup.functional.MapDataLoader;
import net.onelitefeather.cygnus.setup.inventory.DataViewInventory;
import net.onelitefeather.cygnus.setup.util.SetupMode;
import org.jetbrains.annotations.NotNull;

public final class GameData extends SetupDataImpl {

    private final DataViewInventory inventory;

    GameData(@NotNull MapDataLoader loader, @NotNull Player player, @NotNull MapEntry mapEntry, @NotNull SetupMode mode, @NotNull BaseMap baseMap) {
        super(loader, player, mapEntry, mode, baseMap);
        this.inventory = new DataViewInventory(((GameMap) baseMap), player);
    }

    @Override
    public void openInventory() {
        this.inventory.openInventory();
    }

    @Override
    public void reset() {
        super.reset();
        this.inventory.clear();
    }
}
