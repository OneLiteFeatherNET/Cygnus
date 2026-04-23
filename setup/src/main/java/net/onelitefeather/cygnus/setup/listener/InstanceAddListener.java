package net.onelitefeather.cygnus.setup.listener;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.instance.AddEntityToInstanceEvent;
import net.onelitefeather.cygnus.setup.util.SetupItems;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.function.Consumer;

public final class InstanceAddListener implements Consumer<AddEntityToInstanceEvent> {

    private final UUID mainInstanceID;

    public InstanceAddListener(UUID mainInstanceID) {
        this.mainInstanceID = mainInstanceID;
    }

    @Override
    public void accept(AddEntityToInstanceEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (event.getInstance().getUuid().equals(mainInstanceID)) return;
        MinecraftServer.getSchedulerManager().buildTask(() -> SetupItems.setSaveData((Player) event.getEntity()))
                .delay(Duration.of(3, ChronoUnit.SECONDS))
                .schedule();
    }
}
