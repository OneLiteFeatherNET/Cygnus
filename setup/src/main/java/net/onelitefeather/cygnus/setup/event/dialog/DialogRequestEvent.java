package net.onelitefeather.cygnus.setup.event.dialog;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.Nullable;

/**
 * The event will be used in the setup to handle the open request for different dialog targets.
 *
 * @author Joltra
 * @version 1.0.0
 * @since 0.1.0
 */
public final class DialogRequestEvent implements PlayerEvent {

    private final Player player;
    private final DialogTarget target;
    private final @Nullable DialogContext context;

    /**
     * Creates a new event instance with the given value
     *
     * @param player the involved event
     * @param target the target of the dialog
     */
    public DialogRequestEvent(Player player, DialogTarget target, @Nullable DialogContext context) {
        this.player = player;
        this.target = target;
        this.context = context;
    }

    /**
     * Creates a new event instance with the given value
     *
     * @param player the involved event
     * @param target the target of the dialog
     */
    public DialogRequestEvent(Player player, DialogTarget target) {
        this.player = player;
        this.target = target;
        this.context = null;
    }

    /**
     * Returns the target of the dialog
     *
     * @return the dialog target
     */
    public DialogTarget getTarget() {
        return this.target;
    }

    /**
     * Returns the context which contains additional information about the dialog request
     * @return the additional context
     */
    public @Nullable DialogContext getContext() {
        return this.context;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Player getPlayer() {
        return this.player;
    }
}
