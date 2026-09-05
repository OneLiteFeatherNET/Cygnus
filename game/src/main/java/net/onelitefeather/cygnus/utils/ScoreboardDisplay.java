package net.onelitefeather.cygnus.utils;

import net.kyori.adventure.key.Key;
import net.minestom.server.color.TeamColor;
import net.onelitefeather.cygnus.component.TeamNameComponent;
import net.theevilreaper.xerus.api.ColorData;
import net.theevilreaper.xerus.api.component.team.ColorComponent;
import net.theevilreaper.xerus.api.team.Team;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.TeamsPacket;
import net.minestom.server.scoreboard.TeamBuilder;
import net.minestom.server.scoreboard.TeamManager;
import net.onelitefeather.cygnus.common.config.GameConfig;

import java.util.List;

/**
 * This is a utility class to manage the {@link net.minestom.server.scoreboard.Team} which are visible for the players.
 * The class is used to create the teams and add them to a team or remove them.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
public final class ScoreboardDisplay {

    /**
     * Creates a new instance of the {@link ScoreboardDisplay} and creates the teams for the given list.
     *
     * @param teams the list of teams to create
     */
    public ScoreboardDisplay(List<Team> teams) {
        TeamManager teamManager = MinecraftServer.getTeamManager();

        for (Team team : teams) {
            if (!team.has(TeamNameComponent.class)) continue;
            String teamName = team.get(TeamNameComponent.class).teamName();
            ColorData colorData = team.has(ColorComponent.class) ? team.get(ColorComponent.class).colorData() : ColorData.WHITE;

            TeamsPacket.NameTagVisibility visibility = GameConfig.SURVIVOR_TEAM_NAME.equals(teamName)
                    ? TeamsPacket.NameTagVisibility.HIDE_FOR_OTHER_TEAMS
                    : TeamsPacket.NameTagVisibility.NEVER;

            TeamBuilder sbTeamBuilder = teamManager
                    .createBuilder(teamName)
                    .nameTagVisibility(visibility)
                    .collisionRule(TeamsPacket.CollisionRule.NEVER)
                    // temp fix
                    .teamColor(TeamColor.fromName(colorData.name()));

            sbTeamBuilder.build();
        }
    }

    /**
     * Add a player to a team
     *
     * @param player  the player to add
     * @param teamKey the key of the team to add the player to
     */
    public void addPlayer(Player player, Key teamKey) {
        var teamName = getTeamName(teamKey);
        if (teamName == null) return;
        var team = MinecraftServer.getTeamManager().getTeam(teamName);
        if (team != null) {
            team.addMember(player.getUsername());
        }
    }

    /**
     * Remove a player from a team
     *
     * @param player  the player to remove
     * @param teamKey the key of the team to remove the player from
     */
    public void removePlayer(Player player, Key teamKey) {
        var teamName = getTeamName(teamKey);
        if (teamName == null) return;
        var team = MinecraftServer.getTeamManager().getTeam(teamName);
        if (team != null) {
            team.removeMember(player.getUsername());
        }
    }

    /**
     * Remove a player from all scoreboard teams.
     *
     * @param player the player to remove
     */
    public void removePlayer(Player player) {
        for (net.minestom.server.scoreboard.Team team : MinecraftServer.getTeamManager().getTeams()) {
            if (team.getMembers().contains(player.getUsername())) {
                team.removeMember(player.getUsername());
            }
        }
    }

    /**
     * Clears all members from all registered scoreboard teams.
     */
    public void clear() {
        for (net.minestom.server.scoreboard.Team team : MinecraftServer.getTeamManager().getTeams()) {
            for (String member : List.copyOf(team.getMembers())) {
                team.removeMember(member);
            }
        }
    }

    /**
     * Get the team name by the team key
     *
     * @param teamKey the team key
     * @return the team name, or {@code null} if unknown
     */
    private String getTeamName(Key teamKey) {
        if (GameConfig.SLENDER_KEY.equals(teamKey)) {
            return GameConfig.SLENDER_TEAM_NAME;
        } else if (GameConfig.SURVIVOR_KEY.equals(teamKey)) {
            return GameConfig.SURVIVOR_TEAM_NAME;
        } else if (GameConfig.SPECTATOR_KEY.equals(teamKey)) {
            return GameConfig.SPECTATOR_TEAM_NAME;
        }
        return null;
    }
}
