package net.onelitefeather.cygnus.setup.dialog.handler;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.event.player.PlayerCustomClickEvent;

/**
 * Represents a custom handler to deal with different data contexts from a dialog.
 *
 * @version 1.0.0
 * @author theEvilReaper
 * @since 2.6.6
 */
@FunctionalInterface
public interface DialogHandler {

    /**
     * Handles the payload logic for a specific dialog key.
     *
     * @param event   which is involved
     * @param payload of the dialog
     */
    void handle(PlayerCustomClickEvent event, CompoundBinaryTag payload);
}
