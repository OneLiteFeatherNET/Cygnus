package net.onelitefeather.cygnus.setup.data;

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
import net.onelitefeather.cygnus.common.map.MapEntry;

import net.onelitefeather.cygnus.setup.functional.MapDataLoader;
import net.onelitefeather.cygnus.setup.inventory.ConfirmInventory;
import net.onelitefeather.cygnus.setup.util.SetupMode;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract sealed class SetupDataImpl implements SetupData permits LobbyData, GameData {

    private static final Pos SPAWN_POINT = new Pos(0, 100, 0);

    protected final Player player;

    protected MapEntry mapEntry;
    protected SetupMode mode;
    protected BaseMap baseMap;
    protected InstanceContainer instance;
    protected boolean pageMode;
    protected BossBar bossBar;

    protected ConfirmInventory confirmInventory;

    SetupDataImpl(
            @NotNull Player player,
            @NotNull MapEntry mapEntry,
            @NotNull SetupMode mode,
            @NotNull BaseMap baseMap
    ) {
        this.mapEntry = mapEntry;
        this.mode = mode;
        this.player = player;
        this.bossBar = BossBar.bossBar(Component.empty(), 1, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
        this.baseMap = baseMap;
    }

    @Override
    public void openConfirmInventory() {
        if (this.confirmInventory == null) {
            this.confirmInventory = new ConfirmInventory(() -> {});
        }
        this.player.openInventory(this.confirmInventory.getInventory());
    }

    @Override
    public void updateTitle() {
        TextComponent title = Component.text("Setup mode: ", NamedTextColor.GRAY)
                .append(Component.text(mode.getName(), NamedTextColor.LIGHT_PURPLE))
                .append(Component.text(", Map: ", NamedTextColor.GRAY))
                .append(Component.text(mapEntry.path().getFileName().toString(), NamedTextColor.LIGHT_PURPLE));
        this.bossBar.name(title);
    }

    @Override
    public boolean loadMap() {
        if (this.instance != null && this.instance.isRegistered()) {
            return false;
        }
        if (this.instance == null) {
            this.instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        }
        AnvilLoader anvilLoader = new AnvilLoader(this.mapEntry.path());
        this.instance.setChunkLoader(anvilLoader);
        this.updateTitle();
        MinecraftServer.getInstanceManager().registerInstance(this.instance);
        return true;
    }

    @Override
    public void reset() {
        if (instance == null) return;
        this.mode = null;
        this.mapEntry = null;
        this.baseMap = null;
        player.hideBossBar(this.bossBar);
        MinecraftServer.getInstanceManager().unregisterInstance(instance);
    }

    @Override
    public void teleport() {
        Pos spawnPoint = baseMap.hasSpawn() ? baseMap.getSpawn() : SPAWN_POINT;
        player.setInstance(this.instance, spawnPoint);
        player.showBossBar(this.bossBar);
    }

    @Override
    public boolean hasMap() {
        return this.mapEntry != null;
    }

    @Override
    public void swapPageMode() {
        if (this.mode == SetupMode.LOBBY) return;
        this.pageMode = !this.pageMode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SetupDataImpl setupData = (SetupDataImpl) o;
        return Objects.equals(player.getUuid(), setupData.player.getUuid());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(player.getUuid());
    }

    @Override
    public boolean hasPageMode() {
        return this.pageMode;
    }

    @Override
    public @NotNull MapEntry getMapEntry() {
        return this.mapEntry;
    }

    @Override
    public @NotNull SetupMode getSetupMode() {
        return this.mode;
    }

    @Override
    public @NotNull Player getPlayer() {
        return this.player;
    }

    @Override
    public @NotNull BaseMap getBaseMap() {
        return this.baseMap;
    }
}
