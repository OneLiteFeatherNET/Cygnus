package net.onelitefeather.cygnus.utils;

import de.icevizion.xerus.api.team.Team;
import java.util.List;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.TeamsPacket;
import net.onelitefeather.cygnus.config.GameConfig;
import org.jetbrains.annotations.NotNull;

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

    public void addPlayer(@NotNull Player player, byte teamId) {
        var teamName = teamId == (byte) 0 ? GameConfig.SLENDER_TEAM_NAME : GameConfig.SURVIVOR_TEAM_NAME;
        var team = MinecraftServer.getTeamManager().getTeam(teamName);
        team.addMember(player.getUsername());
    }

    public void removePlayer(@NotNull Player player, byte teamId) {
        var teamName = teamId == (byte) 0 ? GameConfig.SLENDER_TEAM_NAME : GameConfig.SURVIVOR_TEAM_NAME;
        var team = MinecraftServer.getTeamManager().getTeam(teamName);
        team.removeMember(player.getUsername());
    }
}
