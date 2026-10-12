package net.onelitefeather.cygnus.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientApiConfigTest {

    private static ClientApiConfig read(String... pairs) {
        Properties properties = new Properties();
        for (int i = 0; i < pairs.length; i += 2) {
            properties.setProperty(pairs[i], pairs[i + 1]);
        }
        return ClientApiConfig.read(ConfigSection.root(properties).section("client-api."));
    }

    @Test
    @DisplayName("Without keys WorldDownloader is blocked and no LabyMod addon is disabled")
    void defaults() {
        assertEquals(ClientApiConfig.DEFAULT, read());
        assertTrue(read().blockWorldDownloader(), "downloading is blocked by default");
        assertTrue(read().labyModDisabledAddons().isEmpty(), "no addon is disabled by default");
    }

    @Test
    @DisplayName("The block-world-downloader key can switch the block off")
    void blockCanBeSwitchedOff() {
        assertFalse(read("client-api.block-world-downloader", "false").blockWorldDownloader());
    }

    @Test
    @DisplayName("The addon list is split on commas and trimmed")
    void addonListIsSplit() {
        ClientApiConfig config = read("client-api.labymod-disabled-addons", " voicechat , clearwater ");
        assertEquals(List.of("voicechat", "clearwater"), config.labyModDisabledAddons());
    }

    @Test
    @DisplayName("An invalid addon namespace is skipped and the valid ones are kept")
    void invalidNamespaceIsSkipped() {
        ClientApiConfig config = read("client-api.labymod-disabled-addons", "voicechat,Bad Name,,clearwater");
        assertEquals(List.of("voicechat", "clearwater"), config.labyModDisabledAddons());
    }

    @Test
    @DisplayName("A null addon list is rejected by the record")
    void nullAddonListIsRejected() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new ClientApiConfig(true, null));
        assertTrue(exception.getMessage().contains("must not be null"), exception.getMessage());
    }
}
