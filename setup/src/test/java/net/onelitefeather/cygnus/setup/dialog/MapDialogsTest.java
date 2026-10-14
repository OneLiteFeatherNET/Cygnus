package net.onelitefeather.cygnus.setup.dialog;

import net.minestom.server.coordinate.Vec;
import net.minestom.server.utils.Direction;
import net.onelitefeather.cygnus.common.page.PageResource;
import net.onelitefeather.cygnus.setup.dialog.handler.DynamicDataHandler;
import net.onelitefeather.cygnus.setup.event.dialog.DialogContext;
import net.onelitefeather.cygnus.setup.map.MapDataCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapDialogsTest {

    private static final PageResource SPOT = new PageResource(new Vec(1, 40, 1), Direction.NORTH);

    @Test
    @DisplayName("Deleting a page from the list goes back to the list")
    void aDeleteFromTheListReopensIt() {
        assertTrue(DynamicDataHandler.reopensList(
                MapDialogs.deletePayload(MapDataCategory.PAGE, new DialogContext.PageContent(SPOT, true))));
    }

    @Test
    @DisplayName("Deleting a page from the world stays in the world")
    void aDeleteFromTheWorldDoesNotOpenTheList() {
        assertFalse(DynamicDataHandler.reopensList(
                MapDialogs.deletePayload(MapDataCategory.PAGE, new DialogContext.PageContent(SPOT, false))));
    }

    @Test
    @DisplayName("Every other delete goes back to its list as before")
    void otherDeletesReopenTheirList() {
        assertTrue(DynamicDataHandler.reopensList(
                MapDialogs.deletePayload(MapDataCategory.SURVIVOR, new DialogContext.PositionContent(new Vec(1, 40, 1)))));
    }
}
