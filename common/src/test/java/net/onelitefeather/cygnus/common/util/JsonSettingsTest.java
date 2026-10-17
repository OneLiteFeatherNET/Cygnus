package net.onelitefeather.cygnus.common.util;

import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonSettingsTest {

    private static final Path FILE = Path.of("maps", "forest", "creek.json");

    private static JsonSettings parse(String json) {
        return JsonSettings.of(JsonParser.parseString(json).getAsJsonObject(), FILE);
    }

    @Test
    @DisplayName("A key in the file wins over the default")
    void keyInTheFileWins() {
        JsonSettings settings = parse("{ \"settings\": { \"sightRange\": 30, \"launchHeight\": 2.5 } }");

        assertEquals(30, settings.getInt("sightRange", 48));
        assertEquals(2.5D, settings.getDouble("launchHeight", 5.0D));
        assertEquals(List.of(), settings.missing());
    }

    @Test
    @DisplayName("0 is a value, not a missing key")
    void zeroIsAValue() {
        JsonSettings settings = parse("{ \"settings\": { \"launchHeight\": 0 } }");

        assertEquals(0.0D, settings.getDouble("launchHeight", 5.0D));
        assertEquals(List.of(), settings.missing());
    }

    @Test
    @DisplayName("A missing key takes the default and is remembered")
    void missingKeyTakesTheDefault() {
        JsonSettings settings = parse("{ \"settings\": { \"sightRange\": 30 } }");

        assertEquals(15, settings.getInt("personalSpace", 15));
        assertEquals(3.0D, settings.getDouble("routeLinkDistance", 3.0D));
        assertEquals(List.of("personalSpace", "routeLinkDistance"), settings.missing());
        assertTrue(settings.hasSettings());
    }

    @Test
    @DisplayName("Without a settings block every value is a default, and nothing counts as missing")
    void noBlockMeansDefaults() {
        JsonSettings settings = parse("{ \"routes\": [] }");

        assertFalse(settings.hasSettings());
        assertEquals(48, settings.getInt("sightRange", 48));
        assertEquals(List.of(), settings.missing(), "a file without settings is logged once, not key by key");
        assertFalse(JsonSettings.none(FILE).hasSettings());
    }

    @Test
    @DisplayName("A fraction in a whole-number key names file and key instead of cutting it off")
    void fractionInAWholeNumberIsRejected() {
        JsonSettings settings = parse("{ \"settings\": { \"sightRange\": 30.5 } }");

        JsonSyntaxException exception = assertThrows(JsonSyntaxException.class, () -> settings.getInt("sightRange", 48));
        assertTrue(exception.getMessage().contains("creek.json"), exception.getMessage());
        assertTrue(exception.getMessage().contains("sightRange"), exception.getMessage());
    }

    @Test
    @DisplayName("Text where a number belongs names file and key")
    void textIsRejected() {
        JsonSettings settings = parse("{ \"settings\": { \"launchHeight\": \"high\" } }");

        JsonSyntaxException exception = assertThrows(JsonSyntaxException.class, () -> settings.getDouble("launchHeight", 5.0D));
        assertTrue(exception.getMessage().contains("launchHeight"), exception.getMessage());
    }

    @Test
    @DisplayName("A settings block that is no object names the file")
    void settingsMustBeAnObject() {
        JsonSyntaxException exception = assertThrows(JsonSyntaxException.class, () -> parse("{ \"settings\": [] }"));
        assertTrue(exception.getMessage().contains("creek.json"), exception.getMessage());
    }
}
