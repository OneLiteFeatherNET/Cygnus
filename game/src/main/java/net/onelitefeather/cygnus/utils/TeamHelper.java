package net.onelitefeather.cygnus.utils;

import net.theevilreaper.aves.util.Players;
import net.theevilreaper.xerus.api.team.Team;
import net.theevilreaper.xerus.api.team.TeamService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.utils.validate.Check;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.map.GameMap;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import static net.onelitefeather.cygnus.common.util.Helper.SLENDER_ID;
import static net.onelitefeather.cygnus.common.util.Helper.SURVIVOR_ID;

/**
 * This class provides utility methods for the team handling in the game.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
public final class TeamHelper {

    /**
     * Prepares which player should be the slender and which players should be the survivors.
     *
     * @param slenderTeam  the team for the slender
     * @param survivorTeam the team for the survivors
     * @return the player who is the slender
     */
    public static @NotNull Player prepareTeamAllocation(@NotNull Team slenderTeam, @NotNull Team survivorTeam) {
        Check.argCondition(slenderTeam.getCapacity() != 1, "The slender team must have a capacity from one");
        Optional<Player> randomPlayer = Players.getRandomPlayer();
        if (randomPlayer.isEmpty()) {
            throw new IllegalStateException("No player found to be the slender");
        }

        Player slenderPlayer = randomPlayer.get();
        slenderPlayer.setTag(Tags.TEAM_ID, SLENDER_ID);
        slenderPlayer.updateViewableRule(ViewRuleUpdater::viewableRuleForSlender);

        slenderTeam.addPlayer(slenderPlayer);
        Set<@NotNull Player> onlinePlayers = new HashSet<>(MinecraftServer.getConnectionManager().getOnlinePlayers());
        onlinePlayers.remove(slenderPlayer);
        survivorTeam.addPlayers(onlinePlayers);

        onlinePlayers.forEach(player -> player.setTag(Tags.TEAM_ID, SURVIVOR_ID));

        return slenderPlayer;
    }

    /**
     * Teleport the teams to their spawn points
     *
     * @param teamService  the service to get the teams
     * @param gameMap      the map to get the spawn points
     * @param gameInstance the instance to teleport the players
     */
    public static void teleportTeams(@NotNull TeamService<Team> teamService, @NotNull GameMap gameMap, @NotNull Instance gameInstance) {
        Team slenderTeam = teamService.getTeams().getFirst();

        slenderTeam.getPlayers().forEach(player -> updateInstance(player, gameInstance, gameMap.getSlenderSpawn()));

        Team survivorTeam = teamService.getTeams().getLast();

        if (gameMap.getSurvivorSpawns().size() == 1) {
            Pos position = gameMap.getSurvivorSpawns().iterator().next();
            survivorTeam.getPlayers().forEach(player -> updateInstance(player, gameInstance, position));
        }

        //throw new UnsupportedOperationException("Multiple survivor spawns are not supported yet");
    }

    /**
     * Update the tab list for the teams.
     *
     * @param teamService the service to get the teams
     */
    public static void updateTabList(@NotNull TeamService<Team> teamService) {
        if (!teamService.hasTeams()) return;

        Team slenderTeam = teamService.getTeams().getFirst();
        Team survivorTeam = teamService.getTeams().getLast();

        if (slenderTeam.isEmpty()) {
            throw new IllegalStateException("The slender team must have at least one player");
        }

        slenderTeam.getPlayers().forEach(player -> {
            Component slenderDisplayName = Component.text("⛧ ", NamedTextColor.RED)
                    .append(Component.text(player.getUsername(), NamedTextColor.GRAY));
            player.setDisplayName(slenderDisplayName);
        });

        survivorTeam.getPlayers().forEach(player -> {
            Component survivorDisplayName = Component.text(player.getUsername(), NamedTextColor.GREEN);
            player.setDisplayName(survivorDisplayName);
        });
    }

    /**
     * Check if the player is in the slender team
     *
     * @param player the player to check
     * @return true if the player is in the slender team
     */
    public static boolean isSlenderTeam(@NotNull Player player) {
        return player.getTag(Tags.TEAM_ID) == SLENDER_ID;
    }

    /**
     * Check if the player is in the survivor team
     *
     * @param player the player to check
     * @return true if the player is in the survivor team
     */
    public static boolean isSurvivorTeam(@NotNull Player player) {
        return player.getTag(Tags.TEAM_ID) == SURVIVOR_ID;
    }

    /**
     * Update the instance of the player
     *
     * @param player   the player to update
     * @param instance the new instance
     * @param position the new position
     */
    private static void updateInstance(@NotNull Player player, @NotNull Instance instance, @NotNull Pos position) {
        player.setInstance(instance, position);
    }

    private TeamHelper() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}
