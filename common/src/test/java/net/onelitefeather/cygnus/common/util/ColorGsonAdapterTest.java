package net.onelitefeather.cygnus.common.util;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import net.minestom.server.color.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ColorGsonAdapterTest {

    private static final Gson GSON = GsonHelper.GSON;
    private static final Color MOSS = new Color(15, 96, 52);

    @Test
    void writesColorsAsHex() {
        assertEquals("\"#0F6034\"", GSON.toJson(MOSS, Color.class));
    }

    @Test
    void readsHexBackIntoTheSameColor() {
        assertEquals(MOSS, GSON.fromJson("\"#0F6034\"", Color.class));
    }

    @Test
    void acceptsLowerCaseAndMissingHash() {
        assertEquals(MOSS, GSON.fromJson("\"0f6034\"", Color.class));
    }

    @Test
    void padsChannelsBelowSixteen() {
        assertEquals("\"#000000\"", GSON.toJson(new Color(0, 0, 0), Color.class));
        assertEquals("\"#01020F\"", GSON.toJson(new Color(1, 2, 15), Color.class));
    }

    @Test
    void rejectsMalformedHex() {
        assertThrows(JsonParseException.class, () -> GSON.fromJson("\"#xyzxyz\"", Color.class));
        assertThrows(JsonParseException.class, () -> GSON.fromJson("\"#0F60\"", Color.class));
    }
}
