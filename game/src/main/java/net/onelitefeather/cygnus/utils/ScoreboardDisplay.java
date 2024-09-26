package net.onelitefeather.cygnus.utils;

import de.icevizion.xerus.api.team.Team;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.TeamsPacket;
import net.onelitefeather.cygnus.common.config.GameConfig;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class ScoreboardDisplay {

    public ScoreboardDisplay(@NotNull List<Team> teams) {
        var serverTeamManager = MinecraftServer.getTeamManager();

        for (int i = 0; i < teams.size(); i++) {
            var team = teams.get(i);
            final String teamName = PlainTextComponentSerializer.plainText().serialize(team.getName());
            serverTeamManager.createBuilder(teamName)
                    .nameTagVisibility(TeamsPacket.NameTagVisibility.HIDE_FOR_OTHER_TEAMS)
                    .teamColor(team.getColorData().getChatColor())
                    .build();
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
        return teamId == (byte) 0 ? GameConfig.SLENDER_TEAM_NAME : GameConfig.SURVIVOR_TEAM_NAME;
    }
}
