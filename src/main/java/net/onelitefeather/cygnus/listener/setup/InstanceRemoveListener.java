package net.onelitefeather.cygnus.listener.setup;

import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent;
import net.onelitefeather.cygnus.utils.Tags;
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
        if (!event.getEntity().hasTag(Tags.OCCUPIED_TAG)) {
            event.getEntity().removeTag(Tags.OCCUPIED_TAG);
        }
    }
}
