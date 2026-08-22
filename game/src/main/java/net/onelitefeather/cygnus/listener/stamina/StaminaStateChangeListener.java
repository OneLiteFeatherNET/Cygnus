package net.onelitefeather.cygnus.listener.stamina;

import net.onelitefeather.cygnus.event.StaminaStateChangeEvent;
import net.onelitefeather.cygnus.visibility.VisibilityRules;

import java.util.function.Consumer;

/**
 * Turns a stamina state change of the slender into a visibility update.
 * <p>
 * {@code SlenderBar#enterDraining(boolean)} and {@code SlenderBar#enterRegenerating()} flip
 * {@link net.onelitefeather.cygnus.common.Tags#HIDDEN} and then fire the
 * {@link StaminaStateChangeEvent}. Re-evaluating the viewable rule here is the single mechanism that
 * translates that tag into spawn and destroy packets, and it is reached by the manual toggle
 * ({@code SlenderBarTrigger}) as well as by the automatic transition once the bar runs dry, so both paths
 * behave identically.
 * <p>
 * The former implementation additionally broadcast {@code Player#getMetadataPacket()} to every online
 * player. That broadcast is gone and is not needed for anything else, not even for the slender running as
 * {@code EntityType.ENDERMAN}: Minestom pushes every metadata change to the entity's viewers on its own
 * ({@code MetadataHolder} change listener), {@code Entity#updateNewViewer(Player)} sends the metadata as
 * part of the spawn sequence, and {@code Entity#switchEntityType(EntityType)} respawns the entity for its
 * current viewers itself. Broadcasting it to everybody only leaked the slender's metadata to players that
 * must not see them.
 *
 * @author Joltra
 * @version 2.0.0
 * @since 1.0.0
 **/
public final class StaminaStateChangeListener implements Consumer<StaminaStateChangeEvent> {

    /**
     * Re-evaluates the viewable rule of the player whose stamina state just changed.
     *
     * @param event the stamina state change event
     */
    @Override
    public void accept(StaminaStateChangeEvent event) {
        VisibilityRules.refresh(event.getPlayer());
    }
}
