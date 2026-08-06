package net.onelitefeather.cygnus.spectator;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.player.event.SpectatorAddEvent;
import net.onelitefeather.cygnus.player.listener.SpectatorAddListener;
import net.onelitefeather.cygnus.player.listener.SpectatorItemListener;
import net.onelitefeather.cygnus.team.TeamHelper;
import net.onelitefeather.cygnus.utils.Items;
import net.theevilreaper.xerus.api.team.Team;

import java.util.concurrent.CompletableFuture;

/**
 * Encapsulates all spectator-related behavior: gamemode/team/visibility changes on death,
 * the spectate-overview GUI, and leaving spectator mode.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.7.0
 */
public final class SpectatorService {

    private static final Component LEAVE_MESSAGE = Component.text("Thanks for playing it. <3", NamedTextColor.RED);

    private final Team spectatorTeam;
    private final SpectatorInventory spectatorInventory;

    public SpectatorService(Team spectatorTeam, Team survivorTeam) {
        this.spectatorTeam = spectatorTeam;
        this.spectatorInventory = new SpectatorInventory(survivorTeam, this::teleportTo);
    }

    /**
     * Registers some spectator listener into a given {@link EventNode<Event>} reference.
     *
     * @param node to register the listeners
     */
    public void registerListener(EventNode<Event> node) {
        node.addListener(SpectatorAddEvent.class, new SpectatorAddListener(this));
        node.addListener(PlayerUseItemEvent.class, new SpectatorItemListener(this));
    }

    /**
     * Converts the given player into a spectator and update several things like hotbar, {@link GameMode} and so on.
     *
     * @param player the player to convert
     */
    public void join(Player player) {
        player.setGameMode(GameMode.SPECTATOR);
        player.setTag(Tags.TEAM_KEY, net.onelitefeather.cygnus.common.config.GameConfig.SPECTATOR_KEY);
        spectatorTeam.addPlayer(player);
        Items.setSpectatorLayout(player);
        player.updateViewableRule(_ -> false);
    }

    /**
     * Removes the player from the match. Placeholder: kicks unconditionally.
     * A follow-up design will route this through CloudNet to a Lobby service when available.
     *
     * @param player the leaving spectator
     */
    public void leave(Player player) {
        player.kick(LEAVE_MESSAGE);
    }

    /**
     * Opens the spectate-overview GUI for the given player.
     *
     * @param player the spectator to show the overview to
     */
    public void openOverview(Player player) {
        spectatorInventory.open(player);
    }

    /**
     * Performs a one-time teleport of the spectator to the target's current position.
     *
     * @param spectator the spectating player
     * @param target    the player to teleport to
     * @return the teleport completion future
     */
    public CompletableFuture<Void> teleportTo(Player spectator, Player target) {
        return spectator.teleport(target.getPosition());
    }

    /**
     * Checks whether the given player is currently a spectator.
     *
     * @param player the player to check
     * @return true if the player is in the spectator team
     */
    public boolean isSpectator(Player player) {
        return TeamHelper.isSpectatorTeam(player);
    }
}
