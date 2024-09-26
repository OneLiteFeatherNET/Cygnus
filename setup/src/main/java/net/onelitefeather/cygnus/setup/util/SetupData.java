package net.onelitefeather.cygnus.setup.util;

import de.icevizion.aves.map.BaseMap;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.anvil.AnvilLoader;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.common.map.MapEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

public final class SetupData {

    private static final Pos SPAWN_POINT = new Pos(0, 100, 0);

    private MapEntry selectedMap;
    private SetupMode setupMode;
    private BaseMap baseMap;

    private AnvilLoader anvilLoader;
    private InstanceContainer instance;
    private boolean pageMode;

    public void setSetupMode(@NotNull SetupMode setupMode) {
        if (this.setupMode != null) return;
        this.setupMode = setupMode;
        this.baseMap = setupMode == SetupMode.LOBBY ? new BaseMap() : new GameMap();
    }

    public void setPageMode(boolean pageMode) {
        this.pageMode = pageMode;
    }

    public void setSelectedMap(@NotNull MapEntry selectedMap) {
        if (this.selectedMap != null) return;
        this.selectedMap = selectedMap;
    }

    public void reset() {
        this.selectedMap = null;
        this.setupMode = null;
        MinecraftServer.getInstanceManager().unregisterInstance(this.instance);
    }

    public void loadMap() {
        this.anvilLoader = new AnvilLoader(this.selectedMap.path());
        this.instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        this.instance.setChunkLoader(this.anvilLoader);
        MinecraftServer.getInstanceManager().registerInstance(this.instance);
    }

    public boolean hasMap() {
        return this.selectedMap != null;
    }

    /**
     * Returns if the page setup mode is activated or not.
     * @return true when its active otherwise false
     */
    public boolean hasPageMode() {
        return pageMode;
    }

    /**
     * Teleports a {@link Player} to the {@link net.minestom.server.instance.Instance} which is used for the setup.
     * @param player the player to teleport
     */
    public void teleport(@NotNull Player player) {
        player.setInstance(this.instance, SPAWN_POINT);
    }

    /**
     * Returns a {@link MapEntry} which represents the selected map for the setup process.
     * @return the given selected map if present otherwise null
     */
    public @Nullable MapEntry getSelectedMap() {
        return selectedMap;
    }

    /**
     * Returns the given instance from a {@link BaseMap}.
     * @return the underlying reference or null
     */
    public @Nullable BaseMap getBaseMap() {
        return baseMap;
    }

    /**
     * Returns the given {@link SetupMode} reference.
     * @return the given reference from the mode or null
     */
    public @UnknownNullability SetupMode getSetupMode() {
        return setupMode;
    }
}
