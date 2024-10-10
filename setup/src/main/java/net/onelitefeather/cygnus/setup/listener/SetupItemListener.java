package net.onelitefeather.cygnus.setup.listener;

import de.icevizion.aves.map.BaseMap;
import de.icevizion.aves.util.functional.PlayerConsumer;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.timer.ExecutionType;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.setup.data.SetupData;
import net.onelitefeather.cygnus.setup.data.SetupDataProvider;
import net.onelitefeather.cygnus.setup.inventory.MapSetupInventory;
import net.onelitefeather.cygnus.setup.util.SetupItems;
import net.onelitefeather.cygnus.setup.util.SetupTags;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

public final class SetupItemListener implements Consumer<PlayerUseItemEvent> {

    private final SetupDataProvider setupDataProvider;
    private final MapSetupInventory mapSetupInventory;
    private final BiPredicate<SetupData, BaseMap> saveLogic;
    private final PlayerConsumer teleportBackLogic;

    public SetupItemListener(
            @NotNull SetupDataProvider setupDataProvider,
            @NotNull MapSetupInventory mapSetupInventory,
            @NotNull BiPredicate<SetupData, BaseMap> saveLogic,
            @NotNull PlayerConsumer teleportBackLogic
    ) {
        this.setupDataProvider = setupDataProvider;
        this.mapSetupInventory = mapSetupInventory;
        this.saveLogic = saveLogic;
        this.teleportBackLogic = teleportBackLogic;
    }

    @Override
    public void accept(@NotNull PlayerUseItemEvent event) {
        if (!event.getItemStack().hasTag(Tags.ITEM_TAG)) return;

        Player player = event.getPlayer();
        byte tagValue = event.getItemStack().getTag(Tags.ITEM_TAG);

        SetupData setupData = setupDataProvider.getSetupData(player);
        if (player.hasTag(SetupTags.SETUP_ID_TAG)) {
            if (1 == tagValue) {
                if (!this.saveLogic.test(setupData, setupData.getBaseMap())) {
                    player.sendMessage("An error occurred while saving a map");
                }
                this.teleportBackLogic.accept(player);
                MinecraftServer.getSchedulerManager().buildTask(setupData::reset)
                        .delay(Duration.ofMillis(75)).executionType(ExecutionType.TICK_END).schedule();
                return;
            }

            // Check if the given tag value is 0 which represents the item for the map selection
            if (SetupItems.SECOND_INDEX == tagValue) {
                setupData.openInventory();
            }
            return;
        }

        // Check if the given tag value is 0 which represents the item for the map selection
        if (!player.hasTag(SetupTags.SETUP_ID_TAG) && SetupItems.ZERO_INDEX == tagValue) {
            mapSetupInventory.open(player);
        }
    }
}
