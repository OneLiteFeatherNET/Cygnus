package net.onelitefeather.cygnus.setup.listener;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.setup.event.MapSetupSelectEvent;
import net.onelitefeather.cygnus.setup.util.SetupData;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;

public final class MapSetupSelectListener implements Consumer<MapSetupSelectEvent> {

    private final SetupData setupData;

    public MapSetupSelectListener(@NotNull SetupData setupData) {
        this.setupData = setupData;
    }

    @Override
    public void accept(@NotNull MapSetupSelectEvent event) {
        if (setupData.hasMap()) {
            event.getPlayer().sendMessage("You already selected a map");
            event.setCancelled(true);
            return;
        }

        setupData.setSelectedMap(event.getMapEntry());
        setupData.setSetupMode(event.getSetupMode());
        event.getPlayer().sendMessage("You selected the map " + event.getMapEntry().path().getFileName());
        event.getPlayer().sendMessage("Loading world please wait...");
        setupData.loadMap();
        MinecraftServer.getSchedulerManager().buildTask(() -> handleTeleport(event.getPlayer()))
                .delay(Duration.of(3, ChronoUnit.SECONDS))
                .schedule();
    }

    private void handleTeleport(@NotNull Player player) {
        setupData.teleport(player);
        player.setTag(Tags.OCCUPIED_TAG, (byte) 1);
    }
}
