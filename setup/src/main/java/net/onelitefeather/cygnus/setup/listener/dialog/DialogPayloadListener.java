package net.onelitefeather.cygnus.setup.listener.dialog;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.FloatBinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import net.minestom.server.coordinate.Point;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerCustomClickEvent;
import net.onelitefeather.cygnus.setup.data.InstanceSetupData;
import net.onelitefeather.cygnus.setup.dialogs.MapDialogs;
import net.onelitefeather.cygnus.setup.event.dialog.DialogContext;
import net.onelitefeather.cygnus.setup.event.dialog.DialogRequestEvent;
import net.onelitefeather.cygnus.setup.event.dialog.DialogTarget;
import net.onelitefeather.cygnus.setup.map.MapDataCategory;
import net.onelitefeather.cygnus.setup.player.SetupPlayer;
import net.onelitefeather.guira.SetupDataService;

import java.util.function.Consumer;

public class DialogPayloadListener implements Consumer<PlayerCustomClickEvent> {

    private final SetupDataService dataService;

    public DialogPayloadListener(SetupDataService dataService) {
        this.dataService = dataService;
    }

    @Override
    public void accept(PlayerCustomClickEvent event) {
        Key key = event.getKey();
        BinaryTag payload = event.getPayload();

        if (payload == null) return;

        CompoundBinaryTag castedPayload = (CompoundBinaryTag) payload;

        if (key.equals(MapDialogs.MAP_KEY)) {
            StringBinaryTag nameBinary = (StringBinaryTag) castedPayload.get("name");
            String nameEntry = nameBinary.value();
            if (nameEntry.trim().isBlank()) {
                return;
            }

            dataService.get(event.getPlayer().getUuid()).ifPresent(data -> {
                InstanceSetupData instanceSetupData = (InstanceSetupData) data;
                instanceSetupData.getMapBuilder().name(nameEntry);
                instanceSetupData.triggerUpdate(InstanceSetupData.InventoryTarget.GENERAL);
            });
        }

        if (key.equals(MapDialogs.AUTHOR_AMOUNT_KEY)) {
            FloatBinaryTag amountBinary = (FloatBinaryTag) castedPayload.get("amount");
            if (amountBinary == null) return;
            float amount = amountBinary.value();

            if (amount == 0) return;
            EventDispatcher.call(new DialogRequestEvent(event.getPlayer(), DialogTarget.AUTHOR_INPUT, new DialogContext.AuthorAmount(amount)));
        }

        if (key.equals(MapDialogs.AUTHOR_INPUT_ENTRY_KEY)) {
            FloatBinaryTag amountBinary = (FloatBinaryTag) castedPayload.get("amount");
            if (amountBinary == null) return;

            float amount = amountBinary.value();

            String[] authors = new String[(int) amount];
            for (int i = 0; i < amount; i++) {
                authors[i] = castedPayload.getString("author_" + i);
            }

            dataService.get(event.getPlayer().getUuid()).ifPresent(data -> {
                InstanceSetupData instanceSetupData = (InstanceSetupData) data;
                instanceSetupData.getMapBuilder().builders(authors);
                instanceSetupData.triggerUpdate(InstanceSetupData.InventoryTarget.GENERAL);
            });
        }

        if (key.equals(MapDialogs.NON_DYNAMIC_DELETE_KEY)) {
            IntBinaryTag idTag = (IntBinaryTag) castedPayload.get("category_id");
            if (idTag == null) return;
            int categoryId = idTag.value();
            MapDataCategory category = MapDataCategory.byId(categoryId);

            dataService.get(event.getPlayer().getUuid()).ifPresent(data -> {
                ((InstanceSetupData)data).handleDataDelete(category);
            });
        }

        if (key.equals(MapDialogs.DYNAMIC_DELETE_KEY)) {
            IntBinaryTag idTag = (IntBinaryTag) castedPayload.get("category_id");
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
                    point = player.getPageToDelete();
                }
                ((InstanceSetupData)data).handleDataContextDelete(category, point);
                if (category == MapDataCategory.SURVIVOR) {
                    player.setSurvivorToDelete(null);
                }
                if (category == MapDataCategory.PAGE) {
                    player.setPageToDelete(null);
                }
            });

        }
    }
}
