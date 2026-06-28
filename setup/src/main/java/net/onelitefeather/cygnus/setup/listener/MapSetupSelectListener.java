package net.onelitefeather.cygnus.setup.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.setup.data.GameData;
import net.onelitefeather.cygnus.setup.data.InstanceSetupData;
import net.onelitefeather.cygnus.setup.data.LobbyData;
import net.onelitefeather.cygnus.setup.event.MapSetupSelectEvent;
import net.onelitefeather.cygnus.setup.util.SetupTags;
import net.onelitefeather.guira.SetupDataService;
import net.onelitefeather.guira.data.SetupData;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.function.Consumer;

public final class MapSetupSelectListener implements Consumer<MapSetupSelectEvent> {

    private final Component alreadySelected;
    private final Component loadingMessage;
    private final SetupDataService dataService;

    public MapSetupSelectListener(SetupDataService dataService) {
        this.alreadySelected = Messages.withPrefix(Component.text("You already selected a map", NamedTextColor.RED));
        this.loadingMessage = Messages.withPrefix(Component.text("Loading world please wait...", NamedTextColor.GRAY));
        this.dataService = dataService;
    }

    @Override
    public void accept(MapSetupSelectEvent event) {
        Player player = event.getPlayer();

        Optional<SetupData> optionalSetupData = this.dataService.get(player.getUuid());

        if (optionalSetupData.isPresent()) {
            player.sendMessage(this.alreadySelected);
            event.setCancelled(true);
            return;
        }

        SetupData setupData = switch (event.getSetupMode()) {
            case GAME -> new GameData(player.getUuid(), event.getMapEntry());
            case LOBBY -> new LobbyData(player.getUuid(), event.getMapEntry());
        };

        player.setTag(SetupTags.SETUP_ID_TAG, event.getSetupMode().ordinal());
        Component message = Messages.withPrefix(Component.text("You selected the map: ", NamedTextColor.GRAY))
                .append(Component.text(event.getMapEntry().getDirectoryRoot().getFileName().toString(), NamedTextColor.AQUA));
        player.sendMessage(message);
        player.sendMessage(this.loadingMessage);
        player.getInventory().clear();
        this.dataService.add(player.getUuid(), setupData);
        MinecraftServer.getSchedulerManager().buildTask(() -> handleTeleport(event.getPlayer(), setupData))
                .delay(Duration.of(3, ChronoUnit.SECONDS))
                .schedule();
    }

    private void handleTeleport(Player player, SetupData setupData) {
        ((InstanceSetupData) setupData).teleport(player);
    }
}
