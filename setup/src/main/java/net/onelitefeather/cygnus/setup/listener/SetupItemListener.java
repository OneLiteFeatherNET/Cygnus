package net.onelitefeather.cygnus.setup.listener;

import net.minestom.server.event.EventDispatcher;
import net.onelitefeather.cygnus.setup.data.GameData;
import net.onelitefeather.cygnus.setup.data.InstanceSetupData;
import net.onelitefeather.cygnus.setup.event.MapSetupSaveEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.setup.inventory.MapSetupInventory;
import net.onelitefeather.cygnus.setup.item.SetupItems;
import net.onelitefeather.cygnus.setup.util.SetupMessages;
import net.onelitefeather.cygnus.setup.util.SetupTags;
import net.onelitefeather.guira.SetupDataService;

import java.util.Set;
import java.util.function.Consumer;

public final class SetupItemListener implements Consumer<PlayerUseItemEvent> {

    private final SetupDataService dataService;
    private final MapSetupInventory mapSetupInventory;

    public SetupItemListener(SetupDataService dataService, MapSetupInventory mapSetupInventory) {
        this.dataService = dataService;
        this.mapSetupInventory = mapSetupInventory;
    }

    @Override
    public void accept(PlayerUseItemEvent event) {
        if (!event.getItemStack().hasTag(Tags.ITEM_TAG)) return;

        var player = event.getPlayer();
        byte tagValue = event.getItemStack().getTag(Tags.ITEM_TAG);

        if (1 == tagValue && player.hasTag(SetupTags.SETUP_ID_TAG)) {
            EventDispatcher.call(new MapSetupSaveEvent(player));
            return;
        }

        // Check if the given tag value is 0 which represents the item for the map selection
        if (SetupItems.ZERO_INDEX == tagValue) {
            mapSetupInventory.open(player);
        }

        if (!player.hasTag(SetupTags.SETUP_ID_TAG)) return;

        this.dataService.get(player.getUuid()).ifPresent(data ->
                ((InstanceSetupData) data).handleItemInteraction(player, tagValue));

    }
}
