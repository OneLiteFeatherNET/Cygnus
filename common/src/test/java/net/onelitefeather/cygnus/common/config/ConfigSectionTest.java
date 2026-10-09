package net.onelitefeather.cygnus.common.config;

import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigSectionTest {

    private static ConfigSection root(String... pairs) {
        Properties properties = new Properties();
        for (int i = 0; i < pairs.length; i += 2) {
            properties.setProperty(pairs[i], pairs[i + 1]);
        }
        return ConfigSection.root(properties);
    }

    @Test
    @DisplayName("A section reads its keys under its prefix")
    void readsUnderThePrefix() {
        ConfigSection creek = root("creek.sightRange", "50", "sightRange", "7").section("creek.");

        assertEquals(50, creek.getInt("sightRange", 48));
        assertEquals("creek.sightRange", creek.key("sightRange"));
    }

    @Test
    @DisplayName("Nested sections append their prefixes")
    void nestedSectionsAppend() {
        ConfigSection nested = root("a.b.c", "3").section("a.").section("b.");

        assertEquals(3, nested.getInt("c", 0));
    }

    @Test
    @DisplayName("A missing key gives the fallback")
    void missingKeyFallsBack() {
        ConfigSection section = root();

        assertEquals(48, section.getInt("sightRange", 48));
        assertEquals(0.5F, section.getFloat("share", 0.5F));
        assertEquals(1.5D, section.getDouble("distance", 1.5D));
        assertTrue(section.getBoolean("enabled", true));
        assertEquals(Key.key("entity.player.hurt"), section.getSound("sound", Key.key("entity.player.hurt")));
        assertNull(section.getString("sentryDsn"));
    }

    @Test
    @DisplayName("An unreadable value gives the fallback instead of failing")
    void unreadableValueFallsBack() {
        ConfigSection section = root("int", "abc", "float", "x", "double", "y", "flag", "maybe", "sound", "Not A Key!");

        assertEquals(48, section.getInt("int", 48));
        assertEquals(0.5F, section.getFloat("float", 0.5F));
        assertEquals(1.5D, section.getDouble("double", 1.5D));
        assertTrue(section.getBoolean("flag", true));
        assertEquals(Key.key("entity.player.hurt"), section.getSound("sound", Key.key("entity.player.hurt")));
    }

    @Test
    @DisplayName("Values are trimmed, and a blank value counts as not set")
    void trimsAndTreatsBlankAsMissing() {
        ConfigSection section = root("int", " 12 ", "flag", " FALSE ", "name", "  value  ", "blank", "   ");

        assertEquals(12, section.getInt("int", 0));
        assertFalse(section.getBoolean("flag", true));
        assertEquals("value", section.getString("name"));
        assertNull(section.getString("blank"));
        assertEquals(0.25F, root("f", "0.25").getFloat("f", 0.0F));
    }

}
