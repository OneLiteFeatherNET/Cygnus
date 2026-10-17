package net.onelitefeather.cygnus.common.map;

import com.google.gson.JsonParser;
import net.onelitefeather.cygnus.common.map.settings.CreekSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MapSideFileTest {

    @Test
    @DisplayName("Missing keys are named in record order, keys that are there are not")
    void missingKeysInRecordOrder() {
        var block = JsonParser.parseString("{ \"sightRange\": 30, \"launchHeight\": 0 }").getAsJsonObject();

        assertEquals(List.of("stalkMinDistance", "stalkMaxDistance", "respawnMinDistance", "routeLinkDistance",
                "personalSpace"), MapSideFile.missingKeys(block, CreekSettings.class));
    }

    @Test
    @DisplayName("A key set to null counts as missing")
    void nullCountsAsMissing() {
        var block = JsonParser.parseString("{ \"sightRange\": null }").getAsJsonObject();

        assertEquals("sightRange", MapSideFile.missingKeys(block, CreekSettings.class).getFirst());
    }
}
