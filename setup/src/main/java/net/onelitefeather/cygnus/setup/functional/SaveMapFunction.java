package net.onelitefeather.cygnus.setup.functional;

import net.theevilreaper.aves.map.BaseMap;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.common.map.MapProvider;
import net.onelitefeather.cygnus.setup.util.SetupData;
import org.jetbrains.annotations.Nullable;

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 **/
public final class SaveMapFunction {

    private final MapProvider mapProvider;

    public SaveMapFunction(MapProvider mapProvider) {
        this.mapProvider = mapProvider;
    }

    public boolean saveMap(@Nullable SetupData setupData, BaseMap baseMap) {
        if (setupData == null || !setupData.hasMap()) return false;
        this.mapProvider.saveMap(setupData.getSelectedMap().getMapFile(), baseMap instanceof GameMap gameMap ? gameMap : baseMap);
        return true;
    }
}
