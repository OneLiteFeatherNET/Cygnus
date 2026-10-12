package net.onelitefeather.cygnus.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LunarConfigTest {

    private static ConfigSection section(String... pairs) {
        Properties properties = new Properties();
        for (int i = 0; i < pairs.length; i += 2) {
            properties.setProperty(pairs[i], pairs[i + 1]);
        }
        return ConfigSection.root(properties).section("lunar.");
    }

    @Test
    @DisplayName("Without any keys the request is on and switches off the default mods")
    void emptyGivesDefaults() {
        assertEquals(LunarConfig.DEFAULT, LunarConfig.read(section()));
    }

    @Test
    @DisplayName("The default request is on and names every known mod")
    void defaultsSwitchOffEveryKnownMod() {
        assertTrue(LunarConfig.DEFAULT.enabled());
        assertEquals(LunarConfig.KNOWN_MODS, LunarConfig.DEFAULT.disabledMods());
    }

    @Test
    @DisplayName("The disabled-mods list names exactly the mods to switch off")
    void readsTheListedMods() {
        assertEquals(List.of("minimap", "zoom"), LunarConfig.read(section("lunar.disabled-mods", "minimap,zoom")).disabledMods());
    }

    @Test
    @DisplayName("Mod names are trimmed and lower-cased before they are checked")
    void normalisesNames() {
        assertEquals(List.of("minimap", "zoom"), LunarConfig.read(section("lunar.disabled-mods", " Minimap , ZOOM ")).disabledMods());
    }

    @Test
    @DisplayName("An unknown mod name is skipped while the known names still apply")
    void unknownNameIsSkipped() {
        assertEquals(List.of("minimap"), LunarConfig.read(section("lunar.disabled-mods", "minimap,radar")).disabledMods());
    }

    @Test
    @DisplayName("A repeated mod name is switched off once")
    void repeatedNameIsKeptOnce() {
        assertEquals(List.of("minimap"), LunarConfig.read(section("lunar.disabled-mods", "minimap,minimap")).disabledMods());
    }

    @Test
    @DisplayName("The value none switches off no mod")
    void noneSwitchesOffNothing() {
        assertEquals(List.of(), LunarConfig.read(section("lunar.disabled-mods", "none")).disabledMods());
    }

    @Test
    @DisplayName("An empty disabled-mods value counts as absent and gives the default list")
    void emptyValueGivesDefaultList() {
        assertEquals(LunarConfig.KNOWN_MODS, LunarConfig.read(section("lunar.disabled-mods", "")).disabledMods());
    }

    @Test
    @DisplayName("enabled=false turns the request off and keeps the list")
    void disabledFlagTurnsRequestOff() {
        LunarConfig config = LunarConfig.read(section("lunar.enabled", "false"));
        assertFalse(config.enabled());
        assertEquals(LunarConfig.KNOWN_MODS, config.disabledMods());
    }

    @Test
    @DisplayName("An unreadable enabled flag falls back to on")
    void invalidFlagFallsBackToOn() {
        assertTrue(LunarConfig.read(section("lunar.enabled", "maybe")).enabled());
    }

    @Test
    @DisplayName("The record rejects a mod outside the known list and names it")
    void recordRejectsUnknownMod() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new LunarConfig(true, List.of("radar")));
        assertTrue(exception.getMessage().contains("radar"), exception.getMessage());
    }

    @Test
    @DisplayName("The record rejects a missing mod list")
    void recordRejectsNullList() {
        assertThrows(IllegalArgumentException.class, () -> new LunarConfig(true, null));
    }
}
