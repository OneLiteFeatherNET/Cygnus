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
 * <p>The warning states what the game does to the screen and leaves the player two ways out. Taking
 * note of it plays the warning back as a title, so it stays on screen for a moment after the dialog
 * is gone, along with a sound to draw attention to it. Declining it disconnects the player, which
 * behind a proxy hands them back to the network lobby. That is what makes the choice a real one: a
 * warning whose only answer is "understood" asks a question it will not accept an answer to.</p>
 *
 * <p>Escape stays disabled, so the two buttons are the only ways out and neither is reached by
 * accident.</p>
 *
 * @author TheMeinerLP
 * @version 1.1.0
 * @since 2.11.0
 */
public final class EpilepsyDisclaimer {

    /**
     * The id the dialog button sends back. The listener below waits for exactly this key, so the two
     * have to be read together: a button carrying a different id leaves the title unsent.
     */
    static final Key ACKNOWLEDGE_KEY = Key.key("cygnus", "disclaimer/epilepsy/acknowledged");

    /**
     * The id the leave button sends back. Answering with it takes the player off the service instead
     * of into the round.
     */
    static final Key DECLINE_KEY = Key.key("cygnus", "disclaimer/epilepsy/declined");

    private static final Key DIALOG_KEY = Key.key("cygnus", "dialog/epilepsy_disclaimer");

    private static final Component DIALOG_TITLE = Messages.withMini("<red><bold>Photosensitivity warning");
    private static final Component DIALOG_BODY = Messages.withMini(
            "<white>This game flashes, flickers and distorts the screen. The effects grow stronger the "
                    + "closer <gray>Slenderman</gray> <white>gets, and they are at their strongest "
                    + "when he catches you.");
    private static final Component DIALOG_ADVICE = Messages.withMini(
            "<white>If you are sensitive to flashing images, please do not play. Stop playing if you "
                    + "start to feel unwell.");
    private static final Component ACCEPT_LABEL = Messages.withMini("<green>I understand");
    private static final Component ACCEPT_TOOLTIP = Messages.withMini("<white>Takes note of the warning and lets you play.");
    private static final Component DECLINE_LABEL = Messages.withMini("<red>Leave");
    private static final Component DECLINE_TOOLTIP = Messages.withMini("<white>Sends you back to the lobby without playing.");

    private static final Component LEAVE_MESSAGE = Messages.withMini("<white>You declined the photosensitivity warning.");

    private static final Component TITLE = Messages.withMini("<red><bold>⚠ Flashing lights");
    private static final Component SUBTITLE = Messages.withMini("<white>Stop playing if you start to feel unwell.");
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
        return DialogType.confirm(DIALOG_KEY)
                .meta(meta -> meta
                        .title(DIALOG_TITLE)
                        // A warning the player can dismiss with a keypress before reading it is not a
                        // warning, so escape stays off and the button is the only way out.
                        .closeWithEscape(false)
                        .afterAction(DialogAfterAction.CLOSE)
                        .messageBody(body -> body.contents(DIALOG_BODY))
                        .emptyMessage()
                        .messageBody(body -> body.contents(DIALOG_ADVICE)))
                .yesButton(button -> button
                        .label(ACCEPT_LABEL)
                        .tooltip(ACCEPT_TOOLTIP)
                        .action(new DialogAction.Custom(ACKNOWLEDGE_KEY, null)))
                .noButton(button -> button
                        .label(DECLINE_LABEL)
                        .tooltip(DECLINE_TOOLTIP)
                        .action(new DialogAction.Custom(DECLINE_KEY, null)))
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
     * Answers the button the player pressed. Taking note of the warning plays it back as a title;
     * declining it disconnects the player, which behind a proxy puts them back in the network lobby.
     * Clicks carrying any other id belong to another dialog and are left alone.
     *
     * @param event the click the client sent back
     */
    void handleClick(PlayerCustomClickEvent event) {
        Player player = event.getPlayer();
        if (ACKNOWLEDGE_KEY.equals(event.getKey())) {
            player.showTitle(REMINDER);
            player.playSound(REMINDER_SOUND);
            return;
        }
        if (DECLINE_KEY.equals(event.getKey())) {
            player.kick(LEAVE_MESSAGE);
        }
    }
}
