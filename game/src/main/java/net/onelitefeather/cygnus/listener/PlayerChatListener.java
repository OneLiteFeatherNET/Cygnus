package net.onelitefeather.cygnus.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerChatEvent;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.rank.RankTag;
import net.onelitefeather.cygnus.phase.GamePhase;
import net.onelitefeather.cygnus.team.TeamHelper;
import net.theevilreaper.xerus.api.phase.Phase;
import net.theevilreaper.xerus.api.team.Team;

import java.util.function.Consumer;
import java.util.function.Supplier;

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
 * @author theEvilReaper
 * @version 2.2.0
 * @since 1.0.0
 **/
public final class PlayerChatListener implements Consumer<PlayerChatEvent> {

    private static final Component MESSAGE_PREFIX = Component.text("≫", NamedTextColor.YELLOW);

    private final Team spectatorTeam;
    private final Supplier<Phase> phaseSupplier;

    /**
     * Creates a new instance of the {@link PlayerChatListener}.
     *
     * @param spectatorTeam the team which receives the messages written by a spectator
     * @param phaseSupplier supplier providing the currently active phase
     */
    public PlayerChatListener(Team spectatorTeam, Supplier<Phase> phaseSupplier) {
        this.spectatorTeam = spectatorTeam;
        this.phaseSupplier = phaseSupplier;
    }

    /**
     * Creates a new instance of the {@link PlayerChatListener} without phase supplier.
     *
     * @param spectatorTeam the team which receives the messages written by a spectator
     */
    public PlayerChatListener(Team spectatorTeam) {
        this(spectatorTeam, () -> null);
    }

    @Override
    public void accept(PlayerChatEvent event) {
        Phase phase = this.phaseSupplier.get();
        if (phase instanceof GamePhase) {
            event.setFormattedMessage(this.setGameLayout(event));
        } else {
            event.setFormattedMessage(this.setLobbyLayout(event));
        }

        if (!TeamHelper.isSpectatorTeam(event.getPlayer())) return;

        // Minestom pre-fills the recipients with every online player. Dropping all of them and
        // re-delivering to the spectator team keeps the message inside the spectator group even if
        // the team and the player tag ever drift apart, because the fallback is "nobody" and never
        // "everybody".
        event.getRecipients().clear();
        this.spectatorTeam.sendMessage(event.getFormattedMessage());
    }

    /**
     * Builds the chat line during the lobby and restart phases.
     *
     * @param event the chat event to format
     * @return the formatted chat line
     */
    private Component setLobbyLayout(PlayerChatEvent event) {
        Player player = event.getPlayer();
        Component displayName = player.getDisplayName() != null
                ? player.getDisplayName()
                : Component.text(player.getUsername());

        return Component.empty()
                .append(displayName)
                .append(Component.space())
                .append(MESSAGE_PREFIX)
                .append(Component.space())
                .append(Component.text(event.getRawMessage(), NamedTextColor.GRAY));
    }

    /**
     * Builds the chat line during active gameplay.
     *
     * @param event the chat event to format
     * @return the formatted chat line
     */
    private Component setGameLayout(PlayerChatEvent event) {
        Player player = event.getPlayer();

        if (TeamHelper.isSpectatorTeam(player)) {
            RankTag tag = player.getTag(Tags.ACTIVE_RANK_TAG);
            Component tagComponent = tag != null ? tag.asComponent().append(Component.space()) : Component.empty();

            return Component.empty()
                    //TODO: Replace the [SPEC prefix in a later spec
                    .append(Component.text("[SPEC] ", NamedTextColor.DARK_GRAY))
                    .append(tagComponent)
                    .append(Component.text(player.getUsername(), NamedTextColor.GRAY))
                    .append(Component.space())
                    .append(MESSAGE_PREFIX)
                    .append(Component.space())
                    .append(Component.text(event.getRawMessage(), NamedTextColor.DARK_GRAY));
        }

        Component displayName = player.getDisplayName() != null
                ? player.getDisplayName()
                : Component.text(player.getUsername(), NamedTextColor.GREEN);

        return Component.empty()
                .append(displayName)
                .append(Component.space())
                .append(MESSAGE_PREFIX)
                .append(Component.space())
                .append(Component.text(event.getRawMessage(), NamedTextColor.GRAY));
    }
}
