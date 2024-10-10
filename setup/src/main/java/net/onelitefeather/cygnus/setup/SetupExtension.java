package net.onelitefeather.cygnus.setup;

import de.icevizion.aves.file.FileHandler;
import de.icevizion.aves.file.GsonFileHandler;
import de.icevizion.aves.map.BaseMap;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.instance.AddEntityToInstanceEvent;
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.extensions.Extension;
import net.minestom.server.instance.InstanceContainer;
import net.onelitefeather.cygnus.common.ListenerHandling;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.common.map.MapProvider;
import net.onelitefeather.cygnus.common.page.PageProvider;
import net.onelitefeather.cygnus.common.util.GsonUtil;
import net.onelitefeather.cygnus.setup.command.SetupCommand;
import net.onelitefeather.cygnus.setup.data.SetupDataProvider;
import net.onelitefeather.cygnus.setup.event.MapSetupSelectEvent;
import net.onelitefeather.cygnus.setup.functional.SaveMapFunction;
import net.onelitefeather.cygnus.setup.inventory.MapSetupInventory;
import net.onelitefeather.cygnus.setup.listener.PageCreationListener;
import net.onelitefeather.cygnus.setup.listener.PlayerDisconnectListener;
import net.onelitefeather.cygnus.setup.listener.PlayerSpawnListener;
import net.onelitefeather.cygnus.setup.listener.SetupItemListener;
import net.onelitefeather.cygnus.setup.listener.instance.InstanceAddListener;
import net.onelitefeather.cygnus.setup.listener.instance.InstanceRemoveListener;
import net.onelitefeather.cygnus.setup.listener.map.MapSetupSelectListener;
import net.onelitefeather.cygnus.setup.util.SetupItems;
import net.onelitefeather.cygnus.setup.util.SetupMode;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;

public class SetupExtension extends Extension implements ListenerHandling {

    private final FileHandler fileHandler;
    private final SetupItems setupItems;
    private final InstanceContainer instanceContainer;
    private final SetupDataProvider dataProvider;
    private SaveMapFunction mapSaveFunction;
    private PageProvider pageProvider;
    private MapSetupInventory mapSetupInventory;
    private MapProvider mapProvider;

    public SetupExtension() {
        this.fileHandler = new GsonFileHandler(GsonUtil.GSON);
        this.setupItems = new SetupItems();
        this.instanceContainer = MinecraftServer.getInstanceManager().createInstanceContainer();
        this.dataProvider = new SetupDataProvider();
    }

    @Override
    public void initialize() {
        MinecraftServer.getInstanceManager().registerInstance(instanceContainer);
        this.pageProvider = new PageProvider(() -> {
        });
        this.mapProvider = new MapProvider(getDataDirectory(), this.instanceContainer, this.pageProvider);
        this.mapSaveFunction = new SaveMapFunction(mapProvider);
        this.mapSetupInventory = new MapSetupInventory(mapProvider.getAvailableMaps());
        registerSetupComponents();
    }

    @Override
    public void terminate() {

    }

    private void registerSetupComponents() {
        var manager = MinecraftServer.getGlobalEventHandler();
        var commandManager = MinecraftServer.getCommandManager();
        var spawnPos = new Pos(0, 150, 0);
        commandManager.register(new SetupCommand(dataProvider));

        manager.addListener(MapSetupSelectEvent.class, new MapSetupSelectListener(dataProvider, this::loadMapData));
        manager.addListener(PlayerUseItemEvent.class, new SetupItemListener(dataProvider, mapSetupInventory, mapSaveFunction::saveMap, this::setMainInstance));

        manager.addListener(AsyncPlayerConfigurationEvent.class, event -> event.setSpawningInstance(instanceContainer));
        manager.addListener(PlayerSpawnEvent.class, new PlayerSpawnListener(spawnPos, setupItems, instance -> this.instanceContainer.getUniqueId().equals(instance.getUniqueId())));

        manager.addListener(PlayerBlockBreakEvent.class, new PageCreationListener(dataProvider));
        manager.addListener(AddEntityToInstanceEvent.class, new InstanceAddListener(instanceContainer.getUniqueId(), setupItems));
        manager.addListener(RemoveEntityFromInstanceEvent.class, new InstanceRemoveListener(instanceContainer.getUniqueId()));
        manager.addListener(PlayerDisconnectEvent.class, new PlayerDisconnectListener(dataProvider::removeSetupData));
        registerCancelListener(manager);
    }

    private @NotNull BaseMap loadMapData(@NotNull Path path, @NotNull SetupMode mode) {
        if (!Files.exists(path)) {
            return mode == SetupMode.LOBBY ? new BaseMap() : new GameMap();
        }
        return switch (mode) {
            case LOBBY -> fileHandler.load(path, BaseMap.class).get();
            case GAME -> fileHandler.load(path, GameMap.class).get();
        };
    }

    private void setMainInstance(@NotNull Player player) {
        player.setInstance(this.instanceContainer, Vec.ZERO);
    }
}
