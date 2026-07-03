package net.onelitefeather.cygnus.setup.dialog.handler;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.FloatBinaryTag;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerCustomClickEvent;
import net.onelitefeather.cygnus.setup.event.dialog.DialogContext;
import net.onelitefeather.cygnus.setup.event.dialog.DialogRequestEvent;
import net.onelitefeather.cygnus.setup.event.dialog.DialogTarget;

public final class AuthorAmountHandler implements DialogHandler {

    /**
     * {@inheritDoc}
     */
    @Override
    public void handle(PlayerCustomClickEvent event, CompoundBinaryTag payload) {
        FloatBinaryTag amountBinary = (FloatBinaryTag) payload.get("amount");
        if (amountBinary == null) return;
        float amount = amountBinary.value();

        if (amount == 0) return;
        EventDispatcher.call(new DialogRequestEvent(event.getPlayer(), DialogTarget.AUTHOR_INPUT, new DialogContext.AuthorAmount(amount)));
    }
}
