package net.onelitefeather.cygnus.team;

import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.common.config.GameConfigReader;
import net.theevilreaper.xerus.api.team.TeamService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Paths;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link TeamHelper#survivorsOf(TeamService)} and {@link TeamHelper#slenderOf(TeamService)},
 * the roster lookups the full-screen effects read once per tick.
 * <p>
 * The empty cases carry the weight here: the effects are ticked from a scheduler that runs before a
 * round has set up its teams and after it has torn them down, so both lookups have to answer for a
 * game that is not running rather than throw into a scheduler task.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
@ExtendWith(MicrotusExtension.class)
class TeamRosterTest {

    private static GameConfig gameConfig;

    @BeforeAll
    static void init() {
        gameConfig = new GameConfigReader(Paths.get("")).getConfig();
    }

    /**
     * @return a service holding the round's two teams, both still empty
     */
    private static TeamService teamsWithoutPlayers() {
        TeamService teamService = TeamService.of();
        TeamCreator teamCreator = new TeamCreator() {
        };
        teamCreator.createTeams(gameConfig, teamService);
        return teamService;
    }

    @Test
    @DisplayName("Before the teams exist the survivors are an empty set")
    void survivorsWithoutTeams() {
        assertTrue(TeamHelper.survivorsOf(TeamService.of()).isEmpty());
    }

    @Test
    @DisplayName("Before the teams exist there is no slender")
    void slenderWithoutTeams() {
        assertNull(TeamHelper.slenderOf(TeamService.of()));
    }

    @Test
    @DisplayName("An empty survivor team yields an empty set rather than throwing")
    void emptySurvivorTeam() {
        assertTrue(TeamHelper.survivorsOf(teamsWithoutPlayers()).isEmpty());
    }

    @Test
    @DisplayName("An unassigned slender role reads as null")
    void emptySlenderTeam() {
        assertNull(TeamHelper.slenderOf(teamsWithoutPlayers()));
    }

    @Test
    @DisplayName("The players on the survivor team are the ones reported")
    void survivorsAreReported(Env env) {
        Instance instance = env.createFlatInstance();
        Player first = env.createPlayer(instance);
        Player second = env.createPlayer(instance);

        TeamService teamService = teamsWithoutPlayers();
        teamService.getTeam(GameConfig.SURVIVOR_KEY).orElseThrow().addPlayer(first);
        teamService.getTeam(GameConfig.SURVIVOR_KEY).orElseThrow().addPlayer(second);

        assertEquals(Set.of(first, second), TeamHelper.survivorsOf(teamService));
    }

    @Test
    @DisplayName("The player on the slender team is the slender")
    void slenderIsReported(Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = env.createPlayer(instance);

        TeamService teamService = teamsWithoutPlayers();
        teamService.getTeam(GameConfig.SLENDER_KEY).orElseThrow().addPlayer(slender);

        assertSame(slender, TeamHelper.slenderOf(teamService));
    }

    @Test
    @DisplayName("The reported survivors are a snapshot, not the team's own collection")
    void survivorsAreASnapshot(Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createPlayer(instance);

        TeamService teamService = teamsWithoutPlayers();
        teamService.getTeam(GameConfig.SURVIVOR_KEY).orElseThrow().addPlayer(survivor);

        Set<Player> survivors = TeamHelper.survivorsOf(teamService);

        assertThrows(UnsupportedOperationException.class, () -> survivors.add(survivor),
                "an effect iterating the roster mid-tick must not be able to change the team through it");
    }
}
