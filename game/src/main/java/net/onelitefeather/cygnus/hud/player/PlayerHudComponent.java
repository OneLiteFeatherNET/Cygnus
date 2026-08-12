package net.onelitefeather.cygnus.hud;

import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Consumer;

public class PlayerHudComponent implements HudComponent {

    protected final CygnusPlayer player;

    public PlayerHudComponent(CygnusPlayer player) {
        this.player = player;
    }

    @Override
    public void render() {

    }

    @Override
    public void addPlayer(Player player, @Nullable Consumer<Player> consumer) {

    }

    @Override
    public void addPlayers(Collection<Player> players, @Nullable Consumer<Player> consumer) {
        throw new UnsupportedOperationException("A player hud doesn't support addPlayers");
    }

    @Override
    public void removePlayer(Player player, @Nullable Consumer<Player> consumer) {

    }

    @Override
    public void removePlayers(Collection<Player> players, @Nullable Consumer<Player> consumer) {
        throw new UnsupportedOperationException("A player hud doesn't support removesPlayers");
    }
}
