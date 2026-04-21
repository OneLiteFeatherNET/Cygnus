package net.onelitefeather.cygnus.listener;

import net.theevilreaper.aves.util.Broadcaster;
import net.theevilreaper.aves.util.functional.PlayerConsumer;
import net.theevilreaper.xerus.api.phase.Phase;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.phase.LobbyPhase;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 **/
public final class PlayerSpawnListener implements Consumer<PlayerSpawnEvent> {

    private final PlayerConsumer spawnSupplier;
    private final Supplier<Phase> phaseSupplier;

    public PlayerSpawnListener(PlayerConsumer spawnSupplier, @NotNull Supplier<Phase> phaseSupplier) {
        this.spawnSupplier = spawnSupplier;
        this.phaseSupplier = phaseSupplier;
    }

    @Override
    public void accept(PlayerSpawnEvent event) {
        Player player = event.getPlayer();
        player.setDisplayName(Component.text(player.getUsername()));

        if (phaseSupplier.get() instanceof LobbyPhase lobbyPhase) {
            Broadcaster.broadcast(Messages.getJoinMessage(player));
            player.setDisplayName(Component.text(player.getUsername()));
            this.spawnSupplier.accept(player);
            lobbyPhase.setLevel(player);
            lobbyPhase.checkStartCondition();
            return;
        }

        var tag = player.getTag(Tags.TEAM_ID);

        if (tag == null) {
            this.spawnSupplier.accept(player);
        }
    }
}
