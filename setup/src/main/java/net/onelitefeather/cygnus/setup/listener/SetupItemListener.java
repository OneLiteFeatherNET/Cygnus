package net.onelitefeather.cygnus.setup.listener;

import net.minestom.server.event.EventDispatcher;
import net.onelitefeather.cygnus.setup.event.MapSetupSaveEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.setup.inventory.MapSetupInventory;
import net.onelitefeather.cygnus.setup.util.SetupData;
import net.onelitefeather.cygnus.setup.item.SetupItems;
import net.onelitefeather.cygnus.setup.util.SetupTags;

import java.util.function.Consumer;

public final class SetupItemListener implements Consumer<PlayerUseItemEvent> {

    private final SetupData setupData;
    private final MapSetupInventory mapSetupInventory;

    public SetupItemListener(SetupData setupData, MapSetupInventory mapSetupInventory) {
        this.setupData = setupData;
        this.mapSetupInventory = mapSetupInventory;
    }

    @Override
    public void accept(PlayerUseItemEvent event) {
        if (!event.getItemStack().hasTag(Tags.ITEM_TAG)) return;

        var player = event.getPlayer();
        byte tagValue = event.getItemStack().getTag(Tags.ITEM_TAG);

        if (1 == tagValue && player.hasTag(SetupTags.SETUP_ID_TAG)) {
            EventDispatcher.call(new MapSetupSaveEvent(player, setupData));
            return;
        }

        // Check if the given tag value is 0 which represents the item for the map selection
        if (SetupItems.ZERO_INDEX == tagValue && !setupData.hasMap()) {
            mapSetupInventory.open(player);
        }
    }
}
