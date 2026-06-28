package net.onelitefeather.cygnus.setup.listener.position;

import net.onelitefeather.cygnus.setup.data.InstanceSetupData;
import net.onelitefeather.cygnus.setup.event.PositionSetEvent;
import net.onelitefeather.guira.SetupDataService;

import java.util.function.Consumer;

public class PositionSetListener implements Consumer<PositionSetEvent> {

    private final SetupDataService dataService;

    public PositionSetListener(SetupDataService dataService) {
        this.dataService = dataService;
    }

    @Override
    public void accept(PositionSetEvent event) {
        this.dataService.get(event.getPlayer().getUuid()).ifPresent(data ->
                ((InstanceSetupData) data).setPosition(event.getCategory(), event.getPlayer()));
    }
}
