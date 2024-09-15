package net.onelitefeather.cygnus.setup;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.instance.AddEntityToInstanceEvent;
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.extensions.Extension;
import net.minestom.server.instance.Instance;
import net.onelitefeather.cygnus.common.ListenerHandling;
import net.onelitefeather.cygnus.common.map.MapProvider;
import net.onelitefeather.cygnus.setup.command.SetupCommand;
import net.onelitefeather.cygnus.setup.event.MapSetupSelectEvent;
import net.onelitefeather.cygnus.setup.functional.SaveMapFunction;
import net.onelitefeather.cygnus.setup.inventory.MapSetupInventory;
import net.onelitefeather.cygnus.setup.listener.InstanceAddListener;
import net.onelitefeather.cygnus.setup.listener.InstanceRemoveListener;
import net.onelitefeather.cygnus.setup.listener.MapSetupSelectListener;
import net.onelitefeather.cygnus.setup.listener.PageCreationListener;
import net.onelitefeather.cygnus.setup.listener.SetupItemListener;
import net.onelitefeather.cygnus.setup.util.SetupData;
import net.onelitefeather.cygnus.setup.util.SetupItems;
import org.jetbrains.annotations.NotNull;

public class SetupExtension extends Extension implements ListenerHandling {

    private SetupItems setupItems;
    private SetupData setupData;
    private MapSetupInventory mapSetupInventory;
    private MapProvider mapProvider;

    @Override
    public void initialize() {

    }

    @Override
    public void terminate() {

    }

    private void registerSetupComponents() {
        var manager = MinecraftServer.getGlobalEventHandler();
        var commandManager = MinecraftServer.getCommandManager();
        var setupMapInventory = new MapSetupInventory(mapProvider.getAvailableMaps());
        var setupData = new SetupData();
        var setupItems = new SetupItems();
        var spawnPos = new Pos(0, 150, 0);
        var mainInstance = MinecraftServer.getInstanceManager().getInstances().iterator().next();
        commandManager.register(new SetupCommand(setupData));

        manager.addListener(MapSetupSelectEvent.class, new MapSetupSelectListener(setupData));
        var mapSaveFunction = new SaveMapFunction(mapProvider);
        manager.addListener(PlayerUseItemEvent.class, new SetupItemListener(setupData, setupMapInventory, mapSaveFunction::saveMap, player -> setMainInstance(player, mainInstance)));

        var instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        MinecraftServer.getInstanceManager().registerInstance(instance);

        manager.addListener(AsyncPlayerConfigurationEvent.class, playerLoginEvent -> playerLoginEvent.setSpawningInstance(instance));
        manager.addListener(PlayerSpawnEvent.class, playerSpawnEvent -> {
            playerSpawnEvent.getPlayer().teleport(spawnPos);
            playerSpawnEvent.getPlayer().setGameMode(GameMode.CREATIVE);
            setupItems.setMapSelection(playerSpawnEvent.getPlayer());
        });

        manager.addListener(PlayerBlockBreakEvent.class, new PageCreationListener(setupData));
        manager.addListener(AddEntityToInstanceEvent.class, new InstanceAddListener(instance.getUniqueId(), setupItems));
        manager.addListener(RemoveEntityFromInstanceEvent.class, new InstanceRemoveListener(instance.getUniqueId()));
        registerCancelListener(manager);
    }

    private void setMainInstance(@NotNull Player player, @NotNull Instance instance) {
        player.setInstance(instance, Vec.ZERO);
    }

}
