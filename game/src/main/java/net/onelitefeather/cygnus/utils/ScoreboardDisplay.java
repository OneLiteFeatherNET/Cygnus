package net.onelitefeather.cygnus.utils;

import net.kyori.adventure.key.Key;
import net.minestom.server.color.TeamColor;
import net.onelitefeather.cygnus.component.TeamNameComponent;
import net.theevilreaper.xerus.api.ColorData;
import net.theevilreaper.xerus.api.component.team.ColorComponent;
import net.theevilreaper.xerus.api.team.Team;
import net.theevilreaper.xerus.api.team.TeamService;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.TeamsPacket;
import net.minestom.server.scoreboard.TeamBuilder;
import net.minestom.server.scoreboard.TeamManager;
import net.onelitefeather.cygnus.common.config.GameConfig;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This is a utility class to manage the {@link net.minestom.server.scoreboard.Team} which are visible for the players.
 * The class is used to create the teams and add them to a team or remove them.
 *
 * <p>The name tag above a player's head is a client-side decision, and the only lever a server has
 * over it is a scoreboard team. {@link TeamsPacket.NameTagVisibility#HIDE_FOR_OTHER_TEAMS} is what
 * makes the survivors' names unreadable for the slender while they stay readable among themselves -
 * without it, the slender can track a survivor through a wall by their floating name.</p>
 *
 * @author theEvilReaper
 * @version 1.1.0
 * @since 1.0.0
 */
public final class ScoreboardDisplay {

    /**
     * Creates a new instance of the {@link ScoreboardDisplay} and creates the teams for the given list.
     *
     * <p>A game team without a {@link TeamNameComponent} gets no scoreboard team: it has no name to
     * create one under. The spectator team is such a team, and deliberately so - see
     * {@link #sync(TeamService)} for where its players end up instead.</p>
     *
     * @param teams the list of teams to create
     */
    public ScoreboardDisplay(List<Team> teams) {
        TeamManager teamManager = MinecraftServer.getTeamManager();

        for (Team team : teams) {
            if (!team.has(TeamNameComponent.class)) continue;
            String teamName = team.get(TeamNameComponent.class).teamName();
            ColorData colorData = team.get(ColorComponent.class).colorData();

            TeamBuilder sbTeamBuilder = teamManager
                    .createBuilder(teamName)
                    // Not NEVER: the survivors are supposed to keep reading each other's names, only
                    // the slender must not.
                    .nameTagVisibility(TeamsPacket.NameTagVisibility.HIDE_FOR_OTHER_TEAMS)
                    .collisionRule(TeamsPacket.CollisionRule.NEVER)
                    // temp fix
                    .teamColor(TeamColor.fromName(colorData.name()));

            sbTeamBuilder.build();
        }
    }

    /**
     * Brings the scoreboard teams in line with the current game teams.
     *
     * <p>Membership is what makes {@code HIDE_FOR_OTHER_TEAMS} mean anything: a player in no team is
     * "another team" to everybody, so the roles have to be mirrored here or the whole mechanism is
     * inert.</p>
     *
     * <p>Spectators are put on the <b>survivor</b> scoreboard team rather than one of their own. That
     * is what keeps survivor names readable while spectating, and it leaks nothing in return: a
     * survivor never receives a spectator as an entity at all, so there is no name of theirs for
     * anyone still in the round to see.</p>
     *
     * @param teamService the service holding the current game teams
     */
    public void sync(TeamService teamService) {
        setMembers(GameConfig.SLENDER_TEAM_NAME, usernames(teamService, GameConfig.SLENDER_KEY));

        Set<String> survivorSide = usernames(teamService, GameConfig.SURVIVOR_KEY);
        survivorSide.addAll(usernames(teamService, GameConfig.SPECTATOR_KEY));
        setMembers(GameConfig.SURVIVOR_TEAM_NAME, survivorSide);
    }

    /**
     * Add a player to a team
     *
     * @param player  the player to add
     * @param teamKey the key of the team to add the player to
     */
    public void addPlayer(Player player, Key teamKey) {
        var team = findTeam(getTeamName(teamKey));
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
        var team = findTeam(getTeamName(teamKey));
        if (team != null) {
            team.removeMember(player.getUsername());
        }
    }

    /**
     * Replaces the members of the named scoreboard team with exactly the given usernames.
     *
     * @param teamName the scoreboard team to update
     * @param wanted   the usernames that should be on it afterwards
     */
    private void setMembers(String teamName, Set<String> wanted) {
        net.minestom.server.scoreboard.Team team = findTeam(teamName);
        if (team == null) return;

        Set<String> current = new HashSet<>(team.getMembers());
        Set<String> stale = new HashSet<>(current);
        stale.removeAll(wanted);
        if (!stale.isEmpty()) team.removeMembers(stale);

        Set<String> missing = new HashSet<>(wanted);
        missing.removeAll(current);
        if (!missing.isEmpty()) team.addMembers(missing);
    }

    /**
     * Reads the usernames on the game team behind the given key.
     *
     * @param teamService the service holding the game teams
     * @param teamKey     the key of the game team to read
     * @return the usernames, empty if the team does not exist
     */
    private Set<String> usernames(TeamService teamService, Key teamKey) {
        return teamService.getTeam(teamKey)
                .map(team -> {
                    Set<String> names = new HashSet<>();
                    team.getPlayers().forEach(player -> names.add(player.getUsername()));
                    return names;
                })
                .orElseGet(HashSet::new);
    }

    /**
     * Looks a scoreboard team up by name.
     *
     * @param teamName the name to look up
     * @return the team, or {@code null} if it was never created
     */
    private @Nullable net.minestom.server.scoreboard.Team findTeam(String teamName) {
        return MinecraftServer.getTeamManager().getTeam(teamName);
    }

    /**
     * Get the team name by the team key
     *
     * @param teamKey the team key
     * @return the team name
     */
    private String getTeamName(Key teamKey) {
        return GameConfig.SLENDER_KEY.equals(teamKey) ? GameConfig.SLENDER_TEAM_NAME : GameConfig.SURVIVOR_TEAM_NAME;
    }
}
