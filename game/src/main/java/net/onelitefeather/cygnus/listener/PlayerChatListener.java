package net.onelitefeather.cygnus.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.event.player.PlayerChatEvent;
import net.onelitefeather.cygnus.phase.GamePhase;
import net.onelitefeather.cygnus.team.TeamHelper;
import net.theevilreaper.xerus.api.phase.Phase;
import net.theevilreaper.xerus.api.team.Team;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class PlayerChatListener implements Consumer<PlayerChatEvent> {

    private static final Component MESSAGE_PREFIX = Component.text("≫", NamedTextColor.YELLOW);

    private final Team spectatorTeam;
    private final Supplier<Phase> phaseSupplier;

    public PlayerChatListener(Team spectatorTeam, Supplier<Phase> phaseSupplier) {
        this.spectatorTeam = spectatorTeam;
        this.phaseSupplier = phaseSupplier;
    }

    @Override
    public void accept(PlayerChatEvent event) {
        //TODO: Improve chat during each phase
        event.setFormattedMessage(this.setLobbyLayout(event));

        if (phaseSupplier.get() instanceof GamePhase && TeamHelper.isSpectatorTeam(event.getPlayer())) {
            event.getRecipients().clear();
            spectatorTeam.sendMessage(event.getFormattedMessage());
        }
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
