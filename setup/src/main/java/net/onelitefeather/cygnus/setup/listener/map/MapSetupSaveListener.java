package net.onelitefeather.cygnus.setup.listener.map;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.setup.event.MapSetupSaveEvent;
import net.onelitefeather.guira.SetupDataService;
import net.onelitefeather.guira.data.SetupData;
import net.theevilreaper.aves.util.functional.PlayerConsumer;

import java.util.Optional;
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
        Player player = event.getPlayer();
        Optional<SetupData> setupData = this.dataService.get(player.getUuid());

        if (setupData.isEmpty()) return;

        SetupData data = setupData.get();

        data.save();
        this.teleportBackLogic.accept(event.getPlayer());
        MinecraftServer.getSchedulerManager().scheduleNextTick(data::reset);
    }
}
