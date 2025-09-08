package net.onelitefeather.cygnus.setup.listener;

import net.theevilreaper.aves.map.BaseMap;
import net.theevilreaper.aves.util.functional.PlayerConsumer;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.setup.inventory.MapSetupInventory;
import net.onelitefeather.cygnus.setup.util.SetupData;
import net.onelitefeather.cygnus.setup.util.SetupItems;
import net.onelitefeather.cygnus.setup.util.SetupTags;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiPredicate;
import java.util.function.Consumer;

public final class SetupItemListener implements Consumer<PlayerUseItemEvent> {

    private final SetupData setupData;
    private final MapSetupInventory mapSetupInventory;
    private final BiPredicate<SetupData, BaseMap> saveLogic;
    private final PlayerConsumer teleportBackLogic;

    public SetupItemListener(
            @NotNull SetupData setupData,
            @NotNull MapSetupInventory mapSetupInventory,
            @NotNull BiPredicate<SetupData, BaseMap> saveLogic,
            @NotNull PlayerConsumer teleportBackLogic
    ) {
        this.setupData = setupData;
        this.mapSetupInventory = mapSetupInventory;
        this.saveLogic = saveLogic;
        this.teleportBackLogic = teleportBackLogic;
    }

    @Override
    public void accept(@NotNull PlayerUseItemEvent event) {
        if (!event.getItemStack().hasTag(Tags.ITEM_TAG)) return;

        var player = event.getPlayer();
        byte tagValue = event.getItemStack().getTag(Tags.ITEM_TAG);

        if (1 == tagValue && player.hasTag(SetupTags.SETUP_ID_TAG)) {
            if (!this.saveLogic.test(setupData, setupData.getBaseMap())) {
                player.sendMessage("An error occurred while saving a map");
            }
            this.teleportBackLogic.accept(player);
            MinecraftServer.getSchedulerManager().scheduleNextTick(setupData::reset);
            return;
        }

        // Check if the given tag value is 0 which represents the item for the map selection
        if (SetupItems.ZERO_INDEX == tagValue && !setupData.hasMap()) {
            mapSetupInventory.open(player);
        }
    }
}
