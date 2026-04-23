package net.onelitefeather.cygnus.setup.listener;

import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent;
import net.onelitefeather.cygnus.setup.util.SetupTags;

import java.util.UUID;
import java.util.function.Consumer;

public class InstanceRemoveListener implements Consumer<RemoveEntityFromInstanceEvent> {

    private final UUID mainInstanceID;

    public InstanceRemoveListener(UUID mainInstanceID) {
        this.mainInstanceID = mainInstanceID;
    }

    @Override
    public void accept(RemoveEntityFromInstanceEvent event) {
        if (!event.getInstance().getUuid().equals(mainInstanceID)) return;

        if (!event.getEntity().hasTag(SetupTags.SETUP_ID_TAG)) {
            event.getEntity().removeTag(SetupTags.SETUP_ID_TAG);
        }
    }
}
