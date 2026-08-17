package net.onelitefeather.cygnus.stats.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class PlayerMapStatsIdTest {

    @Test
    void sameFields_areEqualAndHashConsistently() {
        UUID playerId = UUID.randomUUID();
        PlayerMapStatsId first = new PlayerMapStatsId(playerId, "Granskoga");
        PlayerMapStatsId second = new PlayerMapStatsId(playerId, "Granskoga");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void differentMapName_isNotEqual() {
        UUID playerId = UUID.randomUUID();
        PlayerMapStatsId granskoga = new PlayerMapStatsId(playerId, "Granskoga");
        PlayerMapStatsId lobby = new PlayerMapStatsId(playerId, "lobby");

        assertNotEquals(granskoga, lobby);
    }
}
