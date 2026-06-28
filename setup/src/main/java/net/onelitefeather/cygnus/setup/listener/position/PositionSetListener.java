package net.onelitefeather.cygnus.setup.listener.position;

import net.onelitefeather.cygnus.common.map.GameMapBuilder;
import net.onelitefeather.cygnus.setup.data.GameData;
import net.onelitefeather.cygnus.setup.data.InstanceSetupData;
import net.onelitefeather.cygnus.setup.data.LobbyData;
import net.onelitefeather.cygnus.setup.event.PositionSetEvent;
import net.onelitefeather.cygnus.setup.map.MapDataCategory;
import net.onelitefeather.guira.SetupDataService;
import net.onelitefeather.guira.data.SetupData;

import java.util.Optional;
import java.util.function.Consumer;

public class PositionSetListener implements Consumer<PositionSetEvent> {

    private final SetupDataService dataService;

    public PositionSetListener(SetupDataService dataService) {
        this.dataService = dataService;
    }

    @Override
    public void accept(PositionSetEvent event) {
        MapDataCategory category = event.getCategory();

        Optional<SetupData> optionalSetupData = this.dataService.get(event.getPlayer().getUuid());

        if (optionalSetupData.isEmpty()) return;

        switch (optionalSetupData.get()) {
            case LobbyData lobbyData -> {
                lobbyData.getMapBuilder().spawn(event.getPos());
                lobbyData.triggerUpdate(InstanceSetupData.InventoryTarget.GENERAL);
            }
            case GameData gameData -> {
                switch (category) {
                    case SPAWN -> {
                        gameData.getMapBuilder().spawn(event.getPos());
                        gameData.triggerUpdate(InstanceSetupData.InventoryTarget.GENERAL);

                    }
                    case SLENDER -> {
                        ((GameMapBuilder) gameData.getMapBuilder()).setSlenderSpawn(event.getPos());
                        gameData.triggerUpdate(InstanceSetupData.InventoryTarget.GENERAL);
                    }
                    default -> {
                        // Nothing to do here
                    }
                }
            }
            default -> {
                // Nothing to do here
            }
        }


    }
}
