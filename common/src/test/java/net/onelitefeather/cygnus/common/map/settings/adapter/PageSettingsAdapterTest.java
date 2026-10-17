package net.onelitefeather.cygnus.common.map.settings.adapter;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.onelitefeather.cygnus.common.map.settings.PageSettings;
import net.onelitefeather.cygnus.common.util.GsonHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageSettingsAdapterTest {

    private static PageSettings read(String json) {
        return GsonHelper.GSON.fromJson(json, PageSettings.class);
    }

    @Test
    @DisplayName("A missing key takes its default")
    void missingKeyTakesTheDefault() {
        assertEquals(new PageSettings(12, 4, 60, 15), read("{ \"proximityRange\": 12 }"));
    }

    @Test
    @DisplayName("A value out of range is rejected by the record")
    void outOfRangeIsRejected() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> read("{ \"ttlSeconds\": 5 }"));
        assertTrue(exception.getMessage().contains("ttlSeconds"), exception.getMessage());
    }

    @Test
    @DisplayName("A fraction in a whole-number key names the key")
    void fractionIsRejected() {
        JsonParseException exception = assertThrows(JsonParseException.class, () -> read("{ \"minActivePages\": 4.5 }"));
        assertTrue(exception.getMessage().contains("minActivePages"), exception.getMessage());
    }

    @Test
    @DisplayName("Every value is written under its own key and comes back the same")
    void roundTrip() {
        PageSettings settings = new PageSettings(30, 6, 90, 20);

        JsonObject written = GsonHelper.GSON.toJsonTree(settings).getAsJsonObject();

        assertEquals(Set.of("proximityRange", "minActivePages", "ttlSeconds", "respawnDelaySeconds"), written.keySet());
        assertEquals(settings, GsonHelper.GSON.fromJson(written, PageSettings.class));
    }

    @Test
    @DisplayName("A value equal to its default is not written")
    void defaultIsLeftOut() {
        PageSettings settings = new PageSettings(30, 4, 60, 15);

        JsonObject written = GsonHelper.GSON.toJsonTree(settings).getAsJsonObject();

        assertEquals(Set.of("proximityRange"), written.keySet());
        assertEquals(settings, GsonHelper.GSON.fromJson(written, PageSettings.class));
    }

    @Test
    @DisplayName("The defaults are written as an empty block")
    void defaultsWriteNothing() {
        assertTrue(GsonHelper.GSON.toJsonTree(PageSettings.DEFAULT).getAsJsonObject().isEmpty());
    }
}
