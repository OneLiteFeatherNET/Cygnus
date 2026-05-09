package net.onelitefeather.cygnus.view;

import net.kyori.adventure.text.Component;
import net.theevilreaper.xerus.api.Joinable;

/**
 * Represents a view that can be displayed to players
 *
 * @author theEvilReaper
 * @version 1.1.0
 * @since 0.1.0
 */
public interface GameView extends Joinable {

    /**
     * Updates the view with a new {@link Component} to display
     *
     * @param component to display
     */
    void updateView(Component component);
}
