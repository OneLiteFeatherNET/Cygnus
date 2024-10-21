package net.onelitefeather.cygnus.setup;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.instance.AddEntityToInstanceEvent;
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.InstanceContainer;
import net.onelitefeather.cygnus.common.ListenerHandling;
import net.onelitefeather.cygnus.common.map.MapProvider;
import net.onelitefeather.cygnus.common.page.PageProvider;
import net.onelitefeather.cygnus.setup.command.SetupCommand;
import net.onelitefeather.cygnus.setup.event.MapSetupSelectEvent;
import net.onelitefeather.cygnus.setup.functional.SaveMapFunction;
import net.onelitefeather.cygnus.setup.inventory.MapSetupInventory;
import net.onelitefeather.cygnus.setup.listener.InstanceAddListener;
import net.onelitefeather.cygnus.setup.listener.InstanceRemoveListener;
import net.onelitefeather.cygnus.setup.listener.MapSetupSelectListener;
import net.onelitefeather.cygnus.setup.listener.PageCreationListener;
import net.onelitefeather.cygnus.setup.listener.PlayerSpawnListener;
import net.onelitefeather.cygnus.setup.listener.SetupItemListener;
import net.onelitefeather.cygnus.setup.util.SetupData;
import net.onelitefeather.cygnus.setup.util.SetupItems;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Paths;

public class SetupExtension implements ListenerHandling {

    private final SetupItems setupItems;
    private final SetupData setupData;
    private final InstanceContainer instanceContainer;
    private final SaveMapFunction mapSaveFunction;
    private final PageProvider pageProvider;
    private final MapSetupInventory mapSetupInventory;
    private final MapProvider mapProvider;

    public SetupExtension() {
        this.setupItems = new SetupItems();
        this.setupData = new SetupData();
        this.instanceContainer = MinecraftServer.getInstanceManager().createInstanceContainer();
        MinecraftServer.getInstanceManager().registerInstance(instanceContainer);
        this.pageProvider = new PageProvider(() -> {
        });
        this.mapProvider = new MapProvider(Paths.get(""), this.instanceContainer, this.pageProvider);
        this.mapSaveFunction = new SaveMapFunction(mapProvider);
        this.mapSetupInventory = new MapSetupInventory(mapProvider.getAvailableMaps());
        registerSetupComponents();
    }

    private void registerSetupComponents() {
        var manager = MinecraftServer.getGlobalEventHandler();
        var commandManager = MinecraftServer.getCommandManager();
        var spawnPos = new Pos(0, 150, 0);
        commandManager.register(new SetupCommand(setupData));

        manager.addListener(MapSetupSelectEvent.class, new MapSetupSelectListener(setupData));
        manager.addListener(PlayerUseItemEvent.class, new SetupItemListener(setupData, mapSetupInventory, mapSaveFunction::saveMap, this::setMainInstance));

        manager.addListener(AsyncPlayerConfigurationEvent.class, event -> event.setSpawningInstance(instanceContainer));
        manager.addListener(PlayerSpawnEvent.class, new PlayerSpawnListener(spawnPos, setupItems, instance -> this.instanceContainer.getUniqueId().equals(instance.getUniqueId())));

        manager.addListener(PlayerBlockBreakEvent.class, new PageCreationListener(setupData));
        manager.addListener(AddEntityToInstanceEvent.class, new InstanceAddListener(instanceContainer.getUniqueId(), setupItems));
        manager.addListener(RemoveEntityFromInstanceEvent.class, new InstanceRemoveListener(instanceContainer.getUniqueId()));
        registerCancelListener(manager);
    }

    private void setMainInstance(@NotNull Player player) {
        player.setInstance(this.instanceContainer, Vec.ZERO);
    }
}
