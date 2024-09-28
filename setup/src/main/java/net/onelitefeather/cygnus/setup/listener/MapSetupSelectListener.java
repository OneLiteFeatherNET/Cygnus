package net.onelitefeather.cygnus.setup.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.setup.event.MapSetupSelectEvent;
import net.onelitefeather.cygnus.setup.util.SetupData;
import net.onelitefeather.cygnus.setup.util.SetupTags;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;

public final class MapSetupSelectListener implements Consumer<MapSetupSelectEvent> {

    private final Component alreadySelected;
    private final Component loadingMessage;
    private final SetupData setupData;

    public MapSetupSelectListener(@NotNull SetupData setupData) {
        this.alreadySelected = Messages.withPrefix(Component.text("You already selected a map", NamedTextColor.RED));
        this.loadingMessage = Messages.withPrefix(Component.text("Loading world please wait...", NamedTextColor.GRAY));
        this.setupData = setupData;
    }

    @Override
    public void accept(@NotNull MapSetupSelectEvent event) {
        Player player = event.getPlayer();
        if (setupData.hasMap()) {
            player.sendMessage(this.alreadySelected);
            event.setCancelled(true);
            return;
        }

        setupData.setSetupMode(event.getSetupMode());
        setupData.setSelectedMap(event.getMapEntry());
        Component message = Messages.withPrefix(Component.text("You selected the map: ", NamedTextColor.GRAY))
                .append(Component.text(event.getMapEntry().path().getFileName().toString(), NamedTextColor.AQUA));
        player.sendMessage(message);
        player.sendMessage(this.loadingMessage);
        setupData.loadMap();
        MinecraftServer.getSchedulerManager().buildTask(() -> handleTeleport(event.getPlayer()))
                .delay(Duration.of(3, ChronoUnit.SECONDS))
                .schedule();
    }

    private void handleTeleport(@NotNull Player player) {
        setupData.teleport(player);
        player.setTag(SetupTags.OCCUPIED_TAG, (byte) 1);
    }
}
