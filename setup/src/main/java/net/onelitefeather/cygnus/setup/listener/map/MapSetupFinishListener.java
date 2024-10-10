package net.onelitefeather.cygnus.setup.listener.map;

import net.onelitefeather.cygnus.setup.data.SetupData;
import net.onelitefeather.cygnus.setup.event.MapSetupFinishEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class MapSetupFinishListener implements Consumer<MapSetupFinishEvent> {

    @Override
    public void accept(@NotNull MapSetupFinishEvent event) {
        SetupData setupData = event.setupData();
        setupData.reset();
    }
}
