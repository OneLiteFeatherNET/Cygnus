package net.onelitefeather.cygnus.team;

import net.onelitefeather.cygnus.utils.ViewRuleUpdater;
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

import java.util.HashSet;
import java.util.Set;

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
     * Result of a team allocation, containing the chosen slender player and the resulting survivors.
     *
     * @param slender   the player who is the slender
     * @param survivors the players who are survivors
     */
    public record TeamAllocation(Player slender, Set<Player> survivors) {
    }

    /**
     * Prepares which player should be the slender and which players should be the survivors.
     *
     * @param slenderTeam  the team for the slender
     * @param survivorTeam the team for the survivors
     * @return the resulting allocation, containing the slender player and the survivors
     */
    public static TeamAllocation prepareTeamAllocation(Team slenderTeam, Team survivorTeam) {
        Check.argCondition(slenderTeam.getCapacity() != 1, "The slender team must have a capacity from one");

        Player slenderPlayer = selectSlenderPlayer();
        assignSlender(slenderPlayer, slenderTeam);

        Set<Player> survivors = collectSurvivors(slenderPlayer);
        assignSurvivors(survivors, survivorTeam);

        return new TeamAllocation(slenderPlayer, survivors);
    }

    /**
     * Picks a random online player to become the slender.
     *
     * @return the chosen player
     */
    private static Player selectSlenderPlayer() {
        return Players.getRandomPlayer()
                .orElseThrow(() -> new IllegalStateException("No player found to be the slender"));
    }

    /**
     * Tags and assigns the given player as the slender and updates their view rule.
     *
     * @param player      the player to become the slender
     * @param slenderTeam the team to add the player to
     */
    private static void assignSlender(Player player, Team slenderTeam) {
        player.setTag(Tags.TEAM_ID, SLENDER_ID);
        player.updateViewableRule(ViewRuleUpdater::viewableRuleForSlender);
        slenderTeam.addPlayer(player);
    }

    /**
     * Collects all online players except the slender in a single pass.
     *
     * @param slenderPlayer the player to exclude
     * @return the set of survivor candidates
     */
    private static Set<Player> collectSurvivors(Player slenderPlayer) {
        Set<Player> survivors = new HashSet<>();
        for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            if (!player.getUuid().equals(slenderPlayer.getUuid())) {
                survivors.add(player);
            }
        }
        return survivors;
    }

    /**
     * Tags the given players as survivors and adds them to the survivor team.
     *
     * @param survivors    the players to assign
     * @param survivorTeam the team to add them to
     */
    private static void assignSurvivors(Set<Player> survivors, Team survivorTeam) {
        survivors.forEach(player -> player.setTag(Tags.TEAM_ID, SURVIVOR_ID));
        survivorTeam.addPlayers(survivors);
    }

    /**
     * Teleport the teams to their spawn points
     *
     * @param teamService  the service to get the teams
     * @param gameMap      the map to get the spawn points
     * @param gameInstance the instance to teleport the players
     */
    public static void teleportTeams(TeamService teamService, GameMap gameMap, Instance gameInstance) {
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
    public static void updateTabList(TeamService teamService) {
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
    public static boolean isSlenderTeam(Player player) {
        return player.getTag(Tags.TEAM_ID) == SLENDER_ID;
    }

    /**
     * Check if the player is in the survivor team
     *
     * @param player the player to check
     * @return true if the player is in the survivor team
     */
    public static boolean isSurvivorTeam(Player player) {
        return player.getTag(Tags.TEAM_ID) == SURVIVOR_ID;
    }

    /**
     * Update the instance of the player
     *
     * @param player   the player to update
     * @param instance the new instance
     * @param position the new position
     */
    private static void updateInstance(Player player, Instance instance, Pos position) {
        player.setInstance(instance, position);
    }

    private TeamHelper() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}