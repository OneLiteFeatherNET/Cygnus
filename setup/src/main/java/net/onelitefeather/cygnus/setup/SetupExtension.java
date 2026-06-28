package net.onelitefeather.cygnus.setup;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.instance.AddEntityToInstanceEvent;
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerCustomClickEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.onelitefeather.cygnus.common.ListenerHandling;
import net.minestom.server.instance.Instance;
import net.onelitefeather.cygnus.setup.event.MapSetupSaveEvent;
import net.onelitefeather.cygnus.setup.event.MapSetupSelectEvent;
import net.onelitefeather.cygnus.setup.event.PlayerRemoveDataEvent;
import net.onelitefeather.cygnus.setup.event.PositionSetEvent;
import net.onelitefeather.cygnus.setup.event.dialog.DialogRequestEvent;
import net.onelitefeather.cygnus.setup.inventory.MapSetupInventory;
import net.onelitefeather.cygnus.setup.listener.InstanceAddListener;
import net.onelitefeather.cygnus.setup.listener.InstanceRemoveListener;
import net.onelitefeather.cygnus.setup.listener.MapSetupSelectListener;
import net.onelitefeather.cygnus.setup.listener.PageCreationListener;
import net.onelitefeather.cygnus.setup.listener.PlayerSpawnListener;
import net.onelitefeather.cygnus.setup.listener.SetupItemListener;
import net.onelitefeather.cygnus.setup.listener.SpawnCreationListener;
import net.onelitefeather.cygnus.setup.listener.data.PlayerRemoveDataListener;
import net.onelitefeather.cygnus.setup.listener.dialog.DialogPayloadListener;
import net.onelitefeather.cygnus.setup.listener.dialog.DialogRequestListener;
import net.onelitefeather.cygnus.setup.listener.map.MapSetupSaveListener;
import net.onelitefeather.cygnus.setup.listener.position.PositionSetListener;
import net.onelitefeather.cygnus.setup.map.SetupMapProvider;
import net.theevilreaper.aves.map.provider.AbstractMapProvider;
import net.theevilreaper.aves.util.functional.PlayerConsumer;
import net.onelitefeather.guira.SetupDataService;

import java.nio.file.Paths;
import java.util.UUID;
import java.util.function.Supplier;

public class SetupExtension implements ListenerHandling {

    private final SetupDataService dataService;
    private final MapSetupInventory mapSetupInventory;
    private final AbstractMapProvider mapProvider;

    public SetupExtension() {
        this.dataService = SetupDataService.create();
        this.mapProvider = new SetupMapProvider(Paths.get("").resolve("setup"));
        this.mapSetupInventory = new MapSetupInventory(mapProvider.getEntries());
        registerSetupComponents();
        this.registerMapListeners();
    }

    private void registerSetupComponents() {
        var manager = MinecraftServer.getGlobalEventHandler();
        var spawnPos = new Pos(0, 150, 0);

        Supplier<Instance> instanceSupplier = this.mapProvider.getActiveInstance();
        UUID instanceUUID = instanceSupplier.get().getUuid();

        manager.addListener(MapSetupSelectEvent.class, new MapSetupSelectListener(this.dataService));
        manager.addListener(PlayerUseItemEvent.class, new SetupItemListener(this.dataService, mapSetupInventory));

        manager.addListener(AsyncPlayerConfigurationEvent.class, event -> event.setSpawningInstance(instanceSupplier.get()));
        manager.addListener(PlayerSpawnEvent.class, new PlayerSpawnListener(spawnPos));

        manager.addListener(PlayerDisconnectEvent.class, event -> {
            this.dataService.remove(event.getPlayer().getUuid());
        });
        manager.addListener(PlayerBlockBreakEvent.class, new PageCreationListener(this.dataService));
        manager.addListener(EventListener.builder(PlayerBlockBreakEvent.class)
                .ignoreCancelled(false)
                .handler(new SpawnCreationListener(this.dataService))
                .build());
        manager.addListener(AddEntityToInstanceEvent.class, new InstanceAddListener(instanceUUID));
        manager.addListener(RemoveEntityFromInstanceEvent.class, new InstanceRemoveListener(instanceUUID));
        registerCancelListener(manager);

        //Dialog listener
        manager.addListener(DialogRequestEvent.class, new DialogRequestListener());
        manager.addListener(PlayerCustomClickEvent.class, new DialogPayloadListener(this.dataService));

        manager.addListener(PositionSetEvent.class, new PositionSetListener(this.dataService));
        manager.addListener(PlayerRemoveDataEvent.class, new PlayerRemoveDataListener());
    }

    /**
     * Register all listeners that are responsible for map events.
     */
    private void registerMapListeners() {
        GlobalEventHandler node = MinecraftServer.getGlobalEventHandler();
        PlayerConsumer teleport = player ->  this.mapProvider.teleportToSpawn(player, true);
        node.addListener(MapSetupSaveEvent.class, new MapSetupSaveListener(this.dataService, teleport));
    }
}
