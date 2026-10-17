package net.onelitefeather.cygnus.common.map;

import com.google.gson.JsonParser;
import net.onelitefeather.cygnus.common.map.settings.CreekSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapSideFileTest {

    @Test
    @DisplayName("Unknown keys are named in block order, known keys are not")
    void unknownKeysInBlockOrder() {
        var block = JsonParser.parseString("{ \"sightRang\": 30, \"launchHeight\": 0, \"speed\": 2 }").getAsJsonObject();

        assertEquals(List.of("sightRang", "speed"), MapSideFile.unknownKeys(block, CreekSettings.class));
    }

    @Test
    @DisplayName("A block that leaves keys out has no unknown keys")
    void leftOutKeysAreNotUnknown() {
        var block = JsonParser.parseString("{ \"sightRange\": 30 }").getAsJsonObject();

        assertTrue(MapSideFile.unknownKeys(block, CreekSettings.class).isEmpty());
    }
}
