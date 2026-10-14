package net.onelitefeather.cygnus.setup.dialog.handler;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import net.minestom.server.coordinate.Point;
import net.minestom.server.event.player.PlayerCustomClickEvent;
import net.onelitefeather.cygnus.setup.data.InstanceSetupData;
import net.onelitefeather.cygnus.setup.map.MapDataCategory;
import net.onelitefeather.cygnus.setup.player.SetupPlayer;
import net.onelitefeather.guira.SetupDataService;

public final class DynamicDataHandler implements DialogHandler{

    /** The payload key that says whether the builder goes back to the list after a delete. */
    public static final String REOPEN_LIST = "reopen_list";

    private final SetupDataService dataService;

    public DynamicDataHandler(SetupDataService dataService) {
        this.dataService = dataService;
    }

    /**
     * Tells whether the builder goes back to the list after a delete. A payload without the flag is
     * from a list, like every delete before the page preview.
     *
     * @param payload the payload of the confirmed delete
     * @return {@code true} if the list opens again
     */
    public static boolean reopensList(CompoundBinaryTag payload) {
        return payload.getBoolean(REOPEN_LIST, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handle(PlayerCustomClickEvent event, CompoundBinaryTag payload) {
        IntBinaryTag idTag = (IntBinaryTag) payload.get("category_id");
        if (idTag == null) return;
        int categoryId = idTag.value();
        MapDataCategory category = MapDataCategory.byId(categoryId);

        dataService.get(event.getPlayer().getUuid()).ifPresent(data -> {
            SetupPlayer player = (SetupPlayer) event.getPlayer();
            Point point = null;
            if (category == MapDataCategory.SURVIVOR) {
                point = player.getSurvivorToDelete();
            }
            if (category == MapDataCategory.PAGE) {
                point = player.getPageToDelete() != null ? player.getPageToDelete() : (player.getPageResource() != null ? player.getPageResource().position() : null);
            }
            InstanceSetupData setupData = (InstanceSetupData) data;
            setupData.handleDataContextDelete(category, point);
            if (category == MapDataCategory.SURVIVOR) {
                player.setSurvivorToDelete(null);
                setupData.openInventory(InstanceSetupData.InventoryTarget.SURVIVOR);
            }
            if (category == MapDataCategory.PAGE) {
                player.setPageToDelete(null);
                player.setPageResource(null);
                if (reopensList(payload)) {
                    setupData.openInventory(InstanceSetupData.InventoryTarget.PAGE);
                }
            }
        });

    }
}
