package net.onelitefeather.cygnus.setup.listener.map;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.setup.data.SetupData;
import net.onelitefeather.cygnus.setup.data.SetupDataProvider;
import net.onelitefeather.cygnus.setup.event.MapSetupSelectEvent;
import net.onelitefeather.cygnus.setup.functional.MapDataLoader;
import net.onelitefeather.cygnus.setup.util.SetupMode;
import net.onelitefeather.cygnus.setup.util.SetupTags;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;

public final class MapSetupSelectListener implements Consumer<MapSetupSelectEvent> {

    private final Component alreadySelected;
    private final Component loadingMessage;
    private final Component missingFeature;
    private final SetupDataProvider setupDataProvider;
    private final MapDataLoader loadFunction;

    public MapSetupSelectListener(@NotNull SetupDataProvider setupDataProvider, @NotNull MapDataLoader loadFunction) {
        this.alreadySelected = Messages.withPrefix(Component.text("You already selected a map", NamedTextColor.RED));
        this.loadingMessage = Messages.withPrefix(Component.text("Loading world please wait...", NamedTextColor.GRAY));
        this.missingFeature = Messages.withPrefix(Component.text("In the game setup is it not possible to delete locations at the moment", NamedTextColor.RED))
                .append(Component.newline())
                .append(Messages.withPrefix(Component.text("Please contact an administrator", NamedTextColor.RED)));
        this.setupDataProvider = setupDataProvider;
        this.loadFunction = loadFunction;
    }

    @Override
    public void accept(@NotNull MapSetupSelectEvent event) {
        Player player = event.getPlayer();
        SetupData setupData = setupDataProvider.getSetupData(player);

        if (setupData != null && setupData.hasMap()) {
            player.sendMessage(this.alreadySelected);
            event.setCancelled(true);
            return;
        }
        SetupData.Builder dataBuilder;

        if (setupData == null) {
            dataBuilder = SetupData.builder().player(player);
        } else {
            dataBuilder = SetupData.builder(setupData);
        }

        dataBuilder
                .mode(event.getSetupMode())
                .mapEntry(event.getMapEntry())
                .baseMap(this.loadFunction.loadMapData(event.getMapEntry().getMapFile(), event.getSetupMode()))
        ;

        setupData = dataBuilder.build();
        setupDataProvider.addSetupData(player, setupData);
        player.setTag(SetupTags.SETUP_ID_TAG, event.getSetupMode().ordinal());
        Component message = Messages.withPrefix(Component.text("You selected the map: ", NamedTextColor.GRAY))
                .append(Component.text(event.getMapEntry().path().getFileName().toString(), NamedTextColor.AQUA));
        player.sendMessage(message);
        player.sendMessage(this.loadingMessage);

        if (event.getSetupMode() == SetupMode.GAME) {
            player.sendMessage(this.missingFeature);
        }

        setupData.loadMap();
        player.getInventory().clear();
        MinecraftServer.getSchedulerManager().buildTask(setupData::teleport)
                .delay(Duration.of(3, ChronoUnit.SECONDS))
                .schedule();
    }
}
