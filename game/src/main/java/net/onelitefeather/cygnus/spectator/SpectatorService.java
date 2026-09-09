package net.onelitefeather.cygnus.spectator;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import net.onelitefeather.cygnus.player.event.SpectatorAddEvent;
import net.onelitefeather.cygnus.player.listener.SpectatorAddListener;
import net.onelitefeather.cygnus.player.listener.SpectatorItemListener;
import net.onelitefeather.cygnus.team.TeamHelper;
import net.onelitefeather.cygnus.utils.Items;
import net.onelitefeather.cygnus.visibility.VisibilityRules;
import net.theevilreaper.xerus.api.team.Team;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Encapsulates all spectator-related behavior: gamemode/team/visibility changes on death,
 * the spectate-overview GUI, and leaving spectator mode.
 *
 * @author theEvilReaper
 * @version 1.2.0
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
     * <p>
     * The spectator rule keeps the player invisible for everybody who is still in the round but lets
     * spectators see each other. Because {@link VisibilityRules#spectatorRule()} tests the viewer, the rules
     * of all other players have to be re-evaluated as well so already present spectators pull the new one
     * into their viewer set.
     * <p>
     * {@link Player#setAllowFlying(boolean)} has to be set next to {@link Player#setFlying(boolean)}:
     * {@link Player#setGameMode(GameMode)} resets the flight permission to the one of the mode, and
     * {@code SURVIVAL} does not allow flying. The abilities packet then only carries the flying flag, so the
     * client drops flight the moment the spectator touches the ground and refuses to take off again.
     *
     * @param player the player to convert
     */
    public void join(Player player) {
        player.setGameMode(GameMode.SURVIVAL);
        player.setAllowFlying(true);
        player.setFlying(true);
        player.setTag(Tags.TEAM_KEY, GameConfig.SPECTATOR_KEY);
        spectatorTeam.addPlayer(player);
        clearStaminaHud(player);
        Items.setSpectatorLayout(player);
        markAsSpectator(player);
        player.updateViewableRule(VisibilityRules.spectatorRule());
        VisibilityRules.refresh(player);
    }

    /**
     * Strikes the name of the given player through in the tab list.
     * <p>
     * A spectator keeps the display name they carried into the round, so the tab list still shows them in
     * the green of a living survivor. The strike through plus the gray of the spectator team marks them as
     * out of the round at a glance, which is the only signal the tab list can give: a spectator is invisible
     * for everybody still playing, so their entry is the only place they show up at all.
     *
     * @param player the player who just became a spectator
     */
    private static void markAsSpectator(Player player) {
        player.setDisplayName(Component.text(player.getUsername(), NamedTextColor.GRAY, TextDecoration.STRIKETHROUGH));
    }

    /**
     * Drops the stamina display a survivor carries into spectator mode.
     * <p>
     * The stamina bar is drawn on the experience bar and can leave the player sprint blocked, both of which
     * survive the switch to spectator mode. The bar itself stops ticking because the player is taken out of
     * the {@link net.onelitefeather.cygnus.stamina.StaminaService} when they leave the round, so nothing
     * writes these values again afterwards.
     *
     * @param player the player who just became a spectator
     */
    private static void clearStaminaHud(Player player) {
        player.setExp(0.0f);
        if (player instanceof CygnusPlayer cygnusPlayer) {
            cygnusPlayer.setBlockedSprinting(false);
        }
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

    /**
     * Invalidates the spectator inventory's data layout.
     * <p>
     * {@link net.theevilreaper.aves.inventory.GlobalInventoryBuilder#invalidateLayout()} is called
     * as well: while the overview is already open, its {@code applyDataLayout()} only ever writes
     * non-air stacks into the live inventory, so a survivor removed from the data layout (e.g. one
     * who just died) would otherwise keep their stale entry forever. {@code invalidateLayout()}
     * clears the whole inventory before the data layout is reapplied, which removes it correctly.
     * </p>
     */
    public void updateInventory() {
        LoggerFactory.getLogger(SpectatorService.class).info("Updating inventory");
        this.spectatorInventory.invalidateDataLayout();
        this.spectatorInventory.invalidateLayout();
    }
}
