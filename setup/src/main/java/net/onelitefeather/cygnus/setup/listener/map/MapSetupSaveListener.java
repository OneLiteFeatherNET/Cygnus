package net.onelitefeather.cygnus.setup.listener.map;

import net.minestom.server.MinecraftServer;
import net.onelitefeather.cygnus.setup.event.MapSetupSaveEvent;
import net.onelitefeather.guira.SetupDataService;
import net.theevilreaper.aves.util.functional.PlayerConsumer;

import java.util.function.Consumer;

public class MapSetupSaveListener implements Consumer<MapSetupSaveEvent> {

    private final SetupDataService dataService;
    private final PlayerConsumer teleportBackLogic;

    public MapSetupSaveListener(SetupDataService dataService, PlayerConsumer teleportBackLogic) {
        this.teleportBackLogic = teleportBackLogic;
        this.dataService = dataService;
    }

    @Override
    public void accept(MapSetupSaveEvent event) {
        //TODO: add save back
        this.teleportBackLogic.accept(event.getPlayer());
        MinecraftServer.getSchedulerManager().scheduleNextTick(dataService.get(event.getPlayer().getUuid()).get()::reset);
    }
}
