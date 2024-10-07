package net.onelitefeather.cygnus.setup.util;

import de.icevizion.aves.inventory.InventoryBuilder;
import de.icevizion.aves.map.BaseMap;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.anvil.AnvilLoader;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.common.map.MapEntry;
import net.onelitefeather.cygnus.setup.inventory.LobbyViewInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

/**
 * The {@link SetupData} is a class which stores different data about a setup process.
 * Compared to other setup modes in other games it allows to have multiple maps in the setup.
 * To avoid issue during these specific session each setup has its own data to store important data
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
public final class SetupData {

    private static final Pos SPAWN_POINT = new Pos(0, 100, 0);

    private MapEntry selectedMap;
    private SetupMode setupMode;
    private BaseMap baseMap;

    private InstanceContainer instance;
    private InventoryBuilder inventoryBuilder;
    private boolean pageMode;

    private BossBar bossBar;

    public SetupData() {
        this.bossBar = BossBar.bossBar(Component.empty(), 1, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
    }

    /**
     * Updates the mode for the selection process.
     *
     * @param setupMode the mode to set
     */
    public void setSetupMode(@NotNull SetupMode setupMode) {
        if (this.setupMode != null) return;
        this.setupMode = setupMode;
        this.baseMap = setupMode == SetupMode.LOBBY ? new BaseMap() : new GameMap();
        this.inventoryBuilder = new LobbyViewInventory(this.baseMap);
    }

    /**
     * Updates the current sub mode to the page setup
     */
    public void swapPageMode() {
        if (this.setupMode == SetupMode.LOBBY) return;
        this.pageMode = !this.pageMode;
    }

    /**
     * Updates the selected map entry for the setup
     *
     * @param selectedMap the new map which should receive their setup
     */
    public void setSelectedMap(@NotNull MapEntry selectedMap) {
        if (this.selectedMap != null) return;
        this.selectedMap = selectedMap;
        TextComponent title = Component.text("Setup mode: ")
                .append(Component.text(setupMode.name(), NamedTextColor.LIGHT_PURPLE))
                .append(Component.text(", Map: "))
                .append(Component.text(selectedMap.path().getFileName().toString(), NamedTextColor.LIGHT_PURPLE));
        this.bossBar.name(title);
    }

    /**
     * Resets the given data from the setup.
     * It also removes the underlying {@link InstanceContainer} which holds the loaded world.
     * When the method is invoked the map will be unloaded by the system.
     */
    public void reset() {
        this.selectedMap = null;
        this.setupMode = null;
        MinecraftServer.getInstanceManager().unregisterInstance(this.instance);
        this.bossBar = null;
    }

    /**
     * Removes the boss bar from the given player.
     *
     * @param player the player to remove the boss bar
     */
    public void removeBossBar(@NotNull Player player) {
        player.hideBossBar(this.bossBar);
    }

    /**
     * Loads the given data from the map into the setup reference.
     */
    public void loadMap() {
        AnvilLoader anvilLoader = new AnvilLoader(this.selectedMap.path());
        this.instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        this.instance.setChunkLoader(anvilLoader);
        MinecraftServer.getInstanceManager().registerInstance(this.instance);
    }

    public void openInventory(@NotNull Player player) {
        player.openInventory(this.inventoryBuilder.getInventory());
    }

    /**
     * Returns an indicator if the reference contains a map for the setup.
     *
     * @return true when yes otherwise false
     */
    public boolean hasMap() {
        return this.selectedMap != null;
    }

    /**
     * Returns if the page setup mode is activated or not.
     *
     * @return true when its active otherwise false
     */
    public boolean hasPageMode() {
        return pageMode;
    }

    /**
     * Teleports a {@link Player} to the {@link net.minestom.server.instance.Instance} which is used for the setup.
     *
     * @param player the player to teleport
     */
    public void teleport(@NotNull Player player) {
        player.setInstance(this.instance, SPAWN_POINT);
        player.showBossBar(this.bossBar);
    }

    /**
     * Returns a {@link MapEntry} which represents the selected map for the setup process.
     *
     * @return the given selected map if present otherwise null
     */
    public @Nullable MapEntry getSelectedMap() {
        return selectedMap;
    }

    /**
     * Returns the given instance from a {@link BaseMap}.
     *
     * @return the underlying reference or null
     */
    public @Nullable BaseMap getBaseMap() {
        return baseMap;
    }

    /**
     * Returns the given {@link SetupMode} reference.
     *
     * @return the given reference from the mode or null
     */
    public @UnknownNullability SetupMode getSetupMode() {
        return setupMode;
    }
}
