package net.onelitefeather.cygnus.setup.listener;

import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent;
import net.onelitefeather.cygnus.setup.util.SetupTags;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.function.Consumer;

public class InstanceRemoveListener implements Consumer<RemoveEntityFromInstanceEvent> {

    private final UUID mainInstanceID;

    public InstanceRemoveListener(@NotNull UUID mainInstanceID) {
        this.mainInstanceID = mainInstanceID;
    }

    @Override
    public void accept(@NotNull RemoveEntityFromInstanceEvent event) {
        if (!event.getInstance().getUniqueId().equals(mainInstanceID)) return;

        if (!event.getEntity().hasTag(SetupTags.SETUP_ID_TAG)) {
            event.getEntity().removeTag(SetupTags.SETUP_ID_TAG);
        }
    }
}
