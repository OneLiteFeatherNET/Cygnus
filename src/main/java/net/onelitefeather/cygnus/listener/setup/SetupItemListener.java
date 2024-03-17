package net.onelitefeather.cygnus.listener.setup;

import de.icevizion.aves.map.BaseMap;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.onelitefeather.cygnus.setup.SetupData;
import net.onelitefeather.cygnus.setup.inventory.MapSetupInventory;
import net.onelitefeather.cygnus.utils.Tags;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiPredicate;
import java.util.function.Consumer;

public final class SetupItemListener implements Consumer<PlayerUseItemEvent> {

    private final SetupData setupData;
    private final MapSetupInventory mapSetupInventory;
    private final BiPredicate<SetupData, BaseMap> saveLogic;
    private final Consumer<Player> teleportBackLogic;

    public SetupItemListener(
            @NotNull SetupData setupData,
            @NotNull MapSetupInventory mapSetupInventory,
            @NotNull BiPredicate<SetupData, BaseMap> saveLogic,
            @NotNull Consumer<Player> teleportBackLogic
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

        if (1 == tagValue && player.hasTag(Tags.OCCUPIED_TAG)) {
            if (!this.saveLogic.test(setupData, setupData.getBaseMap())) {
                player.sendMessage("An error occurred while saving a map");
            }
            this.teleportBackLogic.accept(player);
            return;
        }

        // Check if the given tag value is 0 which represents the item for the map selection
        if (0 == tagValue && setupData.hasMap()) {
            setupData.teleport(player);
            setupData.reset();
            return;
        }

        mapSetupInventory.open(event.getPlayer());
    }
}
