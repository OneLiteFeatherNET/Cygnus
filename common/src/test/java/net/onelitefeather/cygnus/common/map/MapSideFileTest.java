package net.onelitefeather.cygnus.common.map;

import com.google.gson.JsonParser;
import net.onelitefeather.cygnus.common.map.settings.CreekSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
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

    @Test
    @DisplayName("Settings out of range fall back to the defaults and name the reason")
    void outOfRangeFallsBackToTheDefaults() {
        var root = JsonParser.parseString("{ \"routes\": [], \"settings\": { \"sightRange\": 200 } }");
        List<String> reasons = new ArrayList<>();

        CreekSettings settings = MapSideFile.settingsOrDefaults(root, Path.of("creek.json"), CreekSettings.class,
                CreekSettings.DEFAULT, reasons::add);

        assertEquals(CreekSettings.DEFAULT, settings);
        assertEquals(1, reasons.size());
        assertTrue(reasons.getFirst().contains("sightRange"), reasons.getFirst());
    }

    @Test
    @DisplayName("A value that is no number falls back to the defaults and names the reason")
    void unreadableValueFallsBackToTheDefaults() {
        var root = JsonParser.parseString("{ \"routes\": [], \"settings\": { \"sightRange\": \"far\" } }");
        List<String> reasons = new ArrayList<>();

        CreekSettings settings = MapSideFile.settingsOrDefaults(root, Path.of("creek.json"), CreekSettings.class,
                CreekSettings.DEFAULT, reasons::add);

        assertEquals(CreekSettings.DEFAULT, settings);
        assertTrue(reasons.getFirst().contains("sightRange"), reasons.getFirst());
    }

    @Test
    @DisplayName("Valid settings are read as they are and report nothing")
    void validSettingsReportNothing() {
        var root = JsonParser.parseString("{ \"routes\": [], \"settings\": { \"sightRange\": 30 } }");
        List<String> reasons = new ArrayList<>();

        CreekSettings settings = MapSideFile.settingsOrDefaults(root, Path.of("creek.json"), CreekSettings.class,
                CreekSettings.DEFAULT, reasons::add);

        assertEquals(30, settings.sightRange());
        assertTrue(reasons.isEmpty());
    }
}
