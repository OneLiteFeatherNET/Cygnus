package net.onelitefeather.cygnus.setup.data;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.InstanceContainer;
import net.onelitefeather.guira.data.SetupData;
import net.theevilreaper.aves.map.BaseMapBuilder;
import net.theevilreaper.aves.map.MapEntry;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Base implementation for setup data working with a temporary map instance.
 *
 * <p>Provides shared functionality for managing setup state, boss bar updates,
 * instance lifecycle handling and map-related access. Subclasses implement
 * setup-specific interaction and update logic.</p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.1.0
 */
public abstract class InstanceSetupData implements SetupData {

    protected static final Pos SPAWN_POINT = new Pos(0, 100, 0);

    protected UUID uuid;
    protected MapEntry mapEntry;
    protected @Nullable InstanceContainer instance;
    protected BossBar bossBar;
    protected @Nullable Component title;

    /**
     * Creates a new setup data container.
     *
     * @param uuid     unique identifier of this setup session
     * @param mapEntry map entry associated with the setup
     * @param color    boss bar color used for setup feedback
     */
    protected InstanceSetupData(UUID uuid, MapEntry mapEntry, BossBar.Color color) {
        this.uuid = uuid;
        this.mapEntry = mapEntry;
        this.bossBar = BossBar.bossBar(Component.empty(), 1, color, BossBar.Overlay.PROGRESS);
    }

    /**
     * Updates the boss bar title.
     *
     * <p>If no title is set, a fallback title is used.</p>
     */
    public void updateTitle() {
        if (this.title == null) {
            this.title = Component.text("Please set a title", NamedTextColor.RED);
        }
        this.bossBar.name(title);
    }

    /**
     * Displays setup-related information to the given player.
     *
     * @param player target player
     */
    public void teleport(Player player) {
        player.showBossBar(this.bossBar);
    }

    /**
     * Opens the setup inventory for the given player.
     *
     * @param player target player
     */
    public abstract void openInventory(Player player);

    /**
     * Triggers an update of the setup state and related visual feedback.
     */
    public abstract void triggerUpdate();

    /**
     * Checks whether a map file is available for this setup.
     *
     * @return true if a map file exists, otherwise false
     */
    public boolean hasMapFile() {
        return this.mapEntry.hasMapFile();
    }

    /**
     * Resets this setup and unregisters its instance if present.
     */
    @Override
    public void reset() {
        if (instance == null) return;
        MinecraftServer.getInstanceManager().unregisterInstance(instance);
    }

    /**
     * Returns the unique identifier of this setup.
     *
     * @return setup id
     */
    @Override
    public UUID getId() {
        return this.uuid;
    }

    /**
     * Returns the map builder used by this setup.
     *
     * @return associated map builder
     */
    public abstract BaseMapBuilder getMapBuilder();
}