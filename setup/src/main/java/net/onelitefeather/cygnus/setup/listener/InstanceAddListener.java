package net.onelitefeather.cygnus.setup.listener;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.instance.AddEntityToInstanceEvent;
import net.onelitefeather.cygnus.setup.data.LobbyData;
import net.onelitefeather.cygnus.setup.item.SetupItems;
import net.onelitefeather.guira.SetupDataService;

import java.util.UUID;
import java.util.function.Consumer;

public final class InstanceAddListener implements Consumer<AddEntityToInstanceEvent> {

    private final UUID mainInstanceID;
    private final SetupDataService dataService;

    public InstanceAddListener(UUID mainInstanceID, SetupDataService dataService) {
        this.mainInstanceID = mainInstanceID;
        this.dataService = dataService;
    }

    @Override
    public void accept(AddEntityToInstanceEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getInstance().getUuid().equals(mainInstanceID)) return;

        MinecraftServer.getSchedulerManager().scheduleNextTick(() -> {
            if (this.dataService.get(player.getUuid()).orElse(null) instanceof LobbyData) {
                SetupItems.setLobbyLayout(player);
                return;
            }
            SetupItems.setGameLayout(player);
        });
    }
}
