package net.onelitefeather.cygnus.common.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameConfigTest {

    @Test
    void testGameConfig() {
        GameConfig gameConfig = new GameConfig(1, 5);
        assertEquals(1, gameConfig.slenderTeamSize());
        assertEquals(5, gameConfig.survivorTeamSize());
    }
}
