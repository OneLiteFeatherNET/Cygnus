package net.onelitefeather.cygnus.setup.listener;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.instance.AddEntityToInstanceEvent;
import net.onelitefeather.cygnus.setup.item.SetupItems;
import net.onelitefeather.cygnus.setup.util.SetupTags;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.function.Consumer;

public final class InstanceAddListener implements Consumer<AddEntityToInstanceEvent> {

    private final UUID mainInstanceID;

    public InstanceAddListener(UUID mainInstanceID) {
        this.mainInstanceID = mainInstanceID;
    }

    @Override
    public void accept(AddEntityToInstanceEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getInstance().getUuid().equals(mainInstanceID)) return;

        MinecraftServer.getSchedulerManager().scheduleNextTick(() -> {
            Integer modeTag = player.getTag(SetupTags.SETUP_ID_TAG);
            int modeId = modeTag != null ? modeTag : -1;
            if (modeId == 0) {
                SetupItems.setLobbyLayout(player);
                return;
            }
            SetupItems.setGameLayout(player);
        });
    }
}
