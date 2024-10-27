package net.onelitefeather.cygnus.utils;

import de.icevizion.xerus.api.team.Team;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.TeamsPacket;
import net.minestom.server.scoreboard.TeamBuilder;
import net.minestom.server.scoreboard.TeamManager;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.common.util.Helper;
import org.jetbrains.annotations.NotNull;

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
    public ScoreboardDisplay(@NotNull List<Team> teams) {
        TeamManager teamManager = MinecraftServer.getTeamManager();

        for (Team team : teams) {
            String teamName = PlainTextComponentSerializer.plainText().serialize(team.getName());

            TeamBuilder sbTeamBuilder = teamManager
                    .createBuilder(teamName)
                    .nameTagVisibility(TeamsPacket.NameTagVisibility.NEVER)
                    .collisionRule(TeamsPacket.CollisionRule.NEVER)
                    .teamColor(team.getColorData().getChatColor());

            sbTeamBuilder.build();
        }
    }

    /**
     * Add a player to a team
     *
     * @param player the player to add
     * @param teamId the team id to add the player to
     */
    public void addPlayer(@NotNull Player player, byte teamId) {
        var teamName = getTeamName(teamId);
        var team = MinecraftServer.getTeamManager().getTeam(teamName);
        team.addMember(player.getUsername());
    }

    /**
     * Remove a player from a team
     *
     * @param player the player to remove
     * @param teamId the team id to remove the player from
     */
    public void removePlayer(@NotNull Player player, byte teamId) {
        var teamName = getTeamName(teamId);
        var team = MinecraftServer.getTeamManager().getTeam(teamName);
        team.removeMember(player.getUsername());
    }

    /**
     * Get the team name by the team id
     *
     * @param teamId the team id
     * @return the team name
     */
    private String getTeamName(byte teamId) {
        return teamId == Helper.SLENDER_ID ? GameConfig.SLENDER_TEAM_NAME : GameConfig.SURVIVOR_TEAM_NAME;
    }
}
