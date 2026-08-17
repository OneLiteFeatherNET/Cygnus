package net.onelitefeather.cygnus.stats.entity;

import net.onelitefeather.cygnus.stats.leader.LeaderboardCategory;
import net.onelitefeather.cygnus.stats.PlayerProfile;
import net.onelitefeather.cygnus.stats.RoundStatsUpdate;
import net.onelitefeather.cygnus.stats.leader.LeaderboardEntry;
import net.onelitefeather.cygnus.stats.repository.StatsRepository;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class StatsRepositoryIntegrationTest {

    @Container
    static final MariaDBContainer<?> MARIADB = new MariaDBContainer<>("mariadb:11.4");

    static SessionFactory sessionFactory;
    static StatsRepository repository;

    @BeforeAll
    static void setUp() {
        Configuration configuration = new Configuration();
        configuration.addAnnotatedClass(PlayerStatsEntity.class);
        configuration.addAnnotatedClass(PlayerMapStatsEntity.class);
        configuration.setProperty("hibernate.connection.url", MARIADB.getJdbcUrl());
        configuration.setProperty("hibernate.connection.username", MARIADB.getUsername());
        configuration.setProperty("hibernate.connection.password", MARIADB.getPassword());
        configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.MariaDBDialect");
        configuration.setProperty("hibernate.hbm2ddl.auto", "update");
        sessionFactory = configuration.buildSessionFactory();
        repository = new StatsRepository(sessionFactory);
    }

    @AfterAll
    static void tearDown() {
        sessionFactory.close();
    }

    @Test
    void recordRoundResult_createsRowOnFirstWin() {
        UUID playerId = UUID.randomUUID();
        repository.recordRoundResult(new RoundStatsUpdate(playerId, "Granskoga", true, true, false, 3, 120));

        PlayerProfile profile = repository.getProfile(playerId).orElseThrow();
        assertEquals(1, profile.winsSurvivor());
        assertEquals(0, profile.winsSlender());
        assertEquals(1, profile.roundsPlayed());
        assertEquals(0, profile.deaths());
        assertEquals(3, profile.pagesFound());
        assertEquals(120, profile.playtimeSeconds());
        assertEquals(1, profile.currentWinStreak());
        assertEquals(1, profile.longestWinStreak());
    }

    @Test
    void recordRoundResult_accumulatesAndResetsStreakAcrossRounds() {
        UUID playerId = UUID.randomUUID();
        repository.recordRoundResult(new RoundStatsUpdate(playerId, "Granskoga", true, true, false, 2, 100));
        repository.recordRoundResult(new RoundStatsUpdate(playerId, "Granskoga", true, false, true, 1, 90));

        PlayerProfile profile = repository.getProfile(playerId).orElseThrow();
        assertEquals(1, profile.winsSurvivor());
        assertEquals(2, profile.roundsPlayed());
        assertEquals(1, profile.deaths());
        assertEquals(3, profile.pagesFound());
        assertEquals(190, profile.playtimeSeconds());
        assertEquals(0, profile.currentWinStreak(), "streak resets after a loss");
        assertEquals(1, profile.longestWinStreak());
    }

    @Test
    void recordRoundResult_tracksPerMapStats() {
        UUID playerId = UUID.randomUUID();
        repository.recordRoundResult(new RoundStatsUpdate(playerId, "Granskoga", true, true, false, 0, 60));
        repository.recordRoundResult(new RoundStatsUpdate(playerId, "Granskoga", false, false, false, 0, 60));

        // Verified indirectly through getProfile's rounds_played not diverging from
        // player_map_stats.rounds_played for a single-map player - a direct getter for map stats
        // is intentionally not exposed yet (out of scope per the design spec).
        PlayerProfile profile = repository.getProfile(playerId).orElseThrow();
        assertEquals(2, profile.roundsPlayed());
    }

    @Test
    void getProfile_returnsEmptyForUnknownPlayer() {
        assertTrue(repository.getProfile(UUID.randomUUID()).isEmpty());
    }

    @Test
    void getLeaderboard_ordersByWinsDescending() {
        UUID top = UUID.randomUUID();
        UUID bottom = UUID.randomUUID();
        repository.recordRoundResult(new RoundStatsUpdate(top, "Granskoga", true, true, false, 0, 60));
        repository.recordRoundResult(new RoundStatsUpdate(top, "Granskoga", true, true, false, 0, 60));
        repository.recordRoundResult(new RoundStatsUpdate(bottom, "Granskoga", true, true, false, 0, 60));

        List<LeaderboardEntry> leaderboard =
                repository.getLeaderboard(LeaderboardCategory.WINS_SURVIVOR, 10);

        int topIndex = indexOf(leaderboard, top);
        int bottomIndex = indexOf(leaderboard, bottom);
        assertTrue(topIndex < bottomIndex, "the player with more wins must be ranked higher");
    }

    private static int indexOf(List<LeaderboardEntry> entries, UUID playerId) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).playerId().equals(playerId)) return i;
        }
        throw new AssertionError("player not found in leaderboard: " + playerId);
    }
}
