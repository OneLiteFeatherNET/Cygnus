package net.onelitefeather.cygnus.hud;

import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import net.theevilreaper.xerus.api.Joinable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public abstract class GlobalHudComponent implements HudComponent, Joinable {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalHudComponent.class);

    protected final Set<CygnusPlayer> players;
    protected boolean visible = true;

    protected GlobalHudComponent() {
        this.players = new HashSet<>();
    }

    protected GlobalHudComponent(Set<CygnusPlayer> players) {
        this.players = new HashSet<>(players);
    }

    @Override
    public void addPlayer(Player player, @Nullable Consumer<Player> consumer) {
        if (player instanceof CygnusPlayer cygnusPlayer) {
            players.add(cygnusPlayer);
            if (consumer != null) consumer.accept(player);
        } else {
            LOGGER.debug("Ignored addPlayer for non-CygnusPlayer {}", player.getUsername());
        }
    }

    @Override
    public void addPlayers(Collection<Player> players, @Nullable Consumer<Player> consumer) {
        for (Player p : players) {
            addPlayer(p, consumer);
        }
    }

    @Override
    public void removePlayer(Player player, @Nullable Consumer<Player> consumer) {
        if (player instanceof CygnusPlayer cygnusPlayer) {
            players.remove(cygnusPlayer);
            if (consumer != null) consumer.accept(player);
        } else {
            LOGGER.debug("Ignored removePlayer for non-CygnusPlayer {}", player.getUsername());
        }
    }

    @Override
    public void removePlayers(Collection<Player> players, @Nullable Consumer<Player> consumer) {
        for (Player p : players) {
            removePlayer(p, consumer);
        }
    }

    @Contract(pure = true)
    public Set<CygnusPlayer> getPlayers() {
        return Collections.unmodifiableSet(players);
    }

    @Override
    public boolean isVisible() {
        return visible;
    }
}
