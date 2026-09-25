package net.onelitefeather.cygnus.setup.dialog.handler;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.DoubleBinaryTag;
import net.kyori.adventure.nbt.FloatBinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerCustomClickEvent;
import net.onelitefeather.cygnus.setup.data.GameData;
import net.onelitefeather.cygnus.setup.dialog.CreekRouteDialogs;
import net.onelitefeather.cygnus.setup.util.SetupMessages;
import net.onelitefeather.guira.SetupDataService;
import org.jetbrains.annotations.Nullable;

/**
 * Applies the pauses from the dialog to the active creek route.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class CreekRoutePauseHandler implements DialogHandler {

    private final SetupDataService dataService;

    public CreekRoutePauseHandler(SetupDataService dataService) {
        this.dataService = dataService;
    }

    @Override
    public void handle(PlayerCustomClickEvent event, CompoundBinaryTag payload) {
        int startMillis = millis(payload.get(CreekRouteDialogs.START_PAUSE_INPUT));
        int endMillis = millis(payload.get(CreekRouteDialogs.END_PAUSE_INPUT));
        if (startMillis < 0 || endMillis < 0) return;
        Player player = event.getPlayer();
        this.dataService.get(player.getUuid()).ifPresent(data -> {
            if (!(data instanceof GameData gameData)) return;
            String activeRoute = gameData.activeCreekRoute();
            if (activeRoute == null) {
                player.sendMessage(SetupMessages.NO_ACTIVE_CREEK_ROUTE);
                return;
            }
            if (!gameData.setCreekStartPause(startMillis)) {
                player.sendMessage(SetupMessages.CREEK_PAUSE_NEEDS_POINT);
                return;
            }
            if (!gameData.setCreekEndPause(endMillis)) {
                player.sendMessage(SetupMessages.getCreekStartPauseSet(activeRoute, startMillis));
                player.sendMessage(SetupMessages.CREEK_END_PAUSE_NEEDS_TWO_POINTS);
                return;
            }
            player.sendMessage(SetupMessages.getCreekPausesSet(activeRoute, startMillis, endMillis));
        });
    }

    /**
     * Turns a slider value in seconds into milliseconds.
     *
     * @return the milliseconds, or {@code -1} if the client sent nothing usable
     */
    private static int millis(@Nullable BinaryTag tag) {
        return switch (tag) {
            case FloatBinaryTag value -> Math.round(value.value() * 1000f);
            case DoubleBinaryTag value -> (int) Math.round(value.value() * 1000D);
            case IntBinaryTag value -> value.value() * 1000;
            case null, default -> -1;
        };
    }
}
