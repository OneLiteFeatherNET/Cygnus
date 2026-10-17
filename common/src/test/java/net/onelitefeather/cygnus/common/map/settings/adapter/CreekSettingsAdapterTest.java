package net.onelitefeather.cygnus.common.map.settings.adapter;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.onelitefeather.cygnus.common.map.settings.CreekSettings;
import net.onelitefeather.cygnus.common.util.GsonHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreekSettingsAdapterTest {

    private static CreekSettings read(String json) {
        return GsonHelper.GSON.fromJson(json, CreekSettings.class);
    }

    @Test
    @DisplayName("Keys in the block win over the defaults")
    void keysWinOverDefaults() {
        assertEquals(new CreekSettings(30, 20, 35, 30, 2.5D, 15, 5.0D),
                read("{ \"sightRange\": 30, \"routeLinkDistance\": 2.5 }"));
    }

    @Test
    @DisplayName("0 is a value, not a missing key")
    void zeroIsAValue() {
        assertEquals(0.0D, read("{ \"launchHeight\": 0 }").launchHeight());
    }

    @Test
    @DisplayName("An empty block gives the defaults")
    void emptyBlockGivesTheDefaults() {
        assertEquals(CreekSettings.DEFAULT, read("{}"));
    }

    @Test
    @DisplayName("A fraction in a whole-number key names the key instead of cutting it off")
    void fractionInAWholeNumberIsRejected() {
        JsonParseException exception = assertThrows(JsonParseException.class, () -> read("{ \"sightRange\": 30.5 }"));
        assertTrue(exception.getMessage().contains("sightRange"), exception.getMessage());
    }

    @Test
    @DisplayName("Text where a number belongs names the key")
    void textIsRejected() {
        JsonParseException exception = assertThrows(JsonParseException.class, () -> read("{ \"launchHeight\": \"high\" }"));
        assertTrue(exception.getMessage().contains("launchHeight"), exception.getMessage());
    }

    @Test
    @DisplayName("Values that contradict each other are rejected by the record")
    void contradictionIsRejected() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> read("{ \"personalSpace\": 25 }"));
        assertTrue(exception.getMessage().contains("personalSpace"), exception.getMessage());
    }

    @Test
    @DisplayName("Every value is written under its own key and comes back the same")
    void roundTrip() {
        CreekSettings settings = new CreekSettings(30, 18, 28, 20, 2.0D, 10, 0.0D);

        JsonObject written = GsonHelper.GSON.toJsonTree(settings).getAsJsonObject();

        assertEquals(Set.of("sightRange", "stalkMinDistance", "stalkMaxDistance", "respawnMinDistance",
                "routeLinkDistance", "personalSpace", "launchHeight"), written.keySet());
        assertEquals(settings, GsonHelper.GSON.fromJson(written, CreekSettings.class));
    }
}
