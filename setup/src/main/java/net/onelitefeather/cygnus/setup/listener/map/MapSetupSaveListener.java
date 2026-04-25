package net.onelitefeather.cygnus.setup.listener.map;

import net.minestom.server.MinecraftServer;
import net.onelitefeather.cygnus.setup.event.MapSetupSaveEvent;
import net.theevilreaper.aves.util.functional.PlayerConsumer;

import java.util.function.Consumer;

public class MapSetupSaveListener implements Consumer<MapSetupSaveEvent> {

    private final PlayerConsumer teleportBackLogic;

    public MapSetupSaveListener(PlayerConsumer teleportBackLogic) {
        this.teleportBackLogic = teleportBackLogic;
    }

    @Override
    public void accept(MapSetupSaveEvent event) {
        //TODO: add save back
        this.teleportBackLogic.accept(event.getPlayer());
        MinecraftServer.getSchedulerManager().scheduleNextTick(event.getSetupData()::reset);
    }
}
