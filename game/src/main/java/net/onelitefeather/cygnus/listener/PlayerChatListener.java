package net.onelitefeather.cygnus.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.event.player.PlayerChatEvent;
import net.onelitefeather.cygnus.team.TeamHelper;
import net.theevilreaper.xerus.api.team.Team;

import java.util.function.Consumer;

/**
 * Formats every chat message and enforces the spectator chat isolation.
 * <p>
 * The visibility matrix allows a spectator to read survivor and slender chat, but a message written
 * by a spectator must never reach a survivor or the slender. That rule depends on the sender's team
 * only and is deliberately <b>not</b> tied to the currently active phase:
 * <ul>
 *     <li>a phase check is fail-open, because the phase series reports {@code null} while no phase
 *     is running and {@code null instanceof GamePhase} evaluates to {@code false}, which would leak
 *     spectator chat to everyone;</li>
 *     <li>the {@code RestartPhase} that runs after the game phase finished is not a
 *     {@code GamePhase} either, so the isolation would silently disappear for its whole runtime.</li>
 * </ul>
 * Outside a running match nobody carries the spectator team tag, so the team based check is a no-op
 * there and no additional phase guard is needed.
 *
 * @author TheMeinerLP
 * @version 2.0.0
 * @since 1.0.0
 **/
public final class PlayerChatListener implements Consumer<PlayerChatEvent> {

    private static final Component MESSAGE_PREFIX = Component.text("≫", NamedTextColor.YELLOW);

    private final Team spectatorTeam;

    /**
     * Creates a new instance of the {@link PlayerChatListener}.
     *
     * @param spectatorTeam the team which receives the messages written by a spectator
     */
    public PlayerChatListener(Team spectatorTeam) {
        this.spectatorTeam = spectatorTeam;
    }

    @Override
    public void accept(PlayerChatEvent event) {
        //TODO: Improve chat during each phase
        event.setFormattedMessage(this.setLobbyLayout(event));

        if (!TeamHelper.isSpectatorTeam(event.getPlayer())) return;

        // Minestom pre-fills the recipients with every online player. Dropping all of them and
        // re-delivering to the spectator team keeps the message inside the spectator group even if
        // the team and the player tag ever drift apart, because the fallback is "nobody" and never
        // "everybody".
        event.getRecipients().clear();
        this.spectatorTeam.sendMessage(event.getFormattedMessage());
    }

    private Component setLobbyLayout(PlayerChatEvent event) {
        return event.getPlayer().getDisplayName()
                .append(Component.space())
                .append(MESSAGE_PREFIX)
                .append(Component.space())
                .append(Component.text(event.getRawMessage(), NamedTextColor.GRAY)
                );
    }
}
