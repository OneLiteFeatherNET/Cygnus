package net.onelitefeather.cygnus.setup;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.instance.AddEntityToInstanceEvent;
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.onelitefeather.cygnus.common.ListenerHandling;
import net.minestom.server.instance.Instance;
import net.onelitefeather.cygnus.setup.command.SetupCommand;
import net.onelitefeather.cygnus.setup.event.MapSetupSaveEvent;
import net.onelitefeather.cygnus.setup.event.MapSetupSelectEvent;
import net.onelitefeather.cygnus.setup.inventory.MapSetupInventory;
import net.onelitefeather.cygnus.setup.listener.InstanceAddListener;
import net.onelitefeather.cygnus.setup.listener.InstanceRemoveListener;
import net.onelitefeather.cygnus.setup.listener.MapSetupSelectListener;
import net.onelitefeather.cygnus.setup.listener.PageCreationListener;
import net.onelitefeather.cygnus.setup.listener.PlayerSpawnListener;
import net.onelitefeather.cygnus.setup.listener.SetupItemListener;
import net.onelitefeather.cygnus.setup.listener.map.MapSetupSaveListener;
import net.onelitefeather.cygnus.setup.map.SetupMapProvider;
import net.onelitefeather.cygnus.setup.util.SetupData;
import net.theevilreaper.aves.map.provider.AbstractMapProvider;
import net.theevilreaper.aves.util.functional.PlayerConsumer;
import net.onelitefeather.guira.SetupDataService;

import java.nio.file.Paths;
import java.util.UUID;
import java.util.function.Supplier;

public class SetupExtension implements ListenerHandling {

    private final SetupDataService dataService;
    private final SetupData setupData;
    private final MapSetupInventory mapSetupInventory;
    private final AbstractMapProvider mapProvider;

    public SetupExtension() {
        this.setupData = new SetupData();
        this.dataService = SetupDataService.create();
        this.mapProvider = new SetupMapProvider(Paths.get(""));
        this.mapSetupInventory = new MapSetupInventory(mapProvider.getEntries());
        registerSetupComponents();
        this.registerMapListeners();
    }

    private void registerSetupComponents() {
        var manager = MinecraftServer.getGlobalEventHandler();
        var commandManager = MinecraftServer.getCommandManager();
        var spawnPos = new Pos(0, 150, 0);
        commandManager.register(new SetupCommand(setupData));

        Supplier<Instance> instanceSupplier = this.mapProvider.getActiveInstance();
        UUID instanceUUID = instanceSupplier.get().getUuid();

        manager.addListener(MapSetupSelectEvent.class, new MapSetupSelectListener(setupData));
        manager.addListener(PlayerUseItemEvent.class, new SetupItemListener(setupData, mapSetupInventory));

        manager.addListener(AsyncPlayerConfigurationEvent.class, event -> event.setSpawningInstance(instanceSupplier.get()));
        manager.addListener(PlayerSpawnEvent.class, new PlayerSpawnListener(spawnPos));

        manager.addListener(PlayerBlockBreakEvent.class, new PageCreationListener(setupData));

        manager.addListener(AddEntityToInstanceEvent.class, new InstanceAddListener(instanceUUID));
        manager.addListener(RemoveEntityFromInstanceEvent.class, new InstanceRemoveListener(instanceUUID));
        registerCancelListener(manager);
    }

    /**
     * Register all listeners that are responsible for map events.
     */
    private void registerMapListeners() {
        GlobalEventHandler node = MinecraftServer.getGlobalEventHandler();
        PlayerConsumer teleport = player ->  this.mapProvider.teleportToSpawn(player, true);
        node.addListener(MapSetupSaveEvent.class, new MapSetupSaveListener(teleport));
    }
}
