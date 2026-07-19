package net.onelitefeather.cygnus.listener.game;

import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.event.SlenderVisibilityChangeEvent;

import java.util.function.Consumer;

/**
 * Listens for the {@link SlenderVisibilityChangeEvent} to update the visibility rule of the Slender.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
public final class SlenderVisibilityChangeListener implements Consumer<SlenderVisibilityChangeEvent> {

    @Override
    public void accept(SlenderVisibilityChangeEvent event) {
        Player slender = event.getPlayer();
        boolean hidden = event.isHidden();
        slender.updateViewableRule(viewer -> !hidden);
    }
}
