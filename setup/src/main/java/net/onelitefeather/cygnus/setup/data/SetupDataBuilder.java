package net.onelitefeather.cygnus.setup.data;

import de.icevizion.aves.map.BaseMap;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.common.map.MapEntry;
import net.onelitefeather.cygnus.setup.functional.MapDataLoader;
import net.onelitefeather.cygnus.setup.util.SetupMode;
import org.jetbrains.annotations.NotNull;

/**
 * The {@link SetupDataBuilder} is the implementation of the {@link SetupData.Builder} interface definition.
 * It allows the creation of a new {@link SetupData} implementation instance.
 * A builder has the ability to set the required data step by step and finally build the instance. So the problem of
 * setting all data directly in the constructor of the class is not present anymore.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @see SetupData
 * @since 1.0.0
 */
public final class SetupDataBuilder implements SetupData.Builder {

    private MapEntry mapEntry;
    private SetupMode mode;
    private Player player;
    private BaseMap baseMap;

    /**
     * Constructs a new {@link SetupDataBuilder} instance.
     */
    public SetupDataBuilder() {
        
    }

    /**
     * Constructs a new {@link SetupDataBuilder} instance.
     * @param setupData the setup data to create a new instance
     */
    public SetupDataBuilder(@NotNull SetupData setupData) {
        this.player = setupData.getPlayer();
    }

    @Override
    public SetupData.@NotNull Builder mapEntry(@NotNull MapEntry mapEntry) {
        this.mapEntry = mapEntry;
        return this;
    }

    @Override
    public SetupData.@NotNull Builder mode(@NotNull SetupMode mode) {
        this.mode = mode;
        return this;
    }

    @Override
    public SetupData.@NotNull Builder player(@NotNull Player player) {
        this.player = player;
        return this;
    }

    @Override
    public SetupData.@NotNull Builder baseMap(@NotNull BaseMap baseMap) {
        this.baseMap = baseMap;
        return this;
    }

    @Override
    public @NotNull SetupData build() {
        return switch (mode) {
            case LOBBY -> new LobbyData(this.player, this.mapEntry, this.mode, this.baseMap);
            case GAME -> new GameData(this.player, this.mapEntry, this.mode, this.baseMap);
        };
    }
}
