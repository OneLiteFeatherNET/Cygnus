package net.onelitefeather.cygnus.disclaimer;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.minestom.server.dialog.DialogAction;
import net.minestom.server.dialog.DialogAfterAction;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerCustomClickEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.sound.SoundEvent;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.pica.dialog.DialogTemplate;
import net.onelitefeather.pica.dialog.type.DialogType;

import java.time.Duration;

/**
 * Warns every joining player about the flashing and flickering the game uses, before they can run
 * into it.
 *
 * <p>The warning arrives as a notice dialog: it states what the game does to the screen and carries
 * a single button to take note of it. Nothing is asked of the player and nothing is withheld from
 * them - a player who would rather not read it presses the button and plays. That is the point of a
 * notice rather than a confirmation: there is no second answer for them to give.</p>
 *
 * <p>Pressing the button plays the warning back as a title, so it is also on screen for a moment
 * after the dialog is gone, along with a sound to draw attention to it.</p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.11.0
 */
public final class EpilepsyDisclaimer {

    /**
     * The id the dialog button sends back. The listener below waits for exactly this key, so the two
     * have to be read together: a button carrying a different id leaves the title unsent.
     */
    static final Key ACKNOWLEDGE_KEY = Key.key("cygnus", "disclaimer/epilepsy/acknowledged");

    private static final Key DIALOG_KEY = Key.key("cygnus", "dialog/epilepsy_disclaimer");

    private static final Component DIALOG_TITLE = Messages.withMini("<red><bold>Photosensitivity warning");
    private static final Component DIALOG_BODY = Messages.withMini(
            "<gray>This game flashes, flickers and distorts the screen. The effects grow stronger the "
                    + "closer <color:#5A5A5A>Slenderman</color> <gray>gets, and they are at their strongest "
                    + "when he catches you.");
    private static final Component DIALOG_ADVICE = Messages.withMini(
            "<gray>If you are sensitive to flashing images, please do not play - and stop playing if you "
                    + "start to feel unwell.");
    private static final Component BUTTON_LABEL = Messages.withMini("<green>I understand");
    private static final Component BUTTON_TOOLTIP = Messages.withMini("<gray>Takes note of the warning and starts the round.");

    private static final Component TITLE = Messages.withMini("<red><bold>⚠ Flashing lights");
    private static final Component SUBTITLE = Messages.withMini("<gray>Stop playing if you start to feel unwell.");
    private static final Title REMINDER = Title.title(TITLE, SUBTITLE, Title.Times.times(
            Duration.ofMillis(500), Duration.ofSeconds(4), Duration.ofSeconds(1)));

    private static final Sound REMINDER_SOUND =
            Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_PLING, Sound.Source.MASTER, 0.8F, 0.7F);

    private final DialogTemplate dialog;

    /**
     * Creates the disclaimer and builds the dialog it shows.
     */
    public EpilepsyDisclaimer() {
        this.dialog = buildDialog();
    }

    private static DialogTemplate buildDialog() {
        return DialogType.notice(DIALOG_KEY)
                .meta(meta -> meta
                        .title(DIALOG_TITLE)
                        // A warning the player can dismiss with a keypress before reading it is not a
                        // warning, so escape stays off and the button is the only way out.
                        .closeWithEscape(false)
                        .afterAction(DialogAfterAction.CLOSE)
                        .messageBody(body -> body.contents(DIALOG_BODY))
                        .emptyMessage()
                        .messageBody(body -> body.contents(DIALOG_ADVICE)))
                .actionButton(button -> button
                        .label(BUTTON_LABEL)
                        .tooltip(BUTTON_TOOLTIP)
                        .action(new DialogAction.Custom(ACKNOWLEDGE_KEY, null)))
                .build();
    }

    /**
     * Registers the two halves of the disclaimer: showing it on join, and reacting to its button.
     *
     * @param node the event node to register the listeners on
     */
    public void registerListener(EventNode<Event> node) {
        node.addListener(PlayerSpawnEvent.class, event -> {
            if (!event.isFirstSpawn()) return;
            showTo(event.getPlayer());
        });
        node.addListener(PlayerCustomClickEvent.class, this::handleClick);
    }

    /**
     * Opens the warning for the given player.
     *
     * @param player the player to warn
     */
    public void showTo(Player player) {
        this.dialog.open(player);
    }

    /**
     * Plays the warning back as a title once the player pressed the dialog's button. Clicks carrying
     * any other id belong to another dialog and are left alone.
     *
     * @param event the click the client sent back
     */
    void handleClick(PlayerCustomClickEvent event) {
        if (!ACKNOWLEDGE_KEY.equals(event.getKey())) return;
        Player player = event.getPlayer();
        player.showTitle(REMINDER);
        player.playSound(REMINDER_SOUND);
    }
}
