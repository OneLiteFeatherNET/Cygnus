package net.onelitefeather.cygnus.listener.setup;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.instance.AddEntityToInstanceEvent;
import net.onelitefeather.cygnus.setup.SetupItems;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.function.Consumer;

public final class InstanceAddListener implements Consumer<AddEntityToInstanceEvent> {

    private final UUID mainInstanceID;
    private final SetupItems setupItems;

    public InstanceAddListener(@NotNull UUID mainInstanceID, @NotNull SetupItems setupItems) {
        this.mainInstanceID = mainInstanceID;
        this.setupItems = setupItems;
    }

    @Override
    public void accept(@NotNull AddEntityToInstanceEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (event.getInstance().getUniqueId().equals(mainInstanceID)) return;
        MinecraftServer.getSchedulerManager().buildTask(() -> this.setupItems.setSaveData((Player) event.getEntity()))
                .delay(Duration.of(3, ChronoUnit.SECONDS))
                .schedule();
    }
}
