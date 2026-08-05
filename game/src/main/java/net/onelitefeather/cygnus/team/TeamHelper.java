package net.onelitefeather.cygnus.team;

import net.onelitefeather.cygnus.common.strategy.TeleportStrategy;
import net.onelitefeather.cygnus.utils.ViewRuleUpdater;
import net.theevilreaper.aves.util.Players;
import net.theevilreaper.xerus.api.team.Team;
import net.theevilreaper.xerus.api.team.TeamService;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.utils.validate.Check;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.common.map.GameMap;

import java.util.HashSet;
import java.util.Set;

/**
 * This class provides utility methods for the team handling in the game.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
public final class TeamHelper {

    /**
     * The ID representing the Slender team.
     */
    public static final byte SLENDER_TEAM_ID = 0;

    /**
     * The ID representing the Survivor team.
     */
    public static final byte SURVIVOR_TEAM_ID = 1;

    /**
     * The ID representing the Spectator team.
     */
    public static final byte SPECTATOR_TEAM_ID = 2;

    /**
     * Resolves the {@link Key} a numeric team ID tag value refers to.
     *
     * @param id the team ID (see {@link Tags#TEAM_ID})
     * @return the team's key
     * @throws IllegalArgumentException if the ID does not name a known team
     */
    public static Key keyForTeamId(byte id) {
        return switch (id) {
            case SLENDER_TEAM_ID -> GameConfig.SLENDER_KEY;
            case SURVIVOR_TEAM_ID -> GameConfig.SURVIVOR_KEY;
            case SPECTATOR_TEAM_ID -> GameConfig.SPECTATOR_KEY;
            default -> throw new IllegalArgumentException("Unknown team ID: " + id);
        };
    }

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
        player.setTag(Tags.TEAM_ID, SLENDER_TEAM_ID);
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
        survivors.forEach(player -> player.setTag(Tags.TEAM_ID, SURVIVOR_TEAM_ID));
        survivorTeam.addPlayers(survivors);
    }

    /**
     * Teleport the teams to their spawn points using the default round-robin random strategy.
     *
     * @param teamService  the service to get the teams
     * @param gameMap      the map to get the spawn points
     * @param gameInstance the instance to teleport the players
     */
    public static void teleportTeams(TeamService teamService, GameMap gameMap, Instance gameInstance) {
        TeleportStrategy strategy = gameMap.getSurvivorSpawns().size() == 1
                ? TeleportStrategy.SINGLE
                : TeleportStrategy.ROUND_ROBIN_RANDOM;
        teleportTeams(teamService, gameMap, gameInstance, strategy);
    }

    /**
     * Teleport the teams to their spawn points using the specified teleportation strategy.
     *
     * @param teamService  the service to get the teams
     * @param gameMap      the map to get the spawn points
     * @param gameInstance the instance to teleport the players
     * @param strategy     the strategy to use for survivor teleportation
     */
    public static void teleportTeams(TeamService teamService, GameMap gameMap, Instance gameInstance, TeleportStrategy strategy) {
        Team slenderTeam = teamService.getTeam(GameConfig.SLENDER_KEY)
                .orElseThrow(() -> new IllegalStateException("Slender team not found"));
        slenderTeam.getPlayers().forEach(player -> updateInstance(player, gameInstance, gameMap.getSlenderSpawn()));

        Team survivorTeam = teamService.getTeam(GameConfig.SURVIVOR_KEY)
                .orElseThrow(() -> new IllegalStateException("Survivor team not found"));
        strategy.teleport(survivorTeam.getPlayers(), gameInstance, gameMap.getSurvivorSpawns());
    }

    /**
     * Update the tab list for the teams.
     *
     * @param teamService the service to get the teams
     */
    public static void updateTabList(TeamService teamService) {
        if (!teamService.hasTeams()) return;

        Team slenderTeam = teamService.getTeam(GameConfig.SLENDER_KEY)
                .orElseThrow(() -> new IllegalStateException("Slender team not found"));
        Team survivorTeam = teamService.getTeam(GameConfig.SURVIVOR_KEY)
                .orElseThrow(() -> new IllegalStateException("Survivor team not found"));

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
        Byte teamId = player.getTag(Tags.TEAM_ID);
        return teamId != null && teamId == SLENDER_TEAM_ID;
    }

    /**
     * Check if the player is in the survivor team
     *
     * @param player the player to check
     * @return true if the player is in the survivor team
     */
    public static boolean isSurvivorTeam(Player player) {
        Byte teamId = player.getTag(Tags.TEAM_ID);
        return teamId != null && teamId == SURVIVOR_TEAM_ID;
    }

    /**
     * Check if the player is in the spectator team
     *
     * @param player the player to check
     * @return true if the player is in the spectator team
     */
    public static boolean isSpectatorTeam(Player player) {
        Byte teamId = player.getTag(Tags.TEAM_ID);
        return teamId != null && teamId == SPECTATOR_TEAM_ID;
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